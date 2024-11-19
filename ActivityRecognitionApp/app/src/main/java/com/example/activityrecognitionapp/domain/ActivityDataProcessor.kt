package com.example.activityrecognitionapp.domain

import android.graphics.Color
import android.util.Log
import com.example.activityrecognitionapp.presentation.states.DataUiState.ActivityCount
import com.example.activityrecognitionapp.presentation.states.DataUiState.ActivityCountAggregated
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ActivityDataProcessor @Inject constructor() {

    private val inputDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    private val dateHourFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH", Locale.getDefault()) // Zaktualizowany wzorzec

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

    /**
     * Agreguje dane aktywności na poziomie godzinowym.
     */
    fun aggregateActivityData(data: List<ActivityCount>): List<ActivityCountAggregated> {
        return data.groupBy { parseDateHour(it.timestamp) }
            .map { (hour, activities) ->
                ActivityCountAggregated(
                    date = "$hour:00:00", // 'yyyy-MM-dd'T'HH:00:00'
                    stand = activities.filter { it.activity_type.lowercase() == "stand" }.sumOf { it.count ?: 0 },
                    walk = activities.filter { it.activity_type.lowercase() == "walk" }.sumOf { it.count ?: 0 },
                    run = activities.filter { it.activity_type.lowercase() == "run" }.sumOf { it.count ?: 0 }
                )
            }
            .sortedBy { it.date }
    }

    /**
     * Przygotowuje dane dla wybranego dnia.
     */
    fun prepareDataForSelectedDay(
        activityCounts: List<ActivityCountAggregated>,
        selectedDate: Date
    ): Pair<List<ActivityCountAggregated>, List<String>> {
        val selectedLocalDate = selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val dataMap = initializeHourlyData(selectedLocalDate)

        activityCounts.filter { it.date.startsWith(selectedLocalDate.toString()) }
            .forEach { aggregateHourlyData(it, dataMap) }

        val sortedData = dataMap.values.sortedBy { it.date }
        val xAxisLabels = (0..23).map { "$it:00" }

        return Pair(sortedData, xAxisLabels)
    }

    /**
     * Przygotowuje dane dla zakładki "Dzień".
     */
    fun prepareDataForDayTab(activityCounts: List<ActivityCountAggregated>): Pair<List<ActivityCountAggregated>, List<String>> {
        val today = LocalDate.now()
        val dataMap = initializeHourlyData(today)

        activityCounts.filter { it.date.startsWith(today.toString()) }
            .forEach { aggregateHourlyData(it, dataMap) }

        val sortedData = dataMap.values.sortedBy { it.date }
        val xAxisLabels = (0..23).map { "$it:00" }

        return Pair(sortedData, xAxisLabels)
    }

    /**
     * Przygotowuje dane dla zakładki "Tydzień".
     */
    fun prepareDataForWeekTab(
        activityCounts: List<ActivityCountAggregated>,
        referenceDate: Date
    ): Pair<List<ActivityCountAggregated>, List<String>> {
        val endDate = referenceDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val startDate = endDate.minusDays(6)
        val dateList = (0..6).map { startDate.plusDays(it.toLong()) }

        val dataMap = initializeDailyData(dateList)

        activityCounts.filter { activity ->
            val activityDate = parseLocalDate(activity.date)
            activityDate != null && !activityDate.isBefore(startDate) && !activityDate.isAfter(endDate)
        }.forEach { aggregateDailyData(it, dataMap) }

        val sortedData = dateList.map { dataMap[it]!! }
        val xAxisLabels = dateList.map { it.format(DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault())) }

        return Pair(sortedData, xAxisLabels)
    }

    /**
     * Przygotowuje dane dla zakładki "Miesiąc".
     */
    fun prepareDataForMonthTab(
        activityCounts: List<ActivityCountAggregated>,
        referenceDate: Date
    ): Pair<List<ActivityCountAggregated>, List<String>> {
        val localDate = referenceDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val startDate = localDate.withDayOfMonth(1)
        val endDate = localDate.withDayOfMonth(localDate.lengthOfMonth()) // Ustawienie na ostatni dzień miesiąca
        val daysInMonth = startDate.lengthOfMonth()
        val dateList = (1..daysInMonth).map { day ->
            startDate.withDayOfMonth(day)
        }

        val dataMap = initializeDailyData(dateList)

        activityCounts.filter { activity ->
            val activityDate = parseLocalDate(activity.date)
            activityDate != null && !activityDate.isBefore(startDate) && !activityDate.isAfter(endDate)
        }.forEach { aggregateDailyData(it, dataMap) }

        val sortedData = dateList.map { dataMap[it]!! }
        val xAxisLabels = dateList.map { it.dayOfMonth.toString().padStart(2, '0') }

        return Pair(sortedData, xAxisLabels)
    }

    /**
     * Przygotowuje dane do wykresu słupkowego.
     */
    fun prepareStackedBarData(activityCounts: List<ActivityCountAggregated>): BarData {
        val entries = activityCounts.mapIndexed { index, activity ->
            BarEntry(
                index.toFloat(),
                floatArrayOf(
                    activity.stand.toFloat(),
                    activity.walk.toFloat(),
                    activity.run.toFloat()
                )
            )
        }

        val stackedSet = BarDataSet(entries, "Activities").apply {
            stackLabels = arrayOf("Stand", "Walk", "Run")
            colors = listOf(
                Color.parseColor("#1E88E5"),
                Color.parseColor("#43A047"),
                Color.parseColor("#E53935")
            )
            setDrawValues(false)
        }

        return BarData(stackedSet).apply {
            barWidth = 0.9f
        }
    }


    // Funkcja agregująca dane aktywności (zakomentowana)
    // ...

    /**
     * Oblicza maksymalną wartość Y dla skalowania wykresu.
     */
    fun calculateMaxYValue(activityCounts: List<ActivityCountAggregated>): Float {
        return activityCounts.maxOfOrNull { it.stand + it.walk + it.run }?.toFloat() ?: 150f
    }

    /**
     * Formatuje bieżący timestamp.
     */
    fun getCurrentTimestampFormatted(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    // ---------- Private Helper Methods ----------

    private fun parseDateHour(timestamp: String): String {
        return try {
            val localDateTime = LocalDateTime.parse(timestamp, inputDateFormatter)
            localDateTime.format(dateHourFormatter) // 'yyyy-MM-dd'T'HH:mm:ss'
        } catch (e: Exception) {
            Log.e("ActivityDataProcessor", "Error parsing date: $timestamp", e)
            LocalDateTime.now().format(dateHourFormatter)
        }
    }

    private fun parseLocalDate(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr.substring(0, 10), dateFormatter)
        } catch (e: Exception) {
            Log.e("ActivityDataProcessor", "Error parsing date: $dateStr", e)
            null
        }
    }

    private fun initializeHourlyData(date: LocalDate): MutableMap<Int, ActivityCountAggregated> {
        return (0..23).associate { hour ->
            hour to ActivityCountAggregated(
                date = "${date}T${"%02d".format(hour)}:00:00",
                stand = 0,
                walk = 0,
                run = 0
            )
        }.toMutableMap()
    }

    private fun initializeDailyData(dates: List<LocalDate>): MutableMap<LocalDate, ActivityCountAggregated> {
        return dates.associate { date ->
            date to ActivityCountAggregated(
                date = "${date}T00:00:00",
                stand = 0,
                walk = 0,
                run = 0
            )
        }.toMutableMap()
    }

    private fun aggregateHourlyData(activity: ActivityCountAggregated, dataMap: MutableMap<Int, ActivityCountAggregated>) {
        try {
            val localDateTime = LocalDateTime.parse(activity.date, inputDateFormatter)
            val hour = localDateTime.hour
            val existing = dataMap[hour]!!
            dataMap[hour] = existing.copy(
                stand = existing.stand + activity.stand,
                walk = existing.walk + activity.walk,
                run = existing.run + activity.run
            )
            Log.e("ActivityDataProcessor", "Error in aggregateHourlyData for date: ${activity.date}")
        } catch (e: Exception) {
            Log.e("ActivityDataProcessor", "Error in aggregateHourlyData for date: ${activity.date}", e)
        }
    }

    private fun aggregateDailyData(activity: ActivityCountAggregated, dataMap: MutableMap<LocalDate, ActivityCountAggregated>) {
        try {
            val date = LocalDate.parse(activity.date.substring(0, 10), dateFormatter)
            val existing = dataMap[date]!!
            dataMap[date] = existing.copy(
                stand = existing.stand + activity.stand,
                walk = existing.walk + activity.walk,
                run = existing.run + activity.run
            )
        } catch (e: Exception) {
            Log.e("ActivityDataProcessor", "Error in aggregateDailyData for date: ${activity.date}", e)
        }
    }

    private fun activityType(activityType: String): String {
        return activityType.lowercase(Locale.getDefault())
    }
}