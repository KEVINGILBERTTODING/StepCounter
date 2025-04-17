package com.example.lgitracker.presentation.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import com.example.lgitracker.core.data.remote.RequestSendStep
import com.example.lgitracker.domain.models.HealthConnectStatusState
import com.example.lgitracker.domain.models.RequestStepState
import com.example.lgitracker.presentation.viewmodel.MainViewmodel
import com.example.lgitracker.domain.models.StepCountState
import com.example.lgitracker.presentation.ui.common.components.DatePickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate


@Composable
fun MainContent(viewmodel: MainViewmodel, modifier: Modifier) {

    val statusHealthConnect by viewmodel.healthConnectStatus.collectAsState()
    val context = LocalContext.current
    val healthConnectClient = remember { HealthConnectClient.getOrCreate(context) }
    val countState by viewmodel.countState.collectAsState()
    val scope = rememberCoroutineScope()
    val TAG = "main activtiy"
    val isShowDatePicker = remember { mutableStateOf(false) }
    val selectedDate = remember { mutableStateOf(LocalDate.now()) }
    val postStepApiState by viewmodel.postStepState.collectAsState()

    val PERMISSIONS =
        setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
        )

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(PERMISSIONS)) {
            scope.launch(Dispatchers.IO) { readStep(healthConnectClient, selectedDate.value, viewmodel)  }
        } else {
            Toast.makeText(context, "Permission health connect required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect (Unit){
        viewmodel.checkHealthConnectStatus(context)
    }


    LaunchedEffect(statusHealthConnect) {
        if (statusHealthConnect is HealthConnectStatusState.UpdateRequired) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=${viewmodel.providerPackageName}&url=healthconnect%3A%2F%2Fonboarding")
            ).apply {
                setPackage("com.android.vending")
                putExtra("overlay", true)
                putExtra("callerId", context.packageName)
            }
            context.startActivity(intent)
        }else if (statusHealthConnect is HealthConnectStatusState.Available) {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (!granted.containsAll(PERMISSIONS)) {
                Log.d(TAG, "MainContent: $permissionLauncher")
                permissionLauncher.launch(PERMISSIONS)
            } else {
                viewmodel.readTodaySteps(healthConnectClient, selectedDate.value)
            }

        }else if (statusHealthConnect is HealthConnectStatusState.NotAvailable) {
            Toast.makeText(context, "Health connect not available", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        when (val data = countState) {
            is StepCountState.Loading -> {
                CircularProgressIndicator(
                    Modifier
                        .size(20.dp)
                        .align(Alignment.CenterHorizontally))
            }
            is StepCountState.Success -> {
                Text(modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = "${data.step}",
                    fontSize = 40.sp,
                    style = TextStyle.Default.copy(
                        fontWeight = FontWeight.W600
                    )
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = {isShowDatePicker.value = !isShowDatePicker.value }
                ) {
                    Text("Pilih tanggal")
                }

                Spacer(Modifier.height(20.dp))

                when(val requestPostStep = postStepApiState) {
                    is RequestStepState.Loading -> {
                        CircularProgressIndicator(
                            Modifier
                                .size(15.dp)
                                .align(Alignment.CenterHorizontally))
                    }

                    else -> {
                        Button(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    viewmodel.postStep(data.step)
                                }
                            }
                        ) {
                            Text("Post to API")
                        }
                    }
                }

            }
            is StepCountState.Error -> {
                Toast.makeText(context, data.message, Toast.LENGTH_SHORT).show()
            }else  -> {}

        }

    }


    if (isShowDatePicker.value) {
        DatePickerDialog { localDate ->
            isShowDatePicker.value = !isShowDatePicker.value
            selectedDate.value = localDate
            scope.launch(Dispatchers.IO) { readStep(healthConnectClient, selectedDate.value, viewmodel) }
        }
    }

}

    private suspend fun readStep(healthConnectClient: HealthConnectClient, selectedDate: LocalDate,
                         viewmodel: MainViewmodel
    ) {
        viewmodel.readTodaySteps(healthConnectClient, selectedDate)
    }