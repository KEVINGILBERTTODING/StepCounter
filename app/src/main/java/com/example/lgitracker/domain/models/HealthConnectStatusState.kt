package com.example.lgitracker.domain.models

import androidx.health.connect.client.HealthConnectClient

sealed class HealthConnectStatusState{
    object Unknown : HealthConnectStatusState()
    object NotAvailable : HealthConnectStatusState()
    object UpdateRequired : HealthConnectStatusState()
    data class Available(val client: HealthConnectClient) : HealthConnectStatusState()
}