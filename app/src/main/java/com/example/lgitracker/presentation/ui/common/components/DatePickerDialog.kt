package com.example.lgitracker.presentation.ui.common.components

import android.app.DatePickerDialog
import android.util.Log
import android.widget.DatePicker
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.util.Calendar

@Composable
fun DatePickerDialog(callback: (LocalDate) ->Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            val localDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
            callback(localDate)
            Log.d("date picker", "showDatePickerDialog: $localDate")
        },
        year,
        month,
        dayOfMonth
    )

    datePickerDialog.show()
}
