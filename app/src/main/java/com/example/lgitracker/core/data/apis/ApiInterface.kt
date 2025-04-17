package com.example.lgitracker.core.data.apis

import com.example.lgitracker.core.data.remote.RequestSendStep
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiInterface {
    @POST("steps/insert")
    suspend fun postStep(
        @Body dataStep: RequestSendStep
    ): Response<Any>
}