package com.example.weatherapp.controller;

import com.example.weatherapp.dto.WeatherResultDto;
import com.example.weatherapp.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class WeatherControllerTest {

    @Mock
    private WeatherService weatherService;
    @InjectMocks
    private WeatherController weatherController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetWeather_Success() throws Exception{
        String city = "London";
        WeatherResultDto weatherResultDto = new WeatherResultDto(city, 15.43, "2025-02-02", "2025-02-03");

        CompletableFuture<WeatherResultDto> future = CompletableFuture.completedFuture(weatherResultDto);

        when(weatherService.getWeatherSummary(city)).thenReturn(future);

        CompletableFuture<ResponseEntity<WeatherResultDto>> response = weatherController.getWeather(city);

        assertNotNull(response);
        assertEquals(200, response.get().getStatusCode().value());
        assertEquals(city, response.get().getBody().getCity());
        assertEquals(15.43, response.get().getBody().getAverageTemperature(), 0.01);
        assertEquals("2025-02-02", response.get().getBody().getHottestDay());
        assertEquals("2025-02-03", response.get().getBody().getColdestDay());
    }


    @Test
    void testGetWeather_CityNotFound() {
        String city = "UnknownCity";

        CompletableFuture<WeatherResultDto> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Entered city not exists"));

        when(weatherService.getWeatherSummary(city)).thenReturn(future);

        CompletableFuture<ResponseEntity<WeatherResultDto>> responseFuture = weatherController.getWeather(city);

        CompletionException completionException = assertThrows(CompletionException.class, responseFuture::join);
        assertTrue(completionException.getCause() instanceof RuntimeException);
        assertEquals("Entered city not exists", completionException.getCause().getMessage());
    }

}
