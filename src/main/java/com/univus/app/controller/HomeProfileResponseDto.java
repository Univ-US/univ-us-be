package com.univus.app.controller;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HomeProfileResponseDto {
    private Long univId;
    private String schoolName;
    private String phoneNumber;
    private String youtubeUrl;
    private String clubUrl;
    private String snsUrl;
}
