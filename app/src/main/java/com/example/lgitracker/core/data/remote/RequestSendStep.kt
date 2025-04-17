package com.example.lgitracker.core.data.remote

import okhttp3.internal.platform.Platform

data class RequestSendStep(
    val step_count: Int? = 0,
    val platform: String? = "android"
)
