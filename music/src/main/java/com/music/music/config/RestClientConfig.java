package com.music.music.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    @Qualifier("lastFmRestClient")
    public RestClient lastFmRestClient() {
        return RestClient.builder()
            .baseUrl("http://ws.audioscrobbler.com/2.0/")
            .defaultHeader("User-Agent", "MusicRecommendApp")
            .build();
    }

    @Bean
    @Qualifier("itunesRestClient")
    public RestClient itunesRestClient() {
        return RestClient.builder()
            .baseUrl("https://itunes.apple.com")
            .build();
    }
}
