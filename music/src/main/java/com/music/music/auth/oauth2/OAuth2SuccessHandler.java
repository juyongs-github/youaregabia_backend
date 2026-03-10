package com.music.music.auth.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.music.music.user.entity.User;
import com.music.music.user.entity.UserSocialAccount;
import com.music.music.user.repository.UserSocialAccountRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final PendingSocialStore pendingSocialStore;
  private final UserSocialAccountRepository socialAccountRepository;

  @Value("${app.frontend.url}")
  private String frontendUrl;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException {

    OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
    String provider = oauthToken.getAuthorizedClientRegistrationId();
    OAuth2User oAuth2User = oauthToken.getPrincipal();

    OAuth2UserInfo userInfo = extractUserInfo(provider, oAuth2User.getAttributes());
    String token = UUID.randomUUID().toString();

    Optional<UserSocialAccount> existing = socialAccountRepository.findByProviderAndProviderId(provider,
        userInfo.providerId());

    if (existing.isPresent()) {
      // 기존 유저 -> 세션 토큰 저장 후 콜백 페이지로 (User ID만 저장해 LazyInit 방지)
      // Hibernate lazy proxy에서 getId()는 DB 조회 없이 가능
      Long userId = existing.get().getUser().getId();
      pendingSocialStore.storeSession(token, userId);
      response.sendRedirect(frontendUrl + "/oauth2/callback?token=" + token);
    } else {
      // 신규 유저 -> 본인인증 페이지로 (이름 pre-fill)
      pendingSocialStore.storePending(token, userInfo);
      String name = userInfo.name() != null
          ? URLEncoder.encode(userInfo.name(), StandardCharsets.UTF_8)
          : "";
      response.sendRedirect(frontendUrl + "/social-register?token=" + token + "&name=" + name);
    }
  }

  @SuppressWarnings("unchecked")
  private OAuth2UserInfo extractUserInfo(String provider, Map<String, Object> attributes) {
    return switch (provider.toLowerCase()) {
      case "google" -> new OAuth2UserInfo(
          provider,
          (String) attributes.get("sub"),
          (String) attributes.get("name"),
          (String) attributes.get("email"));

      case "kakao" -> {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile")
            : Map.of();
        String name = profile != null ? (String) profile.get("nickname") : null;
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
        yield new OAuth2UserInfo(provider, String.valueOf(attributes.get("id")), name, email);
      }

      case "naver" -> {
        Map<String, Object> naverResponse = (Map<String, Object>) attributes.get("response");
        yield new OAuth2UserInfo(
            provider,
            (String) naverResponse.get("id"),
            (String) naverResponse.get("name"),
            (String) naverResponse.get("email"));
      }

      default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
    };
  }
}
