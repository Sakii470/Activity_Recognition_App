package com.example.activityrecognitionapp.components

/*
 * This file contains composable functions for various UI components used in the login interface of the application.
 * It includes components such as text fields, buttons, clickable text, and dividers, which are styled and functional
 * for user authentication purposes. The components are built using Jetpack Compose and are designed to maintain
 * a cohesive theme throughout the app.
 */

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.activityrecognitionapp.R
import com.example.activityrecognitionapp.domain.BluetoothDevice
import com.example.activityrecognitionapp.presentation.theme.LighterPrimary
import com.example.activityrecognitionapp.presentation.theme.Primary
import com.example.activityrecognitionapp.presentation.theme.Secondary

/**
 * Displays normal-sized centered text.
 *
 * @param value The text to display.
 * @param modifier Modifier for styling.
 * @param style Text styling options.
 * @param textAlign Alignment of the text.
 */
@Composable
fun NormalTextComponent(
    value: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Normal,
        color = MaterialTheme.colorScheme.onBackground
    ),
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        text = value,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        style = style,
        textAlign = textAlign
    )
}

/**
 * Displays heading-sized centered text.
 *
 * @param value The text to display.
 * @param modifier Modifier for styling.
 * @param style Text styling options.
 * @param textAlign Alignment of the text.
 */
@Composable
fun HeadingTextComponent(
    value: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(
        fontSize = 30.sp,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Normal,
        color = MaterialTheme.colorScheme.onBackground
    ),
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        text = value,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        style = style,
        textAlign = textAlign
    )
}

/**
 * Defines custom colors for text fields.
 *
 * @return TextFieldColors with customized color scheme.
 */
@Composable
fun getTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
    unfocusedIndicatorColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    focusedLeadingIconColor = LighterPrimary,
    focusedLabelColor = LighterPrimary
)

/**
 * Displays a customized text field with a leading icon.
 *
 * @param labelValue The label for the text field.
 * @param painterResource The icon to display.
 * @param value Current text value.
 * @param onValueChange Callback for text changes.
 */
@Composable
fun MyTextFieldComponent(
    labelValue: String,
    painterResource: Painter,
    value: String,
    onValueChange: (String) -> Unit
) {
    val textValue = rememberSaveable { mutableStateOf("") }

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = labelValue) },
        colors = getTextFieldColors(),
        keyboardOptions = KeyboardOptions.Default,
        value = textValue.value,
        onValueChange = {
            textValue.value = it
            onValueChange(it)
        },
        leadingIcon = {
            Icon(
                painter = painterResource, contentDescription = "",
                modifier = Modifier.size(25.dp)
            )
        },
        shape = RoundedCornerShape(10.dp),
    )
}

/**
 * Displays a password text field with visibility toggle.
 *
 * @param labelValue The label for the password field.
 * @param painterResource The icon to display.
 * @param value Current password value.
 * @param onValueChange Callback for password changes.
 */
@Composable
fun PasswordTextFieldComponent(
    labelValue: String,
    painterResource: Painter,
    value: String,
    onValueChange: (String) -> Unit
) {
    val password = rememberSaveable { mutableStateOf("") }
    val passwordVisible = rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = labelValue) },
        colors = getTextFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        value = password.value,
        onValueChange = {
            password.value = it
            onValueChange(it)
        },
        leadingIcon = {
            Icon(
                painter = painterResource, contentDescription = "",
                modifier = Modifier.size(25.dp),
            )
        },
        trailingIcon = {
            val iconImage = if (passwordVisible.value) {
                Icons.Filled.Visibility
            } else {
                Icons.Filled.VisibilityOff
            }

            val description = if (passwordVisible.value) {
                stringResource(id = R.string.hide_password)
            } else {
                stringResource(id = R.string.show_password)
            }

            IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                Icon(imageVector = iconImage, contentDescription = description, tint = Primary)
            }
        },
        visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
        shape = RoundedCornerShape(10.dp),
    )
}

/**
 * Displays clickable text composed of normal and clickable segments.
 *
 * @param normalText The non-clickable part of the text.
 * @param clickableText The clickable segment.
 * @param onTextSelected Callback when clickable text is selected.
 */
@Composable
fun ClickableTextComponent(
    normalText: String?,
    clickableText: String,
    onTextSelected: () -> Unit
) {
    val annotatedString = buildAnnotatedString {
        append(normalText)
        pushStringAnnotation(tag = clickableText, annotation = clickableText)
        withStyle(style = SpanStyle(color = LighterPrimary)) {
            append(clickableText)
        }
    }

    ClickableText(
        text = annotatedString,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        style = TextStyle(
            fontSize = 21.sp,
            fontStyle = FontStyle.Normal,
            textAlign = TextAlign.Center,
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = clickableText, start = offset, end = offset)
                .forEach { span ->
                    if (span.item == clickableText) {
                        onTextSelected()
                    }
                }
        }
    )
}

/**
 * Displays a customizable button with gradient background.
 *
 * @param value The text to display on the button.
 * @param onButtonClick Callback when the button is clicked.
 * @param modifier Modifier for styling.
 */
@Composable
fun ButtonComponent(value: String, onButtonClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onButtonClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(48.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(48.dp)
                .background(
                    brush = Brush.horizontalGradient(listOf(Secondary, Primary)),
                    shape = RoundedCornerShape(50.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Displays a row with dividers and the text "Or" in between.
 */
@Composable
fun DividerTextComponent() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.primary,
            thickness = 1.dp
        )

        Text(modifier = Modifier.padding(8.dp), text = "Or", fontSize = 14.sp)

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.primary,
            thickness = 1.dp
        )
    }
}

/**
 * Displays underlined centered text.
 *
 * @param value The text to display.
 * @param onButtonClick Callback when the text is clicked.
 */
@Composable
fun UnderLinedTextComponent(value: String, onButtonClick: () -> Unit) {
    Text(
        text = value,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        style = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Normal,
        ),
        textAlign = TextAlign.Center,
        textDecoration = TextDecoration.Underline
    )
}

/**
 * Displays a list of scanned Bluetooth devices.
 *
 * @param scannedDevices List of BluetoothDevice to display.
 * @param onClick Callback when a device is clicked.
 * @param modifier Modifier for styling.
 */
@Composable
fun BluetoothDeviceList(
    scannedDevices: List<BluetoothDevice>,
    onClick: (BluetoothDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(scannedDevices) { device ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { onClick(device) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${device.name ?: "(No name)"} - RSSI: ${device.signalStrength}",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

/**
 * Displays an animated activity bar chart representing different activities.
 *
 * @param standingPercentage Percentage of time standing.
 * @param walkingPercentage Percentage of time walking.
 * @param runningPercentage Percentage of time running.
 * @param unknownActivityPercentage Percentage of unknown activity.
 * @param modifier Modifier for styling.
 * @param height Height of the bar chart.
 * @param cornerRadius Corner radius for the bars.
 */
@Composable
fun ActivityBarChart(
    standingPercentage: Float,
    walkingPercentage: Float,
    runningPercentage: Float,
    unknownActivityPercentage: Float,
    modifier: Modifier = Modifier,
    height: Dp = 30.dp,
    cornerRadius: Dp = 8.dp
) {
    // Normalize percentages to ensure total does not exceed 1.0
    val total = standingPercentage + walkingPercentage + runningPercentage
    val normalizedStanding = if (total > 1f) standingPercentage / total else standingPercentage
    val normalizedWalking = if (total > 1f) walkingPercentage / total else walkingPercentage
    val normalizedRunning = if (total > 1f) runningPercentage / total else runningPercentage
    val normalizedUnknownActivity = if (total > 1f) unknownActivityPercentage / total else unknownActivityPercentage

    // Handle case where total is 0
    val finalStanding = if (total == 0f) 0f else normalizedStanding
    val finalWalking = if (total == 0f) 0f else normalizedWalking
    val finalRunning = if (total == 0f) 0f else normalizedRunning
    val finalUnknownActivity = if (total == 0f) 0f else normalizedUnknownActivity

    // Animate the percentages
    val animatedStanding by animateFloatAsState(
        targetValue = finalStanding,
        animationSpec = tween(durationMillis = 1000)
    )
    val animatedWalking by animateFloatAsState(
        targetValue = finalWalking,
        animationSpec = tween(durationMillis = 1000)
    )
    val animatedRunning by animateFloatAsState(
        targetValue = finalRunning,
        animationSpec = tween(durationMillis = 1000)
    )
    val animatedUnknownActivity by animateFloatAsState(
        targetValue = finalUnknownActivity,
        animationSpec = tween(durationMillis = 1000)
    )

    Canvas(
        modifier = modifier
            .height(height)
            .fillMaxWidth()
    ) {
        val barHeight = size.height
        val barWidth = size.width
        val cornerRadiusPx = cornerRadius.toPx()

        var startX = 0f

        // List of segments with their colors
        val segments = listOf(
            Pair(
                animatedStanding,
                Brush.horizontalGradient(listOf(Color(0xFF1E88E5), Color(0xFF1976D2)))
            ), // Blue
            Pair(
                animatedWalking,
                Brush.horizontalGradient(listOf(Color(0xFF43A047), Color(0xFF388E3C)))
            ),  // Green
            Pair(
                animatedRunning,
                Brush.horizontalGradient(listOf(Color(0xFFE53935), Color(0xFFD32F2F)))
            ),    // Red
            Pair(
                animatedUnknownActivity,
                Brush.horizontalGradient(listOf(Color(0xFFBDBDBD), Color(0xFF757575)))
            )
        )

        segments.forEachIndexed { index, segment ->
            val (percentage, brush) = segment
            if (percentage <= 0f) return@forEachIndexed // Skip segments with 0%

            val segmentWidth = barWidth * percentage

            val rect =
                androidx.compose.ui.geometry.Rect(startX, 0f, startX + segmentWidth, barHeight)

            val path = Path().apply {
                when (index) {
                    0 -> {
                        // First segment - rounded left corners
                        addRoundRect(
                            RoundRect(rect, cornerRadiusPx, cornerRadiusPx)
                        )
                    }

                    segments.lastIndex -> {
                        // Last segment - rounded right corners
                        addRoundRect(
                            RoundRect(rect, cornerRadiusPx, cornerRadiusPx)
                        )
                    }

                    else -> {
                        // Middle segments - flat corners
                        addRoundRect(
                            RoundRect(rect, cornerRadiusPx, cornerRadiusPx)
                        )
                    }
                }
            }

            // Draw the segment
            drawPath(
                path = path,
                brush = brush,
                style = Fill
            )

            startX += segmentWidth
        }
    }
}

/**
 * Displays a legend item with a colored box and label.
 *
 * @param color The color of the legend box.
 * @param labelColor The color of the label text.
 * @param label The label text.
 * @param modifier Modifier for styling.
 */
@Composable
fun LegendItem(
    color: Color,
    labelColor: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color = color, shape = RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = labelColor
        )
    }
}

/**
 * Combines an activity bar chart with a legend.
 *
 * @param standingPercentage Percentage of time standing.
 * @param walkingPercentage Percentage of time walking.
 * @param runningPercentage Percentage of time running.
 * @param unknownActivityPercentage Percentage of unknown activity.
 * @param modifier Modifier for styling.
 * @param chartHeight Height of the bar chart.
 * @param cornerRadius Corner radius for the bars.
 * @param legendSpacing Spacing between the chart and the legend.
 */
@Composable
fun ActivityBarChartWithLegend(
    standingPercentage: Float,
    walkingPercentage: Float,
    runningPercentage: Float,
    unknownActivityPercentage: Float,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 30.dp,
    cornerRadius: Dp = 8.dp,
    legendSpacing: Dp = 15.dp
) {
    Column(modifier = modifier) {
        // Bar chart representing activity percentages
        ActivityBarChart(
            standingPercentage = standingPercentage,
            walkingPercentage = walkingPercentage,
            runningPercentage = runningPercentage,
            unknownActivityPercentage = unknownActivityPercentage,
            modifier = Modifier.fillMaxWidth(),
            height = chartHeight,
            cornerRadius = cornerRadius
        )

        Spacer(modifier = Modifier.height(legendSpacing))

        // Legend for the activity chart
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(
                color = Color(0xFF1E88E5), // Blue
                label = "Standing",
                labelColor = MaterialTheme.colorScheme.onBackground
            )
            LegendItem(
                color = Color(0xFF43A047), // Green
                label = "Walking",
                labelColor = MaterialTheme.colorScheme.onBackground
            )
            LegendItem(
                color = Color(0xFFE53935), // Red
                label = "Running",
                labelColor = MaterialTheme.colorScheme.onBackground
            )
            LegendItem(
                color = Color(0xFFBDBDBD), // Gray
                label = "Unknown",
                labelColor = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
