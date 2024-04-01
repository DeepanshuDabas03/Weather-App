package com.weatherapp.fast

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Adjust insertion behavior
    suspend fun insertWeatherData(weather: WeatherEntry)

    @Query("SELECT * FROM weather_data WHERE date = :date AND latitude = :latitude AND longitude = :longitude")
    suspend fun getWeatherByDate(date: String, latitude: Double, longitude: Double): WeatherEntry?
}