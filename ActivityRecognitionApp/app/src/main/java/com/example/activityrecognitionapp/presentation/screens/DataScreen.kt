package com.example.activityrecognitionapp.presentation.screens

import android.graphics.Color.LTGRAY
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.activityrecognitionapp.R
import com.example.activityrecognitionapp.presentation.theme.LighterPrimary
import com.example.activityrecognitionapp.presentation.viewmodels.DataScreenViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DataScreen(navController: NavController, viewModel: DataScreenViewModel = hiltViewModel()) {

    // State for the selected tab index - categories D (Day), T (Week), M (Month)
    var selectedTabIndex by remember { mutableStateOf(0) }
    // List of tab titles
    val tabs = listOf("D", "T", "M")

    // Collecting data from the ViewModel
    val activityCounts by viewModel.activityCounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // State for showing the DatePicker dialog
    var showDatePicker by remember { mutableStateOf(false) }
    // State for the selected date
    var selectedDate by remember { mutableStateOf<Date?>(null) }

    // Prepare data and XAxis labels based on selected tab or date
    val (filteredActivityCounts, xAxisLabels) = remember(
        selectedTabIndex,
        selectedDate,
        activityCounts
    ) {
        when {
            selectedDate != null && selectedTabIndex == 0 -> viewModel.prepareDataForSelectedDay(
                selectedDate!!
            )

            selectedDate != null && selectedTabIndex == 1 -> viewModel.prepareDataForWeekTab(
                selectedDate!!
            )

            selectedDate != null && selectedTabIndex == 2 -> viewModel.prepareDataForMonthTab(
                selectedDate!!
            )

            selectedTabIndex == 0 -> viewModel.prepareDataForDayTab()
            selectedTabIndex == 1 -> viewModel.prepareDataForWeekTab(Calendar.getInstance().time)
            selectedTabIndex == 2 -> viewModel.prepareDataForMonthTab(Calendar.getInstance().time)
            else -> Pair(emptyList(), emptyList())
        }
    }

    // Calculate the maximum Y value for chart scaling
    val maxYValue = viewModel.calculateMaxYValue(filteredActivityCounts)

    // Logging for debugging purposes
    Log.d("DataScreen", "Selected Tab: ${tabs[selectedTabIndex]}")
    Log.d("DataScreen", "Filtered Activity Counts: $filteredActivityCounts")
    Log.d("DataScreen", "X-Axis Labels: $xAxisLabels")
    Log.d("DataScreen", "Max Y Value: $maxYValue")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Scaffold(
            modifier = Modifier.fillMaxWidth()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Row with date selection text and icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Display selected date or current date
                    Text(
                        text = selectedDate?.let {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                        } ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .clickable {
                                showDatePicker = true
                            } // Make the text clickable to show DatePicker
                            .padding(1.dp)
                    )
                    // Icon button to open DatePicker
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.arrow),
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // TabRow for selecting Day, Week, or Month
                TabRow(selectedTabIndex = selectedTabIndex,
                    modifier = Modifier,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = LighterPrimary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index }, // Update selected tab index
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        when {
                            isLoading -> {
                                // Show loading indicator while data is being fetched
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }

                            errorMessage != null -> {
                                // Display error message if an error occurs
                                Text(
                                    text = errorMessage ?: "An error occurred.",
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            filteredActivityCounts.isEmpty() -> {
                                // Show message if no data is available
                                Text(
                                    text = "No activity data available.",
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            else -> {
                                // Display the BarChart using AndroidView
                                AndroidView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(400.dp),
                                    factory = { context ->
                                        BarChart(context).apply {
                                            // Configure BarChart appearance
                                            description.isEnabled = false
                                            setDrawGridBackground(false)
                                            setPinchZoom(false)
                                            setScaleEnabled(false)
                                            setDrawBarShadow(false)
                                            setDrawValueAboveBar(true)
                                            animateY(1000)

                                            // Configure X Axis
                                            xAxis.apply {
                                                position = XAxis.XAxisPosition.BOTTOM
                                                setDrawGridLines(false)
                                                labelRotationAngle = -45f
                                                isGranularityEnabled = true
                                                granularity = 1f
                                                textColor = LTGRAY
                                                valueFormatter =
                                                    IndexAxisValueFormatter(xAxisLabels)
                                            }

                                            // Configure Y Axis
                                            axisLeft.apply {
                                                axisMinimum = 0f
                                                axisMaximum = maxYValue * 1.1f // Add 10% margin
                                                textColor = LTGRAY
                                                setDrawGridLines(true)
                                            }
                                            axisRight.isEnabled = false

                                            // Configure Legend
                                            legend.apply {
                                                verticalAlignment =
                                                    Legend.LegendVerticalAlignment.TOP
                                                horizontalAlignment =
                                                    Legend.LegendHorizontalAlignment.CENTER
                                                orientation = Legend.LegendOrientation.HORIZONTAL
                                                setDrawInside(false)
                                                isEnabled = true
                                                textColor = LTGRAY
                                            }

                                            // Prepare and set data for the chart
                                            val barData = viewModel.prepareStackedBarData(
                                                filteredActivityCounts
                                            )
                                            this.data = barData

                                            this.invalidate()
                                        }
                                    },
                                    update = { barChart ->
                                        // Update the chart with new data and labels
                                        barChart.xAxis.valueFormatter =
                                            IndexAxisValueFormatter(xAxisLabels)
                                        barChart.data =
                                            viewModel.prepareStackedBarData(filteredActivityCounts)
                                        barChart.axisLeft.axisMaximum =
                                            maxYValue * 1.1f // Update axis maximum
                                        barChart.notifyDataSetChanged()
                                        barChart.invalidate()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    // Wyświetlanie DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.time
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDateMillis = datePickerState.selectedDateMillis
                    if (selectedDateMillis != null) {
                        val selected = Date(selectedDateMillis)
                        selectedDate = selected
                        // Opcjonalnie: Dodaj wywołanie ViewModelu, aby zaktualizować stan
                        // np. viewModel.prepareDataForSelectedDay(selected)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text(text = "Select Date") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

