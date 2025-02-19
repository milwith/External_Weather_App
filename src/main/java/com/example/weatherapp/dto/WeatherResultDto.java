package com.example.weatherapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WeatherResultDto {
    private String city;
    private double averageTemperature;
    private String hottestDay;
    private String coldestDay;
}
