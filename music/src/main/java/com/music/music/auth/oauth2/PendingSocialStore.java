package com.music.music.auth.oauth2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class PendingSocialStore {

  // 신규 소셜 유저: token -> OAuth2UserInfo (회원가입 대기)
  private final Map<String, OAuth2UserInfo> pendingMap = new ConcurrentHashMap<>();

  // 기존 소셜 유저: token -> userId (LazyInit 방지를 위해 ID만 저장)
  private final Map<String, Long> sessionMap = new ConcurrentHashMap<>();

  public void storePending(String token, OAuth2UserInfo info) {
    pendingMap.put(token, info);
  }

  // 1회 사용 후 제거
  public OAuth2UserInfo getPending(String token) {
    return pendingMap.remove(token);
  }

  public void storeSession(String token, Long userId) {
    sessionMap.put(token, userId);
  }

  // 1회 사용 후 제거
  public Long getSession(String token) {
    return sessionMap.remove(token);
  }
}
