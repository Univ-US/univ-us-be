package com.univus.app.weather.service;

import com.univus.app.weather.dto.WeatherDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final RestTemplate restTemplate;

    @Value("${univus.api.openweather-api-key}")
    private String apiKey;

    private static final String WEATHER_URL =
            "https://api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&appid={apiKey}&units=metric&lang=kr";

    private static final String GEO_URL =
            "https://api.openweathermap.org/geo/1.0/reverse?lat={lat}&lon={lon}&limit=1&appid={apiKey}";

    private static final String DIRECT_GEO_URL =
            "https://api.openweathermap.org/geo/1.0/direct?q={city},KR&limit=1&appid={apiKey}";

    public WeatherDto getWeather(double lat, double lon) {
        Map weatherData = restTemplate.getForObject(WEATHER_URL, Map.class, lat, lon, apiKey);
        List<Map> geoData = restTemplate.getForObject(GEO_URL, List.class, lat, lon, apiKey);

        Map main = (Map) weatherData.get("main");
        List<Map> weatherList = (List<Map>) weatherData.get("weather");
        Map weather = weatherList.get(0);

        String city = resolveCity(geoData, (String) weatherData.get("name"));

        WeatherDto dto = new WeatherDto();
        dto.setTemp(((Number) main.get("temp")).doubleValue());
        dto.setDescription((String) weather.get("description"));
        dto.setIcon((String) weather.get("icon"));
        dto.setCity(city);

        return dto;
    }

    public WeatherDto getWeatherByCityName(String city) {
        try {
            List<Map> geoData = restTemplate.getForObject(DIRECT_GEO_URL, List.class, city, apiKey);
            if (geoData == null || geoData.isEmpty()) return null;

            Map geo = geoData.get(0);
            double lat = ((Number) geo.get("lat")).doubleValue();
            double lon = ((Number) geo.get("lon")).doubleValue();
            return getWeather(lat, lon);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveCity(List<Map> geoData, String fallback) {
        if (geoData != null && !geoData.isEmpty()) {
            Map geo = geoData.get(0);
            Map localNames = (Map) geo.get("local_names");
            if (localNames != null && localNames.get("ko") != null) {
                return (String) localNames.get("ko");
            }
            if (geo.get("name") != null) {
                return (String) geo.get("name");
            }
        }
        return fallback;
    }
}
