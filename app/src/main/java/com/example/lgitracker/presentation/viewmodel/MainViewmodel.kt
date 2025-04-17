package com.example.lgitracker.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lgitracker.core.data.apis.RetrofitClient
import com.example.lgitracker.core.data.remote.RequestSendStep
import com.example.lgitracker.domain.models.HealthConnectStatusState
import com.example.lgitracker.domain.models.RequestStepState
import com.example.lgitracker.domain.models.StepCountState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class MainViewmodel : ViewModel() {
    private val _healthConnectStatus = MutableStateFlow<HealthConnectStatusState>(
        HealthConnectStatusState.Unknown)
    val healthConnectStatus: StateFlow<HealthConnectStatusState> = _healthConnectStatus
    val providerPackageName = "com.google.android.apps.healthdata"
    private val _countState = MutableStateFlow<StepCountState>(StepCountState.Idle)
    val countState : StateFlow<StepCountState> = _countState
    private val TAG = "main viewmodel"
    private val _postStepState = MutableStateFlow<RequestStepState>(RequestStepState.Idle)
    val postStepState : StateFlow<RequestStepState> = _postStepState


    fun checkHealthConnectStatus(context: Context) {
        val availabilityStatus = HealthConnectClient.getSdkStatus(context, providerPackageName)

        when (availabilityStatus) {
            HealthConnectClient.SDK_UNAVAILABLE -> {
                Log.d(TAG, "checkHealthConnectStatus: status health not available")
                _healthConnectStatus.value = HealthConnectStatusState.NotAvailable
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                _healthConnectStatus.value = HealthConnectStatusState.UpdateRequired
                Log.d(TAG, "checkHealthConnectStatus: status health update required")

            }
            else -> {
                _healthConnectStatus.value = HealthConnectStatusState.Available(
                    HealthConnectClient.getOrCreate(context)
                )
            }
        }
    }

    suspend fun readTodaySteps(healthConnectClient: HealthConnectClient, startDate: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            _countState.emit(StepCountState.Loading)
            try {
                val zoneId = ZoneId.systemDefault()

                val startOfDay = startDate.atStartOfDay(zoneId).toInstant()
                val endOfDay = startDate.plusDays(1).atStartOfDay(zoneId).minusNanos(1).toInstant()

                val response = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )

                val samsungPackageName = "com.sec.android.app.shealth"
                val googleFitPackageName = "com.google.android.apps.fitness"

                // filter by google fit
//                val totalStepsToday = response.records.filter {
//                    it.metadata.dataOrigin.packageName == googleFitPackageName
//                }.sumOf { it.count }

                // all data from health connect
//                val totalStepsToday = response.records.sumOf { it.count }


                // Grouping berdasarkan dataOrigin (sumber data)
                val groupedRecords = response.records.groupBy { it.metadata.dataOrigin.packageName }

                val bestSource = groupedRecords.maxByOrNull { (_, records) ->
                    records.sumOf { it.count }
                }

                val totalStepsToday = bestSource?.value?.sumOf { it.count } ?: 0


                _countState.emit(StepCountState.Success(totalStepsToday.toInt()))
                Log.d(TAG, "readTodaySteps: $totalStepsToday")

            } catch (e: Exception) {
                e.printStackTrace()
                _countState.emit(StepCountState.Error(e.message))

            }
        }
    }


     suspend fun postStep(totalStep: Int?) {
        viewModelScope.launch {
            _postStepState.value = RequestStepState.Loading
            try {
                val dataRequest = RequestSendStep(
                    step_count = totalStep,
                    platform = "android"
                )
                val response = RetrofitClient.apiInterface.postStep(dataRequest)

                if (response.isSuccessful) {
                    _postStepState.value = RequestStepState.Success
                }else {
                    _postStepState.value = RequestStepState.Error("Something went wrong")
                }

            }catch (e: Exception) {
                Log.d(TAG, "postStep: ${e.message}")
                _postStepState.value = RequestStepState.Error(e.message)
            }
        }
    }

}