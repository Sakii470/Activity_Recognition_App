package com.example.activityrecognitionapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.Typography
import androidx.compose.ui.res.colorResource
import com.example.activityrecognitionapp.ui.theme.Primary
import com.example.loginui.ui.theme.Typography


private val LightColors = lightColorScheme(
    primary = Color(0xFF155555),
    onPrimary = Color.White,
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    tertiary = Color(0xFF888888),
    onTertiary = Color.Gray,
    tertiaryContainer = Color(0xFFF2F2F2),
    onTertiaryContainer = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,


    background = Color(0xFFFFFFFF), // Domyślny kolor tła
    onBackground = Color.Black,



    // Dodaj inne kolory według potrzeb
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF155555),
    onPrimary = Color.Black,
    secondary = Color(0xFFFFF3E0),
    onSecondary = Color.Black,
    tertiary = Color(0xF4444444),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF222222),
    onTertiaryContainer = Color.Gray,
    surface = Color(0xFF000000),
    onSurface = Color.White,

    background = Color(0xFF000000), // Kolor tła dla trybu ciemnego
    onBackground = Color.White,
    error = Color(0xFFCF6679), // Kolor błędu (np. dla komunikatów o błędach)
    onError = Color.Black, // Kolor tekstu na kolorze błędu
    outline = Color(0xFFBBBBBB), // Kolor konturów, np. ramki wokół pól tekstowych
    inverseOnSurface = Color.Black, // Kolor tekstu na tle powierzchni
    inverseSurface = Color.White // Kolor dla tekstu na ciemnym tle powierzchni



)




@Composable
fun ActivityRecognitionAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
//    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme){
        DarkColors
    } else {
        LightColors
    }


//    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
//
//    val colorScheme = when {
//
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}