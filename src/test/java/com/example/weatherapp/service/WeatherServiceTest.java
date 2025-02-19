package com.example.weatherapp.service;

import com.example.weatherapp.dto.*;
import com.example.weatherapp.service.Impl.WeatherServiceImpl;
import com.example.weatherapp.utils.CommonException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class WeatherServiceTest {
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
    @InjectMocks
    private WeatherServiceImpl weatherService;

    @BeforeEach
    void setUp() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void testGetWeatherSummary_Success() {

        String city = "London";
        GeoLocationDto geoLocationDto = new GeoLocationDto();
        Result geoResult = new Result();
        geoResult.setLatitude(51.5074);
        geoResult.setLongitude(-0.1278);
        geoLocationDto.setResults(List.of(geoResult));

        WeatherResponseDto weatherResponseDto = new WeatherResponseDto();
        DailyWeatherDto dailyWeatherDto = new DailyWeatherDto();
        dailyWeatherDto.setTime(List.of("2023-09-01", "2023-09-02", "2023-09-03"));
        dailyWeatherDto.setTemperature_2m_mean(List.of(15.5, 18.2, 12.3));
        weatherResponseDto.setDaily(dailyWeatherDto);

        when(responseSpec.bodyToMono(GeoLocationDto.class))
                .thenReturn(Mono.just(geoLocationDto));

        when(responseSpec.bodyToMono(WeatherResponseDto.class))
                .thenReturn(Mono.just(weatherResponseDto));

        CompletableFuture<WeatherResultDto> futureResult = weatherService.getWeatherSummary(city);

        WeatherResultDto result = assertTimeoutPreemptively(Duration.ofSeconds(5), futureResult::join);

        assertNotNull(result);
        assertEquals(city, result.getCity());
        assertEquals("2023-09-02", result.getHottestDay());
        assertEquals("2023-09-03", result.getColdestDay());
        assertEquals(15.33, result.getAverageTemperature(), 0.01);
    }

    @Test
    void testGetWeatherSummary_CityNotFound() {

        String city = "UnknownCity";
        GeoLocationDto emptyGeoResponse = new GeoLocationDto();
        emptyGeoResponse.setResults(List.of());

        when(responseSpec.bodyToMono(GeoLocationDto.class))
                .thenReturn(Mono.just(emptyGeoResponse));

        CompletableFuture<WeatherResultDto> futureResult = weatherService.getWeatherSummary(city);

        CompletionException thrown = assertThrows(
                CompletionException.class,
                () -> futureResult.join()
        );

        assertTrue(thrown.getCause() instanceof CommonException);
    }

    @Test
    void testGetWeatherSummary_ApiFailure() {

        String city = "London";
        RuntimeException apiException = new RuntimeException("API failure");

        when(responseSpec.bodyToMono(GeoLocationDto.class))
                .thenReturn(Mono.error(apiException));

        CompletableFuture<WeatherResultDto> futureResult = weatherService.getWeatherSummary(city);

        CompletionException thrown = assertThrows(
                CompletionException.class,
                () -> futureResult.join()
        );

        assertTrue(thrown.getCause() instanceof RuntimeException);
        assertEquals("API failure", thrown.getCause().getMessage());
    }
}
