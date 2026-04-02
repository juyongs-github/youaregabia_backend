# -*- coding: utf-8 -*-
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8")

from fastapi import FastAPI, Query
from pydantic import BaseModel
import yt_dlp
import faiss
import numpy as np
import json
import os
import logging
import re
import threading
import io

# ytmusicapi 인증 초기화 (browser.json이 있을 때만 활성화)
try:
    from ytmusicapi import YTMusic
    _YTMUSIC_AUTH_FILE = os.path.join(os.path.dirname(__file__), "browser.json")
    if os.path.exists(_YTMUSIC_AUTH_FILE):
        ytmusic_client = YTMusic(_YTMUSIC_AUTH_FILE)
        YTMUSIC_AUTH_AVAILABLE = True
        logging.getLogger(__name__).info("[YTMusic] 계정 인증 완료 — 개인화 추천 활성화")
    else:
        ytmusic_client = YTMusic()   # 비인증 (공개 요청)
        YTMUSIC_AUTH_AVAILABLE = False
        logging.getLogger(__name__).info("[YTMusic] browser.json 없음 — 비인증 모드")
except Exception as _e:
    ytmusic_client = None
    YTMUSIC_AUTH_AVAILABLE = False
    logging.getLogger(__name__).warning(f"[YTMusic] ytmusicapi 초기화 실패: {_e}")
try:
    import httpx
    import librosa
    import soundfile
    AUDIO_AVAILABLE = True
except ImportError as e:
    AUDIO_AVAILABLE = False
    logger.warning(f"[Audio] 오디오 라이브러리 미설치 - 오디오 특징 추출 비활성화: {e}")

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()

# ── 텍스트 FAISS 설정 ────────────────────────────────────
DIMENSION = 1536          # text-embedding-3-small 차원
INDEX_PATH = "songs.index"
META_PATH  = "songs_meta.json"

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
AUDIO_INDEX_PATH = "songs_audio.index"
AUDIO_META_PATH  = "songs_audio_meta.json"

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


# ── 오디오 특징 추출 ─────────────────────────────────────

def extract_audio_features(preview_url: str) -> np.ndarray | None:
    """iTunes 미리듣기 URL에서 오디오 특징 30차원 벡터 추출."""
    if not AUDIO_AVAILABLE:
        return None
    try:
        resp = httpx.get(preview_url, timeout=10, follow_redirects=True)
        resp.raise_for_status()
        audio_bytes = io.BytesIO(resp.content)
        y, sr = librosa.load(audio_bytes, sr=22050, duration=30, mono=True)

        mfcc = np.mean(librosa.feature.mfcc(y=y, sr=sr, n_mfcc=13), axis=1)          # 13
        chroma = np.mean(librosa.feature.chroma_stft(y=y, sr=sr), axis=1)             # 12
        tempo, _ = librosa.beat.beat_track(y=y, sr=sr)                                # 1
        rms = float(np.mean(librosa.feature.rms(y=y)))                                # 1
        centroid = float(np.mean(librosa.feature.spectral_centroid(y=y, sr=sr)))      # 1
        zcr = float(np.mean(librosa.feature.zero_crossing_rate(y)))                   # 1
        rolloff = float(np.mean(librosa.feature.spectral_rolloff(y=y, sr=sr)))        # 1

        features = np.concatenate([
            mfcc, chroma,
            [float(tempo), rms, centroid, zcr, rolloff]
        ]).astype(np.float32)                                                          # 총 30차원
        return features
    except Exception as e:
        logger.warning(f"[Audio] 특징 추출 실패 ({preview_url[:60]}...): {e}")
        return None

def _save_faiss():
    """텍스트 + 오디오 인덱스와 메타데이터를 파일로 저장"""
    faiss.write_index(faiss_index, INDEX_PATH)
    with open(META_PATH, "w", encoding="utf-8") as f:
        json.dump(song_meta, f, ensure_ascii=False, indent=2)
    faiss.write_index(audio_index, AUDIO_INDEX_PATH)
    with open(AUDIO_META_PATH, "w", encoding="utf-8") as f:
        json.dump(audio_meta, f, ensure_ascii=False, indent=2)


# ── yt-dlp 유틸 ─────────────────────────────────────────
YDL_QUIET_OPTS = {"quiet": True, "no_warnings": True, "extract_flat": True}


def _extract_song_info(title: str, uploader: str) -> tuple[str, str]:
    """YouTube 영상 제목에서 곡명·아티스트 추출.
    예) "BTS (방탄소년단) 'Butter' Official MV" → ("Butter", "BTS")
    """
    match = re.search(r"'([^']+)'", title)
    if match:
        song = re.sub(r"\s*\(feat\..*?\)", "", match.group(1), flags=re.IGNORECASE).strip()
        artist_part = title[: title.index("'")].strip()
        artist = re.sub(r"\s*\([^)]*[\uAC00-\uD7A3][^)]*\)", "", artist_part).strip() or uploader
        return song, artist

    dash = re.match(r"^(.+?)\s+[-–]\s+(.+)$", title)
    if dash:
        song = re.sub(r"\s*(Official\s*(MV|Video|Audio|Lyric)|Lyric\s*Video|M/V)\s*$",
                      "", dash.group(2), flags=re.IGNORECASE).strip()
        return song, dash.group(1).strip()

    clean = re.sub(r"\s*(Official\s*(MV|Video|Audio|Lyric)|Lyric\s*Video|M/V)\s*$",
                   "", title, flags=re.IGNORECASE).strip()
    return clean, uploader


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
    previewUrl: str = ""   # 쿼리 곡 오디오 특징 추출용 (선택)


# ── 엔드포인트 ───────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/status")
def status():
    return {
        "status": "ok",
        "ytmusic_auth": YTMUSIC_AUTH_AVAILABLE,
        "ytmusic_available": ytmusic_client is not None,
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

        existing = next((i for i, m in enumerate(song_meta) if m["id"] == req.id), None)
        if existing is not None:
            song_meta[existing] = {
                "id": req.id, "trackName": req.trackName,
                "artistName": req.artistName, "genreName": req.genreName,
                "faiss_idx": song_meta[existing]["faiss_idx"],
            }
        else:
            faiss_idx = faiss_index.ntotal
            faiss_index.add(vec)
            song_meta.append({
                "id": req.id, "trackName": req.trackName,
                "artistName": req.artistName, "genreName": req.genreName,
                "faiss_idx": faiss_idx,
            })

        # ── 오디오 FAISS ──
        if audio_vec is not None:
            av = audio_vec.reshape(1, -1)
            faiss.normalize_L2(av)
            a_existing = next((i for i, m in enumerate(audio_meta) if m["id"] == req.id), None)
            if a_existing is None:
                a_idx = audio_index.ntotal
                audio_index.add(av)
                audio_meta.append({"id": req.id, "audio_idx": a_idx})

        _save_faiss()

    has_audio = audio_vec is not None
    logger.info(f"[FAISS] 인덱싱 완료: {req.trackName} - {req.artistName} "
                f"(텍스트 총 {faiss_index.ntotal}개, 오디오: {'O' if has_audio else 'X'})")
    return {"status": "ok", "total": faiss_index.ntotal, "audio": has_audio}


@app.post("/vector/search")
def search_songs(req: SearchRequest):
    """텍스트 유사도 + 오디오 유사도 결합 기반 유사곡 검색.
    최종점수 = 0.6 × 텍스트유사도 + 0.4 × 오디오유사도 (오디오 없으면 텍스트만)
    """
    # 쿼리 오디오 특징 추출 (lock 밖에서 실행)
    query_audio_vec = None
    if req.previewUrl:
        query_audio_vec = extract_audio_features(req.previewUrl)

    with faiss_lock:
        if faiss_index.ntotal == 0:
            return {"results": []}

        # ── 텍스트 검색 ──
        vec = np.array([req.vector], dtype=np.float32)
        faiss.normalize_L2(vec)
        k = min(req.limit + 10, faiss_index.ntotal)
        text_scores, text_indices = faiss_index.search(vec, k)

        # 텍스트 후보 → {song_id: text_score}
        candidates: dict[int, dict] = {}
        for score, idx in zip(text_scores[0], text_indices[0]):
            if idx < 0 or idx >= len(song_meta):
                continue
            meta = next((m for m in song_meta if m.get("faiss_idx") == int(idx)), None)
            if meta:
                candidates[meta["id"]] = {
                    "id": meta["id"], "trackName": meta["trackName"],
                    "artistName": meta["artistName"], "genreName": meta["genreName"],
                    "text_score": float(score),
                }

        # ── 오디오 유사도 결합 ──
        if query_audio_vec is not None and audio_index.ntotal > 0:
            av = query_audio_vec.reshape(1, -1)
            faiss.normalize_L2(av)
            ak = min(req.limit + 10, audio_index.ntotal)
            a_scores, a_indices = audio_index.search(av, ak)
            audio_score_map: dict[int, float] = {}
            for a_score, a_idx in zip(a_scores[0], a_indices[0]):
                if a_idx < 0 or a_idx >= len(audio_meta):
                    continue
                ameta = next((m for m in audio_meta if m.get("audio_idx") == int(a_idx)), None)
                if ameta:
                    audio_score_map[ameta["id"]] = float(a_score)

            for song_id, entry in candidates.items():
                a_score = audio_score_map.get(song_id, 0.0)
                entry["score"] = round(0.6 * entry["text_score"] + 0.4 * a_score, 4)
                entry["audio_score"] = a_score
        else:
            for entry in candidates.values():
                entry["score"] = entry["text_score"]

    results = sorted(candidates.values(), key=lambda x: x["score"], reverse=True)[:req.limit]
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
        items = song_meta
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
        text_ids  = [m["id"] for m in song_meta]
        audio_ids = [m["id"] for m in audio_meta]
    return {"text_ids": text_ids, "audio_ids": audio_ids}


# ── YouTube 관련곡 ───────────────────────────────────────

_SKIP_KEYWORDS = re.compile(
    r'가사|lyrics|lyric|뮤직뱅크|쇼챔|show.*core|inkigayo|인기가요|방송|뮤뱅|원테이크|노래방|karaoke|cover|커버|반주',
    re.IGNORECASE
)
_PREFER_KEYWORDS = re.compile(r'\bmv\b|official|오피셜', re.IGNORECASE)

def _search_video_id(query: str) -> str | None:
    """yt_dlp로 YouTube 검색 후 MV 우선, 가사/방송 영상 제외해서 videoId 반환"""
    try:
        with yt_dlp.YoutubeDL(YDL_QUIET_OPTS) as ydl:
            search = ydl.extract_info(f"ytsearch5:{query} MV", download=False)
            entries = search.get("entries", [])

            # 1순위: MV/Official 키워드 있는 영상
            for entry in entries:
                title = entry.get("title", "")
                vid = entry.get("id") or entry.get("url", "").split("v=")[-1]
                if vid and len(vid) == 11:
                    if _PREFER_KEYWORDS.search(title) and not _SKIP_KEYWORDS.search(title):
                        logger.info(f"[YTMusic] MV 선택: {title} ({vid})")
                        return vid

            # 2순위: 방송/가사 영상 제외하고 첫 번째
            for entry in entries:
                title = entry.get("title", "")
                vid = entry.get("id") or entry.get("url", "").split("v=")[-1]
                if vid and len(vid) == 11 and not _SKIP_KEYWORDS.search(title):
                    logger.info(f"[YTMusic] 일반 영상 선택: {title} ({vid})")
                    return vid

            # 폴백: 첫 번째
            for entry in entries:
                vid = entry.get("id") or entry.get("url", "").split("v=")[-1]
                if vid and len(vid) == 11:
                    return vid
    except Exception as e:
        logger.warning(f"[YTMusic] videoId 검색 실패: {e}")
    return None


def _related_via_ytmusicapi(video_id: str, title: str, limit: int) -> list[dict]:
    """ytmusicapi get_watch_playlist로 관련곡 수집"""
    tracks = ytmusic_client.get_watch_playlist(videoId=video_id, limit=limit * 2)
    songs = []
    for t in tracks.get("tracks", []):
        t_title = t.get("title", "")
        # 방송/가사 영상 제외
        if _SKIP_KEYWORDS.search(t_title):
            continue
        artists = t.get("artists") or []
        artist_name = artists[0].get("name", "") if artists else ""
        if t_title and t_title.lower() != title.lower():
            songs.append({"title": t_title, "artist": artist_name})
        if len(songs) >= limit:
            break
    return songs


def _related_via_ytdlp(video_id: str, title: str, limit: int) -> list[dict]:
    """yt_dlp 자동재생 라디오로 관련곡 수집 (폴백)"""
    opts = {**YDL_QUIET_OPTS, "playlist_items": f"2:{limit * 3}"}
    songs = []
    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info(
            f"https://www.youtube.com/watch?v={video_id}&list=RD{video_id}",
            download=False,
        )
        for entry in info.get("entries", []):
            t_title  = entry.get("title", "")
            # 방송/가사 영상 제외
            if _SKIP_KEYWORDS.search(t_title):
                continue
            uploader = entry.get("uploader") or entry.get("channel") or ""
            song_name, artist_name = _extract_song_info(t_title, uploader)
            if song_name and song_name.lower() != title.lower():
                songs.append({"title": song_name, "artist": artist_name})
            if len(songs) >= limit:
                break
    return songs


@app.get("/related")
def get_related_songs(
    title:  str = Query(...),
    artist: str = Query(...),
    limit:  int = Query(10),
):
    """YouTube Music 관련곡 반환 (ytmusicapi 인증 우선, yt_dlp 폴백)"""
    try:
        video_id = _search_video_id(f"{title} {artist}")
        if not video_id:
            return {"songs": []}

        # ytmusicapi 인증 모드 우선 시도
        if ytmusic_client is not None:
            try:
                songs = _related_via_ytmusicapi(video_id, title, limit)
                if songs:
                    mode = "인증" if YTMUSIC_AUTH_AVAILABLE else "비인증"
                    logger.info(f"[YTMusic] {title} 관련곡 {len(songs)}개 반환 (ytmusicapi {mode})")
                    return {"songs": songs}
            except Exception as e:
                logger.warning(f"[YTMusic] ytmusicapi 실패, yt_dlp 폴백: {e}")

        # yt_dlp 폴백
        songs = _related_via_ytdlp(video_id, title, limit)
        logger.info(f"[YTMusic] {title} 관련곡 {len(songs)}개 반환 (yt_dlp)")
        return {"songs": songs}

    except Exception as e:
        logger.error(f"[YTMusic] 오류: {e}")
        return {"songs": []}
