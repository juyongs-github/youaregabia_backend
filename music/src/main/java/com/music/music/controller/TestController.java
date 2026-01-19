package com.music.music.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.music.music.dto.SongDTO;
import com.music.music.service.MusicApiService;

@Controller
public class TestController {
    @Autowired
    private MusicApiService musicApiService;

    @GetMapping("/")
    public String testView(
        @RequestParam(required = false) String trackName, 
        @RequestParam(required = false) String artistName,
        Model model) {
        if(StringUtils.hasText(trackName) && StringUtils.hasText(artistName)) {
            List<SongDTO> recommendSongList = musicApiService.getRecommendSongList(trackName, artistName);
            model.addAttribute("recommendSongList", recommendSongList);
        }

        return "index";
    }
}
