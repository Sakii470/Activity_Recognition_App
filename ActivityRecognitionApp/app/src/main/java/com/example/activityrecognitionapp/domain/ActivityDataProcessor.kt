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

/**
 * ActivityDataProcessor is responsible for processing and aggregating activity data
 * for different time frames (daily, weekly, monthly) and preparing data for chart visualization.
 */
class ActivityDataProcessor @Inject constructor() {

    // Formatter to parse input timestamps in the format "yyyy-MM-dd'T'HH:mm:ss"
    private val inputDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    // Formatter to format dates to "yyyy-MM-dd'T'HH" for hourly aggregation
    private val dateHourFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH", Locale.getDefault())

    // Formatter to parse and format dates in "yyyy-MM-dd"
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

    /**
     * Aggregates activity data on an hourly basis.
     *
     * @param data List of ActivityCount objects containing raw activity data.
     * @return List of ActivityCountAggregated objects aggregated by hour.
     */
    fun aggregateActivityData(data: List<ActivityCount>): List<ActivityCountAggregated> {
        return data.groupBy { parseDateHour(it.timestamp) }
            .map { (hour, activities) ->
                ActivityCountAggregated(
                    date = "$hour:00:00", // Format: 'yyyy-MM-dd'T'HH:00:00'
                    stand = activities.filter { it.activity_type.lowercase() == "stand" }.sumOf { it.count ?: 0 },
                    walk = activities.filter { it.activity_type.lowercase() == "walk" }.sumOf { it.count ?: 0 },
                    run = activities.filter { it.activity_type.lowercase() == "run" }.sumOf { it.count ?: 0 },
                    unknownActivity = activities.filter { it.activity_type.lowercase() == "unknown activity" }.sumOf { it.count ?: 0 }
                )
            }
            .sortedBy { it.date }
    }

    /**
     * Prepares data for a selected day.
     *
     * @param activityCounts List of aggregated activity counts.
     * @param selectedDate The date selected by the user.
     * @return Pair containing the filtered activity counts and corresponding X-axis labels.
     */
    fun prepareDataForSelectedDay(
        activityCounts: List<ActivityCountAggregated>,
        selectedDate: Date
    ): Pair<List<ActivityCountAggregated>, List<String>> {
        val selectedLocalDate = selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val dataMap = initializeHourlyData(selectedLocalDate)

        // Filter and aggregate data for the selected day
        activityCounts.filter { it.date.startsWith(selectedLocalDate.toString()) }
            .forEach { aggregateHourlyData(it, dataMap) }

        val sortedData = dataMap.values.sortedBy { it.date }
        val xAxisLabels = (0..23).map { "$it:00" }

        return Pair(sortedData, xAxisLabels)
    }

    /**
     * Prepares data for the "Day" tab.
     *
     * @param activityCounts List of aggregated activity counts.
     * @return Pair containing the filtered activity counts and corresponding X-axis labels.
     */
    fun prepareDataForDayTab(activityCounts: List<ActivityCountAggregated>): Pair<List<ActivityCountAggregated>, List<String>> {
        val today = LocalDate.now()
        val dataMap = initializeHourlyData(today)

        // Filter and aggregate data for today
        activityCounts.filter { it.date.startsWith(today.toString()) }
            .forEach { aggregateHourlyData(it, dataMap) }

        val sortedData = dataMap.values.sortedBy { it.date }
        val xAxisLabels = (0..23).map { "$it:00" }

        return Pair(sortedData, xAxisLabels)
    }

    /**
     * Prepares data for the "Week" tab.
     *
     * @param activityCounts List of aggregated activity counts.
     * @param referenceDate Reference date to determine the week range.
     * @return Pair containing the filtered activity counts and corresponding X-axis labels.
     */
    fun prepareDataForWeekTab(
        activityCounts: List<ActivityCountAggregated>,
        referenceDate: Date
    ): Pair<List<ActivityCountAggregated>, List<String>> {
        val endDate = referenceDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val startDate = endDate.minusDays(6)
        val dateList = (0..6).map { startDate.plusDays(it.toLong()) }

        val dataMap = initializeDailyData(dateList)

        // Filter and aggregate data within the week range
        activityCounts.filter { activity ->
            val activityDate = parseLocalDate(activity.date)
            activityDate != null && !activityDate.isBefore(startDate) && !activityDate.isAfter(endDate)
        }.forEach { aggregateDailyData(it, dataMap) }

        val sortedData = dateList.map { dataMap[it]!! }
        val xAxisLabels = dateList.map { it.format(DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault())) }

        return Pair(sortedData, xAxisLabels)
    }

    /**
     * Prepares data for the "Month" tab.
     *
     * @param activityCounts List of aggregated activity counts.
     * @param referenceDate Reference date to determine the month range.
     * @return Pair containing the filtered activity counts and corresponding X-axis labels.
     */
    fun prepareDataForMonthTab(
        activityCounts: List<ActivityCountAggregated>,
        referenceDate: Date
    ): Pair<List<ActivityCountAggregated>, List<String>> {
        val localDate = referenceDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val startDate = localDate.withDayOfMonth(1)
        val endDate = localDate.withDayOfMonth(localDate.lengthOfMonth()) // Set to the last day of the month
        val daysInMonth = startDate.lengthOfMonth()
        val dateList = (1..daysInMonth).map { day ->
            startDate.withDayOfMonth(day)
        }

        val dataMap = initializeDailyData(dateList)

        // Filter and aggregate data within the month range
        activityCounts.filter { activity ->
            val activityDate = parseLocalDate(activity.date)
            activityDate != null && !activityDate.isBefore(startDate) && !activityDate.isAfter(endDate)
        }.forEach { aggregateDailyData(it, dataMap) }

        val sortedData = dateList.map { dataMap[it]!! }
        val xAxisLabels = dateList.map { it.format(DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault())) }

        return Pair(sortedData, xAxisLabels)
    }

    /**
     * Prepares data for a stacked bar chart.
     *
     * @param activityCounts List of aggregated activity counts.
     * @return BarData object containing the prepared data for the chart.
     */
    fun prepareStackedBarData(activityCounts: List<ActivityCountAggregated>): BarData {
        val entries = activityCounts.mapIndexed { index, activity ->
            BarEntry(
                index.toFloat(),
                floatArrayOf(
                    activity.stand.toFloat(),
                    activity.walk.toFloat(),
                    activity.run.toFloat(),
                    activity.unknownActivity.toFloat()
                )
            )
        }

        val stackedSet = BarDataSet(entries, "Activities").apply {
            stackLabels = arrayOf("Stand", "Walk", "Run", "Unknown")
            colors = listOf(
                Color.parseColor("#1E88E5"), // Stand color
                Color.parseColor("#43A047"), // Walk color
                Color.parseColor("#E53935"), // Run color
                Color.parseColor("#BDBDBD")  // Unknown activity color
            )
            setDrawValues(false) // Disable drawing values on the bars
        }

        return BarData(stackedSet).apply {
            barWidth = 0.9f // Set bar width
        }
    }

    /**
     * Calculates the maximum Y value for chart scaling.
     *
     * @param activityCounts List of aggregated activity counts.
     * @return Maximum Y value as a Float.
     */
    fun calculateMaxYValue(activityCounts: List<ActivityCountAggregated>): Float {
        return activityCounts.maxOfOrNull { it.stand + it.walk + it.run + it.unknownActivity }?.toFloat() ?: 150f
    }

    /**
     * Formats the current timestamp.
     *
     * @return Formatted current timestamp as a String.
     */
    fun getCurrentTimestampFormatted(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    // ---------- Private Helper Methods ----------

    /**
     * Parses a timestamp string to extract the hour component.
     *
     * @param timestamp Timestamp string in the format "yyyy-MM-dd'T'HH:mm:ss".
     * @return Formatted date-hour string in the format "yyyy-MM-dd'T'HH".
     */
    private fun parseDateHour(timestamp: String): String {
        return try {
            val localDateTime = LocalDateTime.parse(timestamp, inputDateFormatter)
            localDateTime.format(dateHourFormatter) // Format: 'yyyy-MM-dd'T'HH'
        } catch (e: Exception) {
            Log.e("ActivityDataProcessor", "Error parsing date: $timestamp", e)
            LocalDateTime.now().format(dateHourFormatter) // Fallback to current hour
        }
    }

    /**
     * Parses a date string to a LocalDate object.
     *
     * @param dateStr Date string in the format "yyyy-MM-dd'T'HH:mm:ss".
     * @return LocalDate object or null if parsing fails.
     */
    private fun parseLocalDate(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr.substring(0, 10), dateFormatter) // Extract "yyyy-MM-dd"
        } catch (e: Exception) {
            Log.e("ActivityDataProcessor", "Error parsing date: $dateStr", e)
            null
        }
    }

    /**
     * Initializes a map for hourly data with default values.
     *
     * @param date The date for which to initialize the hourly data.
     * @return MutableMap mapping each hour to an ActivityCountAggregated object.
     */
    private fun initializeHourlyData(date: LocalDate): MutableMap<Int, ActivityCountAggregated> {
        return (0..23).associate { hour ->
            hour to ActivityCountAggregated(
                date = "${date}T${"%02d".format(hour)}:00:00",
                stand = 0,
                walk = 0,
                run = 0,
                unknownActivity = 0
            )
        }.toMutableMap()
    }

    /**
     * Initializes a map for daily data with default values.
     *
     * @param dates List of dates for which to initialize the daily data.
     * @return MutableMap mapping each date to an ActivityCountAggregated object.
     */
    private fun initializeDailyData(dates: List<LocalDate>): MutableMap<LocalDate, ActivityCountAggregated> {
        return dates.associate { date ->
            date to ActivityCountAggregated(
                date = "${date}T00:00:00",
                stand = 0,
                walk = 0,
                run = 0,
                unknownActivity = 0
            )
        }.toMutableMap()
    }

    /**
     * Aggregates activity data into hourly data map.
     *
     * @param activity Aggregated activity count for a specific hour.
     * @param dataMap MutableMap containing hourly aggregated data.
     */
    private fun aggregateHourlyData(activity: ActivityCountAggregated, dataMap: MutableMap<Int, ActivityCountAggregated>) {
        try {
            val localDateTime = LocalDateTime.parse(activity.date, inputDateFormatter)
            val hour = localDateTime.hour
            val existing = dataMap[hour]!!
            dataMap[hour] = existing.copy(
                stand = existing.stand + activity.stand,
                walk = existing.walk + activity.walk,
                run = existing.run + activity.run,
                unknownActivity = existing.unknownActivity + activity.unknownActivity
            )
        } catch (e: Exception) {
            Log.e("ActivityDataProcessor", "Error in aggregateHourlyData for date: ${activity.date}", e)
        }
    }

    /**
     * Aggregates activity data into daily data map.
     *
     * @param activity Aggregated activity count for a specific day.
     * @param dataMap MutableMap containing daily aggregated data.
     */
    private fun aggregateDailyData(activity: ActivityCountAggregated, dataMap: MutableMap<LocalDate, ActivityCountAggregated>) {
        try {
            val date = LocalDate.parse(activity.date.substring(0, 10), dateFormatter)
            val existing = dataMap[date]!!
            dataMap[date] = existing.copy(
                stand = existing.stand + activity.stand,
                walk = existing.walk + activity.walk,
                run = existing.run + activity.run,
                unknownActivity = existing.unknownActivity + activity.unknownActivity
            )
        } catch (e: Exception) {
            Log.e("ActivityDataProcessor", "Error in aggregateDailyData for date: ${activity.date}", e)
        }
    }

    /**
     * Converts activity type string to lowercase.
     *
     * @param activityType The activity type string.
     * @return Lowercased activity type string.
     */
    private fun activityType(activityType: String): String {
        return activityType.lowercase(Locale.getDefault())
    }
}
