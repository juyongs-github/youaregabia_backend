package com.music.music.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Grade {
    ENSEMBLE,
    SESSION,
    SOLOIST,
    MAESTRO,
    LEGEND;
}
