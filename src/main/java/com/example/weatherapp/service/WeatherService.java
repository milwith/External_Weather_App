package com.example.weatherapp.service;

import com.example.weatherapp.dto.WeatherResponseDto;
import com.example.weatherapp.dto.WeatherResultDto;

import java.util.concurrent.CompletableFuture;

public interface WeatherService {

    CompletableFuture<WeatherResultDto> getWeatherSummary(String city);
}
