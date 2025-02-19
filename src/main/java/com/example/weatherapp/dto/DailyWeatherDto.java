package com.example.weatherapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyWeatherDto {
    private List<String> time;
    private List<Double> temperature_2m_mean;
}
