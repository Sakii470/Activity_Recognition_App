package com.example.activityrecognitionapp.components

/*
 * This file contains composable functions for various UI components used in the login interface of the application.
 * It includes components such as text fields, buttons, clickable text, and dividers, which are styled and functional
 * for user authentication purposes. The components are built using Jetpack Compose and are designed to maintain
 * a cohesive theme throughout the app.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.activityrecognitionapp.ui.theme.LighterPrimary
import com.example.activityrecognitionapp.ui.theme.Primary
import com.example.activityrecognitionapp.ui.theme.Secondary
import com.example.activityrecognitionapp.R
import com.example.activityrecognitionapp.domain.BluetoothDevice


@Composable
fun NormalTextComponent(value: String) {
    Text(
        text = value,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        style = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Normal,

            ),
        textAlign = TextAlign.Center
    )
}

@Composable
fun HeadingTextComponent(value: String, modifier: Modifier) {
    Text(
        text = value,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(),
        style = TextStyle(
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Normal,

            ),
        textAlign = TextAlign.Center
    )
}

@Composable
fun getTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
//    unfocusedLabelColor = GrayColor,
//    focusedLabelColor = Primary,
//    cursorColor = Primary,
    unfocusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
    unfocusedIndicatorColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
//    unfocusedLeadingIconColor = Primary,
    focusedLeadingIconColor = LighterPrimary,
    focusedTextColor = MaterialTheme.colorScheme.primary,
//    unfocusedTextColor = Primary
)

@Composable
fun MyTextFieldComponent(labelValue: String, painterResource: Painter, value: String, onValueChange: (String) -> Unit) {

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

@Composable
fun PasswordTextFieldComponent(labelValue: String, painterResource: Painter,value: String,onValueChange: (String) -> Unit) {

    val password = rememberSaveable { mutableStateOf("") }
    val passwordVisible = rememberSaveable {
        mutableStateOf(false)
    }
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

            var description = if (passwordVisible.value) {
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

@Composable
fun ClickableTextComponent(
    normalText: String?,
    clickableText: String,
    onTextSelected: () -> Unit
) {

    val annotatedString = buildAnnotatedString {
        append(normalText)
        pushStringAnnotation(tag = clickableText, annotation = clickableText)
        withStyle(style = SpanStyle(color = Primary)) {
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
           // fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Normal,
            textAlign = TextAlign.Center,
           // color = Color.White
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

@Composable
fun ButtonComponent(value: String, onButtonClick: () -> Unit, modifier: Modifier = Modifier ) {
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

@Composable
fun UnderLinedTextComponent(value: String, onButtonClick: () -> Unit ) {
    Text(
        text = value,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        style = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Normal,
        ), //color = colorResource(id = R.color.colorGray),
        textAlign = TextAlign.Center,
        textDecoration = TextDecoration.Underline
    )
}

@Composable
fun LoadingComponent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Loading...")
    }
}

@Composable
fun BluetoothDeviceList(
    scannedDevices: List<BluetoothDevice>,
    onClick: (BluetoothDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        item {
            HeadingTextComponent(
                value = stringResource(id = R.string.scanned_devices),
//                fontWeight = FontWeight.Bold,
//                fontSize = 24.sp,
                modifier = Modifier.padding(25.dp)
            )
        }

        items(scannedDevices) { device ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { onClick(device) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer)

            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "${device.name ?: "(No name)"} - RSSI: ${device.signalStrength}" )

                }
            }
        }
    }
}



