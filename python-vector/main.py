# -*- coding: utf-8 -*-
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8")

from fastapi import FastAPI, Query
from pydantic import BaseModel
import faiss
import numpy as np
import json
import os
import logging
import threading
import tempfile
import subprocess

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def _disable_broken_proxy_env():
    for key in ("HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY", "http_proxy", "https_proxy", "all_proxy"):
        value = os.environ.get(key, "")
        if "127.0.0.1:9" in value:
            os.environ.pop(key, None)
            logger.info(f"[Network] 무효 프록시 환경 제거: {key}")


_disable_broken_proxy_env()
try:
    import httpx
    import librosa
    import soundfile
    import audioread
    import imageio_ffmpeg
    AUDIO_AVAILABLE = True
except ImportError as e:
    AUDIO_AVAILABLE = False
    logger.warning(f"[Audio] 오디오 라이브러리 미설치 - 오디오 특징 추출 비활성화: {e}")

app = FastAPI()
DATA_DIR = os.environ.get("VECTOR_DATA_DIR", ".")
os.makedirs(DATA_DIR, exist_ok=True)

# ── 텍스트 FAISS 설정 ────────────────────────────────────
DIMENSION = 1536          # text-embedding-3-small 차원
INDEX_PATH = os.path.join(DATA_DIR, "songs.index")
META_PATH  = os.path.join(DATA_DIR, "songs_meta.json")

if os.path.exists(INDEX_PATH) and os.path.exists(META_PATH):
    faiss_index = faiss.read_index(INDEX_PATH)
    with open(META_PATH, encoding="utf-8") as f:
        song_meta: list[dict] = json.load(f)
    logger.info(f"[FAISS] 텍스트 인덱스 로드 완료 - {faiss_index.ntotal}개 벡터")
else:
    faiss_index = faiss.IndexFlatIP(DIMENSION)
    song_meta: list[dict] = []
    logger.info("[FAISS] 새 텍스트 인덱스 생성")

# ── 오디오 FAISS 설정 ────────────────────────────────────
# MFCC(13) + 크로마(12) + 템포·RMS·스펙트럴 센트로이드·ZCR·롤오프(5) = 30차원
AUDIO_DIM = 30
AUDIO_INDEX_PATH = os.path.join(DATA_DIR, "songs_audio.index")
AUDIO_META_PATH  = os.path.join(DATA_DIR, "songs_audio_meta.json")

if os.path.exists(AUDIO_INDEX_PATH) and os.path.exists(AUDIO_META_PATH):
    audio_index = faiss.read_index(AUDIO_INDEX_PATH)
    with open(AUDIO_META_PATH, encoding="utf-8") as f:
        audio_meta: list[dict] = json.load(f)
    logger.info(f"[FAISS] 오디오 인덱스 로드 완료 - {audio_index.ntotal}개 벡터")
else:
    audio_index = faiss.IndexFlatIP(AUDIO_DIM)
    audio_meta: list[dict] = []
    logger.info("[FAISS] 새 오디오 인덱스 생성")

faiss_lock = threading.Lock()  # 멀티스레드 안전

def _is_active(meta: dict) -> bool:
    return bool(meta.get("active", True))


# ── 오디오 특징 추출 ─────────────────────────────────────

def _safe_float(value: float) -> float:
    if value is None:
        return 0.0
    if isinstance(value, np.ndarray):
        if value.size == 0:
            return 0.0
        return float(value.reshape(-1)[0])
    if isinstance(value, (list, tuple)):
        if not value:
            return 0.0
        return float(value[0])
    return float(value)


def _cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    a_norm = np.linalg.norm(a)
    b_norm = np.linalg.norm(b)
    if a_norm == 0 or b_norm == 0:
        return 0.0
    score = float(np.dot(a, b) / (a_norm * b_norm))
    return max(0.0, min(1.0, (score + 1.0) / 2.0))


def _closeness(a: float, b: float, scale: float) -> float:
    if scale <= 0:
        return 0.0
    return max(0.0, 1.0 - min(abs(a - b) / scale, 1.0))


def _audio_profile(features: np.ndarray) -> dict:
    return {
        "tempo": _safe_float(features[25]),
        "energy": _safe_float(features[26]),
        "brightness": _safe_float(features[27]),
        "rhythm": _safe_float(features[28]),
        "openness": _safe_float(features[29]),
    }


def _audio_detail(query_features: np.ndarray, candidate_features: np.ndarray) -> dict:
    q_profile = _audio_profile(query_features)
    c_profile = _audio_profile(candidate_features)

    timbre = _cosine_similarity(query_features[:13], candidate_features[:13])
    harmony = _cosine_similarity(query_features[13:25], candidate_features[13:25])
    tempo = _closeness(q_profile["tempo"], c_profile["tempo"], max(q_profile["tempo"], c_profile["tempo"], 40.0))
    energy = _closeness(q_profile["energy"], c_profile["energy"], max(q_profile["energy"], c_profile["energy"], 0.05))
    brightness = (
        _closeness(q_profile["brightness"], c_profile["brightness"], max(q_profile["brightness"], c_profile["brightness"], 500.0))
        + _closeness(q_profile["openness"], c_profile["openness"], max(q_profile["openness"], c_profile["openness"], 1000.0))
    ) / 2.0
    rhythm = _closeness(q_profile["rhythm"], c_profile["rhythm"], max(q_profile["rhythm"], c_profile["rhythm"], 0.05))

    return {
        "tempo_similarity": round(tempo, 4),
        "energy_similarity": round(energy, 4),
        "brightness_similarity": round(brightness, 4),
        "rhythm_similarity": round(rhythm, 4),
        "timbre_similarity": round(timbre, 4),
        "harmony_similarity": round(harmony, 4),
    }

def extract_audio_features(preview_url: str) -> np.ndarray | None:
    """iTunes 미리듣기 URL에서 오디오 특징 30차원 벡터 추출."""
    if not AUDIO_AVAILABLE:
        return None
    temp_path = None
    wav_temp_path = None
    try:
        resp = httpx.get(preview_url, timeout=10, follow_redirects=True)
        resp.raise_for_status()
        content_type = (resp.headers.get("content-type") or "").lower()
        content = resp.content
        if not content:
            logger.warning(f"[Audio] 빈 응답으로 특징 추출 실패: {preview_url[:60]}...")
            return None

        suffix = ".m4a"
        if "mpeg" in content_type or preview_url.lower().endswith(".mp3"):
            suffix = ".mp3"
        elif "wav" in content_type or preview_url.lower().endswith(".wav"):
            suffix = ".wav"
        elif "mp4" in content_type or "aac" in content_type:
            suffix = ".m4a"

        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
            tmp.write(content)
            temp_path = tmp.name

        ffmpeg_path = _find_ffmpeg()
        if not ffmpeg_path:
            raise RuntimeError("ffmpeg_not_found")

        with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as wav_tmp:
            wav_temp_path = wav_tmp.name

        command = [
            ffmpeg_path,
            "-y",
            "-i", temp_path,
            "-ac", "1",
            "-ar", "22050",
            wav_temp_path,
        ]
        subprocess.run(command, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        y, sr = librosa.load(wav_temp_path, sr=22050, duration=30, mono=True)
        logger.info("[Audio] ffmpeg WAV 변환 후 로드 성공")

        mfcc = np.mean(librosa.feature.mfcc(y=y, sr=sr, n_mfcc=13), axis=1)          # 13
        chroma = np.mean(librosa.feature.chroma_stft(y=y, sr=sr), axis=1)             # 12
        tempo, _ = librosa.beat.beat_track(y=y, sr=sr)                                # 1
        tempo = _safe_float(tempo)
        rms = _safe_float(np.mean(librosa.feature.rms(y=y)))                          # 1
        centroid = _safe_float(np.mean(librosa.feature.spectral_centroid(y=y, sr=sr)))# 1
        zcr = _safe_float(np.mean(librosa.feature.zero_crossing_rate(y)))             # 1
        rolloff = _safe_float(np.mean(librosa.feature.spectral_rolloff(y=y, sr=sr)))  # 1

        features = np.concatenate([
            mfcc, chroma,
            [tempo, rms, centroid, zcr, rolloff]
        ]).astype(np.float32)                                                          # 총 30차원
        return features
    except Exception as e:
        logger.warning(
            f"[Audio] 특징 추출 실패 ({preview_url[:60]}...): {e}"
        )
        return None
    finally:
        if temp_path and os.path.exists(temp_path):
            try:
                os.remove(temp_path)
            except OSError:
                logger.debug(f"[Audio] 임시 파일 삭제 실패: {temp_path}")
        if wav_temp_path and os.path.exists(wav_temp_path):
            try:
                os.remove(wav_temp_path)
            except OSError:
                logger.debug(f"[Audio] WAV 임시 파일 삭제 실패: {wav_temp_path}")


def _find_ffmpeg() -> str | None:
    try:
        ffmpeg_exe = imageio_ffmpeg.get_ffmpeg_exe()
        if ffmpeg_exe and os.path.exists(ffmpeg_exe):
            return ffmpeg_exe
    except Exception:
        pass

    for cmd in ("ffmpeg", "ffmpeg.exe"):
        try:
            subprocess.run([cmd, "-version"], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            return cmd
        except Exception:
            continue
    return None

def _save_faiss():
    """텍스트 + 오디오 인덱스와 메타데이터를 파일로 저장"""
    faiss.write_index(faiss_index, INDEX_PATH)
    with open(META_PATH, "w", encoding="utf-8") as f:
        json.dump(song_meta, f, ensure_ascii=False, indent=2)
    faiss.write_index(audio_index, AUDIO_INDEX_PATH)
    with open(AUDIO_META_PATH, "w", encoding="utf-8") as f:
        json.dump(audio_meta, f, ensure_ascii=False, indent=2)


# ── Pydantic 모델 ────────────────────────────────────────
class IndexRequest(BaseModel):
    id: int
    trackName: str
    artistName: str
    genreName: str = ""
    vector: list[float]
    previewUrl: str = ""   # 오디오 특징 추출용 (선택)

class SearchRequest(BaseModel):
    vector: list[float]
    limit: int = 10
    songId: int | None = None  # 인덱싱된 기준곡 ID(있으면 오디오 벡터 재사용)
    previewUrl: str = ""   # 쿼리 곡 오디오 특징 추출용 (선택)


# ── 엔드포인트 ───────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/status")
def status():
    return {
        "status": "ok",
        "faiss_text_vectors": faiss_index.ntotal,
        "faiss_audio_vectors": audio_index.ntotal,
    }


# ── FAISS 벡터 인덱싱 ────────────────────────────────────

@app.post("/vector/index")
def index_song(req: IndexRequest):
    """곡 텍스트 벡터 + 오디오 특징을 FAISS에 저장 (upsert)"""
    # 오디오 특징 추출 (lock 밖에서 실행해 대기 시간 최소화)
    audio_vec = None
    if req.previewUrl:
        audio_vec = extract_audio_features(req.previewUrl)

    with faiss_lock:
        # ── 텍스트 FAISS ──
        vec = np.array([req.vector], dtype=np.float32)
        faiss.normalize_L2(vec)
        existing = next(
            (i for i in range(len(song_meta) - 1, -1, -1)
             if song_meta[i].get("id") == req.id and _is_active(song_meta[i])),
            None,
        )
        if existing is not None:
            song_meta[existing]["active"] = False

        faiss_idx = faiss_index.ntotal
        faiss_index.add(vec)
        song_meta.append({
            "id": req.id,
            "trackName": req.trackName,
            "artistName": req.artistName,
            "genreName": req.genreName,
            "faiss_idx": faiss_idx,
            "active": True,
        })

        # ── 오디오 FAISS ──
        if audio_vec is not None:
            av = audio_vec.reshape(1, -1)
            faiss.normalize_L2(av)
            a_existing = next(
                (i for i in range(len(audio_meta) - 1, -1, -1)
                 if audio_meta[i].get("id") == req.id and _is_active(audio_meta[i])),
                None,
            )
            if a_existing is not None:
                audio_meta[a_existing]["active"] = False

            a_idx = audio_index.ntotal
            audio_index.add(av)
            audio_meta.append({
                "id": req.id,
                "audio_idx": a_idx,
                "raw_vector": audio_vec.astype(float).tolist(),
                "profile": _audio_profile(audio_vec),
                "active": True,
            })

        _save_faiss()

    has_audio = audio_vec is not None
    logger.info(f"[FAISS] 인덱싱 완료: {req.trackName} - {req.artistName} "
                f"(텍스트 총 {faiss_index.ntotal}개, 오디오: {'O' if has_audio else 'X'})")
    return {"status": "ok", "total": faiss_index.ntotal, "audio": has_audio}


@app.post("/vector/search")
def search_songs(req: SearchRequest):
    """오디오 유사도 주도 검색.
    오디오 있을 때: 최종점수 = 0.75 × 오디오유사도 + 0.25 × 텍스트유사도
    오디오 없을 때: 최종점수 = 텍스트유사도만 사용
    후보 탐색: 오디오 인덱스 우선, 텍스트 인덱스로 보충
    """
    # 쿼리 오디오 특징 추출 (lock 밖에서 실행)
    query_audio_vec = None
    if req.songId is not None:
        cached = next(
            (m for m in reversed(audio_meta) if m.get("id") == int(req.songId) and _is_active(m)),
            None,
        )
        raw_vector = cached.get("raw_vector") if cached else None
        if raw_vector and len(raw_vector) == AUDIO_DIM:
            query_audio_vec = np.array(raw_vector, dtype=np.float32)
            logger.info(f"[VectorSearch] query audio cache hit: songId={req.songId}")
    if query_audio_vec is None and req.previewUrl:
        query_audio_vec = extract_audio_features(req.previewUrl)
        if query_audio_vec is not None:
            logger.info("[VectorSearch] query audio extracted from previewUrl")

    with faiss_lock:
        if faiss_index.ntotal == 0:
            return {"results": []}

        k = min(req.limit + 20, faiss_index.ntotal)

        # ── 텍스트 인덱스 검색 ──
        vec = np.array([req.vector], dtype=np.float32)
        faiss.normalize_L2(vec)
        text_scores, text_indices = faiss_index.search(vec, k)
        text_lookup = {
            int(m.get("faiss_idx")): m
            for m in song_meta
            if _is_active(m) and isinstance(m.get("faiss_idx"), int)
        }
        text_score_map: dict[int, float] = {}
        for score, idx in zip(text_scores[0], text_indices[0]):
            if idx < 0 or idx >= len(song_meta):
                continue
            meta = text_lookup.get(int(idx))
            if meta:
                text_score_map[meta["id"]] = float(score)

        candidates: dict[int, dict] = {}

        # ── 오디오 인덱스 우선 검색 ──
        if query_audio_vec is not None and audio_index.ntotal > 0:
            av = query_audio_vec.reshape(1, -1)
            faiss.normalize_L2(av)
            ak = min(req.limit + 20, audio_index.ntotal)
            a_scores, a_indices = audio_index.search(av, ak)
            audio_lookup = {
                int(m.get("audio_idx")): m
                for m in audio_meta
                if _is_active(m) and isinstance(m.get("audio_idx"), int)
            }
            for a_score, a_idx in zip(a_scores[0], a_indices[0]):
                if a_idx < 0 or a_idx >= len(audio_meta):
                    continue
                ameta = audio_lookup.get(int(a_idx))
                if not ameta:
                    continue
                song_id = ameta["id"]
                raw_vector = ameta.get("raw_vector")
                detail = None
                if raw_vector and len(raw_vector) == AUDIO_DIM:
                    detail = _audio_detail(query_audio_vec, np.array(raw_vector, dtype=np.float32))
                t_score = text_score_map.get(song_id, 0.0)
                final_score = round(0.75 * float(a_score) + 0.25 * t_score, 4)

                # 텍스트 메타 보충 (trackName, artistName, genreName)
                tmeta = next(
                    (m for m in reversed(song_meta) if m.get("id") == song_id and _is_active(m)),
                    None,
                )
                if tmeta is None:
                    continue
                candidates[song_id] = {
                    "id": song_id,
                    "trackName": tmeta["trackName"],
                    "artistName": tmeta["artistName"],
                    "genreName": tmeta.get("genreName", ""),
                    "text_score": t_score,
                    "audio_score": float(a_score),
                    "score": final_score,
                }
                if detail:
                    candidates[song_id]["audio_detail"] = detail

            # 오디오 인덱스에 없는 곡은 텍스트로 보충 (텍스트 전용 점수)
            for song_id, t_score in text_score_map.items():
                if song_id not in candidates:
                    tmeta = next(
                        (m for m in reversed(song_meta) if m.get("id") == song_id and _is_active(m)),
                        None,
                    )
                    if tmeta is None:
                        continue
                    candidates[song_id] = {
                        "id": song_id,
                        "trackName": tmeta["trackName"],
                        "artistName": tmeta["artistName"],
                        "genreName": tmeta.get("genreName", ""),
                        "text_score": t_score,
                        "score": t_score,
                    }
        else:
            # 오디오 없으면 텍스트만 사용
            for song_id, t_score in text_score_map.items():
                tmeta = next(
                    (m for m in reversed(song_meta) if m.get("id") == song_id and _is_active(m)),
                    None,
                )
                if tmeta is None:
                    continue
                candidates[song_id] = {
                    "id": song_id,
                    "trackName": tmeta["trackName"],
                    "artistName": tmeta["artistName"],
                    "genreName": tmeta.get("genreName", ""),
                    "text_score": t_score,
                    "score": t_score,
                }

    results = sorted(candidates.values(), key=lambda x: x["score"], reverse=True)[:req.limit]
    logger.info(f"[VectorSearch] 결과 {len(results)}곡 (오디오{'O' if query_audio_vec is not None else 'X'})")
    return {"results": results}


# ── FAISS 데이터 확인용 엔드포인트 ───────────────────────

@app.get("/vector/stats")
def vector_stats():
    """FAISS 인덱스 현황"""
    with faiss_lock:
        return {
            "text_vectors":  faiss_index.ntotal,
            "audio_vectors": audio_index.ntotal,
            "text_dimension":  DIMENSION,
            "audio_dimension": AUDIO_DIM,
            "index_type": type(faiss_index).__name__,
        }


@app.get("/vector/list")
def vector_list(
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    q: str = Query("", description="곡명 또는 아티스트 검색"),
):
    """저장된 곡 목록 조회 (페이지네이션 + 검색)"""
    with faiss_lock:
        items = [m for m in song_meta if _is_active(m)]
        if q:
            q_lower = q.lower()
            items = [m for m in items
                     if q_lower in m["trackName"].lower() or q_lower in m["artistName"].lower()]
        total = len(items)
        start = (page - 1) * size
        paged = items[start: start + size]
    return {"total": total, "page": page, "size": size, "items": paged}


@app.delete("/vector/clear")
def vector_clear():
    """FAISS 인덱스 초기화 (전체 삭제)"""
    global faiss_index, song_meta
    with faiss_lock:
        faiss_index = faiss.IndexFlatIP(DIMENSION)
        song_meta = []
        _save_faiss()
    logger.warning("[FAISS] 인덱스 전체 초기화 완료")
    return {"status": "cleared"}


@app.get("/vector/indexed-ids")
def get_indexed_ids():
    """텍스트 및 오디오 인덱싱된 곡 ID 목록 반환 (배치 증분 인덱싱용)"""
    with faiss_lock:
        text_ids  = [m["id"] for m in song_meta if _is_active(m)]
        audio_ids = [m["id"] for m in audio_meta if _is_active(m)]
    return {"text_ids": text_ids, "audio_ids": audio_ids}


# Vector-only service.
