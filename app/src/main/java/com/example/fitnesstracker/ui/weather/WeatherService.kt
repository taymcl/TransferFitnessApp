package com.example.fitnesstracker.ui.weather

import android.service.autofill.UserData
import com.google.android.gms.common.api.internal.ApiKey
import retrofit2.*
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {

    @GET("weather?")
    fun getCurrentWeather(@Query("lat") lat: Double, @Query("lon") lon: Double, @Query("apiKey") apiKey: String, @Query("units") units: String) : Call<WeatherResponse>
    @GET("weather?")
    fun getWeather (@Query("q") cityName: String, @Query("appid") apiKey: String): Call<WeatherResponse>

}