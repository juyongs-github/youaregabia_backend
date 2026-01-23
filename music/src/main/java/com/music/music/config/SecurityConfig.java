package com.music.music.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable);
        http
			.authorizeHttpRequests((requests) -> requests
				// .requestMatchers("/").permitAll()
				// .anyRequest().authenticated()
				.anyRequest().permitAll()
			)
			.formLogin((form) -> Customizer.withDefaults()
			)
			.logout(LogoutConfigurer::permitAll);

        return http.build();
	}

    @Bean // 비밀번호 bcrypt 설정
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean // 로그인 유저 정보 초기 세팅
	public UserDetailsService userDetailsService(PasswordEncoder encoder) {
		String password = encoder.encode("1234");
		UserDetails user = User.withUsername("admin")
            .password(password)
            .roles("USER")
            .build();
		return new InMemoryUserDetailsManager(user);
	}
}