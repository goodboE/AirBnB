package com.example.airbnb

import retrofit2.Call
import retrofit2.http.GET

interface HouseService {
    @GET("/v3/cfb07f19-21a8-4a6a-b3e6-91f7c7ffd556")
    fun getHouseList(): Call<HouseDto>
}