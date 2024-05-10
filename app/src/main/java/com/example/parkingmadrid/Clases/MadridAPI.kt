package com.example.parkingmadrid.Clases

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MadridAPI {
    @GET("listParking")
    fun getParkingInfo(@Query("language") language: String): Call<List<ParkingInfo>>

    @GET("listParking") // Ejemplo de otro endpoint para los elementos sin ocupaciones
    fun getParkingInfoWithoutOccupation(@Query("language") language: String): Call<List<ParkingInfoWithoutOccupation>>
}
