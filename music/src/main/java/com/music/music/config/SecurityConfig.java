package com.music.music.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.music.music.auth.oauth2.CustomOAuth2AuthorizationRequestResolver;
import com.music.music.auth.oauth2.OAuth2SuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CustomOAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(
                "/api/auth/register",
                "/api/auth/login",
                "/api/auth/email-check",
                "/api/auth/social/**")
            .permitAll()
            .requestMatchers("/api/auth/sms/**").permitAll()
            .requestMatchers("/api/ci/**").permitAll()
            .anyRequest().permitAll())
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .oauth2Login(oauth2 -> oauth2
            .authorizationEndpoint(endpoint -> endpoint
                .authorizationRequestResolver(customOAuth2AuthorizationRequestResolver))
            .successHandler(oAuth2SuccessHandler));

      return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

	// @Bean // 로그인 유저 정보 초기 세팅
	// public UserDetailsService userDetailsService(PasswordEncoder encoder) {
	// 	String password = encoder.encode("1234");
	// 	UserDetails user = User.withUsername("admin")
    //         .password(password)
    //         .roles("USER")
    //         .build();
	// 	return new InMemoryUserDetailsManager(user);
	// }
}
