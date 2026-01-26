package com.music.music.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    ModelMapper getMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
        .setFieldMatchingEnabled(true) // 필드명 같은 경우 매핑
        .setFieldAccessLevel(AccessLevel.PRIVATE) // getter, setter 없이도 private 필드 접근 허용
        .setMatchingStrategy(MatchingStrategies.LOOSE); // userName, user_name과 같이 비슷한 이름이면 알아서 매핑

        return modelMapper;
    }
}
