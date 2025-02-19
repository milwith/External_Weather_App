package com.example.weatherapp.utils;

import lombok.Getter;

@Getter
public enum ResponseCode {
    CITY_NOT_FOUND(404,"Entered city not exists");

    private final int code;
    private final String description;

    ResponseCode(int code,String description){
        this.code=code;
        this.description=description;
    }
}
