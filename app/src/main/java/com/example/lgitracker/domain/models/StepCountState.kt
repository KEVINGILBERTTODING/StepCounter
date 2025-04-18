package com.example.lgitracker.domain.models

sealed class StepCountState{
    object Idle : StepCountState()
    data class Success(val step: Int) : StepCountState()
    object Loading : StepCountState()
    data class Error(val message: String?) : StepCountState()
}
