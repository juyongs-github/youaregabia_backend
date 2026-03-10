package com.music.music.auth.oauth2;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 소셜 로그인 시 항상 계정 선택/로그인 화면을 강제하는 커스텀 리졸버
 * - Google: prompt=select_account (계정 선택 화면)
 * - Kakao:  prompt=login (강제 재로그인)
 */
@Component
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

  private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

  public CustomOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
    this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
        clientRegistrationRepository, "/oauth2/authorization");
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
    return addPrompt(defaultResolver.resolve(request));
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
    return addPrompt(defaultResolver.resolve(request, clientRegistrationId));
  }

  private OAuth2AuthorizationRequest addPrompt(OAuth2AuthorizationRequest authRequest) {
    if (authRequest == null) return null;

    Map<String, Object> additionalParams = new HashMap<>(authRequest.getAdditionalParameters());
    String authUri = authRequest.getAuthorizationUri();

    if (authUri.contains("google")) {
      additionalParams.put("prompt", "select_account");
    } else if (authUri.contains("kakao")) {
      additionalParams.put("prompt", "login");
    } else if (authUri.contains("naver")) {
      additionalParams.put("auth_type", "reauthenticate");
    }

    return OAuth2AuthorizationRequest.from(authRequest)
        .additionalParameters(additionalParams)
        .build();
  }
}
