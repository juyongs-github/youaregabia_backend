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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	    http.cors(Customizer.withDefaults())
    	// [현재 단계] REST 테스트 편하게 하려고 CSRF disable
        // [JWT + 쿠키(리프레시)로 가면 CSRF 전략을 다시 정해야 함]
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            // ✅ 회원가입/로그인/이메일중복체크 허용
            .requestMatchers(
                "/api/auth/register",
                "/api/auth/login",
                "/api/auth/email-check")
            .permitAll()
            // ✅ [FIX] SMS 인증 API 허용 경로 수정
            // 프론트 호출: /api/auth/sms/send, /api/auth/sms/verify
            .requestMatchers("/api/auth/sms/**").permitAll()
            // [현재 단계] 전체 허용
            // JWT 붙이면: .anyRequest().authenticated()
            .anyRequest().permitAll())
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable);

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
