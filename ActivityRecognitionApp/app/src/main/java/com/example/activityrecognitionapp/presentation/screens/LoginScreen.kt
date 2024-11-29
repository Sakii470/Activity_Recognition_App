package com.example.activityrecognitionapp.presentation.screens

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.activityrecognitionapp.R
import com.example.activityrecognitionapp.components.ButtonComponent
import com.example.activityrecognitionapp.components.ClickableTextComponent
import com.example.activityrecognitionapp.components.DividerTextComponent
import com.example.activityrecognitionapp.components.HeadingTextComponent
import com.example.activityrecognitionapp.components.MyTextFieldComponent
import com.example.activityrecognitionapp.components.NormalTextComponent
import com.example.activityrecognitionapp.components.PasswordTextFieldComponent
import com.example.activityrecognitionapp.components.UnderLinedTextComponent
import com.example.activityrecognitionapp.presentation.states.LoginUiState
import com.example.activityrecognitionapp.presentation.states.UserState
import com.example.activityrecognitionapp.presentation.theme.LighterPrimary
import com.example.activityrecognitionapp.presentation.viewmodels.SupabaseAuthViewModel

/**
 * Composable function for the Login screen.
 *
 * @param navController Navigation controller to handle screen transitions.
 * @param viewModel ViewModel handling authentication logic.
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: SupabaseAuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
) {
    // Observe the current UI state from the ViewModel
    val uiLoginState by viewModel.uiLoginState.collectAsState()
    val userState = uiLoginState.userState
    val isLoggedIn = uiLoginState.isLoggedIn

    // Navigate to home screen upon successful login
    LaunchedEffect(userState) {
        if (userState is UserState.Success && isLoggedIn) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
            Log.d("AppNavigation", "User logged in: ${uiLoginState.isLoggedIn}")
            viewModel.resetUserState()
        }
    }

    // Render the login content with appropriate callbacks
    LoginScreenContent(
        state = uiLoginState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = { viewModel.login(uiLoginState.userEmail, uiLoginState.userPassword) },
        onRegisterClick = { navController.navigate("signUp") },
        onForgotPasswordClick = { /* Handle forgot password */ }
    )

    // Handle the system back button to navigate to the sign-up screen
    BackHandler {
        navController.navigate("signUp") {
            popUpTo("login") { inclusive = true }
        }
    }
}

/**
 * Composable function that builds the content of the Login screen.
 *
 * @param state Current state of the login form.
 * @param onEmailChange Callback for email input changes.
 * @param onPasswordChange Callback for password input changes.
 * @param onLoginClick Callback when the login button is clicked.
 * @param onRegisterClick Callback to navigate to the sign-up screen.
 * @param onForgotPasswordClick Callback for forgot password action.
 */
@Composable
fun LoginScreenContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Greeting text
            NormalTextComponent(value = stringResource(id = R.string.hello))

            // Heading for the login screen
            HeadingTextComponent(
                value = stringResource(id = R.string.welcome),
                modifier = Modifier.padding(16.dp)
            )

            // Email input field
            MyTextFieldComponent(
                labelValue = stringResource(id = R.string.email),
                painterResource = painterResource(id = R.drawable.user),
                value = state.userEmail,
                onValueChange = onEmailChange
            )

            // Password input field
            PasswordTextFieldComponent(
                labelValue = stringResource(id = R.string.password),
                painterResource = painterResource(id = R.drawable.password_locker),
                value = state.userPassword,
                onValueChange = onPasswordChange
            )

            // Display success or error message
            UserStateMessage(userState = state.userState)

            Spacer(modifier = Modifier.height(8.dp))

            // Forgot password clickable text
            UnderLinedTextComponent(
                value = stringResource(id = R.string.forgot_your_password),
                onButtonClick = onForgotPasswordClick
            )

            Spacer(modifier = Modifier.height(15.dp))

            // Login button
            ButtonComponent(
                value = stringResource(id = R.string.login),
                onButtonClick = onLoginClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Divider with "or" text
            DividerTextComponent()

            // Navigate to the register screen
            ClickableTextComponent(
                normalText = "",
                clickableText = " Register",
                onTextSelected = onRegisterClick
            )
        }
    }
}

/**
 * Composable to display messages based on user state (success or error).
 *
 * @param userState Current user state.
 */
@Composable
private fun UserStateMessage(userState: UserState) {
    // Determine the message to display based on the user state
    val message = when (userState) {
        is UserState.Success -> userState.message
        is UserState.Error -> userState.message
        else -> null
    }

    // Show the message if it exists
    message?.let {
        Text(text = it, color = LighterPrimary)
    }
}
