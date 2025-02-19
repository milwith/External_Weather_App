package com.example.weatherapp.controller;

import com.example.weatherapp.dto.WeatherResponseDto;
import com.example.weatherapp.dto.WeatherResultDto;
import com.example.weatherapp.service.WeatherService;
import com.example.weatherapp.utils.CommonException;
import com.example.weatherapp.utils.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/{city}")
    public CompletableFuture<ResponseEntity<WeatherResultDto>> getWeather(@PathVariable String city) {
        return weatherService.getWeatherSummary(city)
                .thenApply(ResponseEntity::ok);
    }

}
