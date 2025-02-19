package com.example.weatherapp.service.Impl;

import com.example.weatherapp.dto.GeoLocationDto;
import com.example.weatherapp.dto.Result;
import com.example.weatherapp.dto.WeatherResponseDto;
import com.example.weatherapp.dto.WeatherResultDto;
import com.example.weatherapp.service.WeatherService;
import com.example.weatherapp.utils.CommonException;
import com.example.weatherapp.utils.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Service
@Slf4j
@EnableAsync
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final WebClient webClient;

    @Value("${weather.geo.api.url}")
    private String geoApiUrl;

    @Value("${weather.api.url}")
    private String weatherApiUrl;

    @Async
    public CompletableFuture<WeatherResultDto> getWeatherSummary(String city) {
        return CompletableFuture.supplyAsync(() -> getWeatherData(city));
    }

    //get longitude and latitude for the city name
    @Cacheable(value = "weatherCache", key = "#city.toLowerCase()")
    public WeatherResultDto getWeatherData(String city) {
        log.info("Fetching data from API for city: {}", city);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("geocoding-api.open-meteo.com")
                        .path("/v1/search")
                        .queryParam("name", city)
                        .queryParam("count", 1)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(GeoLocationDto.class)
                .flatMap(geoResponse -> processGeoResponse(city, geoResponse))
                .block();
    }

    //using above fetched data get weather data
    private Mono<WeatherResultDto> processGeoResponse(String city, GeoLocationDto geoResponse) {
        if (geoResponse != null && geoResponse.getResults() != null && !geoResponse.getResults().isEmpty()) {
            Result firstResult = geoResponse.getResults().get(0);
            double latitude = firstResult.getLatitude();
            double longitude = firstResult.getLongitude();
            return fetchWeatherData(city, latitude, longitude);
        } else {
            throw  new CommonException(ResponseCode.CITY_NOT_FOUND);
        }
    }

    private Mono<WeatherResultDto> fetchWeatherData(String city, double latitude, double longitude) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.open-meteo.com")
                        .path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("daily", "temperature_2m_mean")
                        .queryParam("past_days", 6)
                        .queryParam("forecast_days", 1)
                        .queryParam("timezone", "auto")
                        .build())
                .retrieve()
                .bodyToMono(WeatherResponseDto.class)
                .map(response -> processWeatherData(city, response));
    }

    //do the calculation for getting summary
    private WeatherResultDto processWeatherData(String city, WeatherResponseDto response) {
        if (response == null || response.getDaily() == null ||
                response.getDaily().getTemperature_2m_mean() == null ||
                response.getDaily().getTime() == null) {
            throw new RuntimeException("Incomplete weather data received");
        }

        List<Double> temperatures = response.getDaily().getTemperature_2m_mean();
        List<String> dates = response.getDaily().getTime();

        if (temperatures.size() != dates.size()) {
            throw new RuntimeException("Mismatch between temperature and date lists");
        }

        double averageTemperature = temperatures.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        int hottestIndex = IntStream.range(0, temperatures.size())
                .boxed()
                .max((i, j) -> Double.compare(temperatures.get(i), temperatures.get(j)))
                .orElse(0);

        int coldestIndex = IntStream.range(0, temperatures.size())
                .boxed()
                .min((i, j) -> Double.compare(temperatures.get(i), temperatures.get(j)))
                .orElse(0);

        String hottestDay = dates.get(hottestIndex);
        String coldestDay = dates.get(coldestIndex);

        log.info("City: {}, Avg Temp: {}, Hottest: {}, Coldest: {}", city, averageTemperature, hottestDay, coldestDay);

        return new WeatherResultDto(city, averageTemperature, hottestDay, coldestDay);
    }
}
