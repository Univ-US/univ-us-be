package com.univus.app.weather.dto;

import lombok.Data;

@Data
public class WeatherDto {
    private double temp;
    private String description;
    private String icon;
    private String city;
}
