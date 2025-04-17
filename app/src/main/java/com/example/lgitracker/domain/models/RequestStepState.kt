package com.example.lgitracker.domain.models

sealed class RequestStepState{
    object Idle : RequestStepState()
    object Success : RequestStepState()
    object Loading : RequestStepState()
    data class Error(val message: String?) : RequestStepState()
}
