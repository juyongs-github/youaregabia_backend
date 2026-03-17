package com.music.music.goods.controller;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/orders")
public class TrackingController {

    @Value("${sweettracker.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/track")
    public ResponseEntity<String> track(
            @RequestParam String carrierId,
            @RequestParam String trackingNumber) {

        String url = "https://info.sweettracker.co.kr/api/v1/trackingInfo"
                + "?t_key=" + apiKey
                + "&t_code=" + carrierId
                + "&t_invoice=" + trackingNumber;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"message\":\"배송 조회 서비스에 연결할 수 없습니다.\"}");
        }
    }

    @GetMapping("/track-popup")
    public ResponseEntity<Void> trackPopup(
            @RequestParam String carrierId,
            @RequestParam String trackingNumber) {

        String redirectUrl = "https://info.sweettracker.co.kr/tracking/1"
                + "?t_key=" + apiKey
                + "&t_code=" + carrierId
                + "&t_invoice=" + trackingNumber;

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }
}
