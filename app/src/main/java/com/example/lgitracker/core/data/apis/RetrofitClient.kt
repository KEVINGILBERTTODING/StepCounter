package com.example.lgitracker.core.data.apis

import android.provider.SyncStateContract.Constants
import com.example.lgitracker.base.util.Constant
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl(Constant.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiInterface: ApiInterface = retrofit.create(ApiInterface::class.java)
}