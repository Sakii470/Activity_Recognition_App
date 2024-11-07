package com.example.activityrecognitionapp.presentation.screens

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
import com.example.activityrecognitionapp.presentation.theme.Primary
import com.example.activityrecognitionapp.presentation.viewmodels.SupabaseAuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: SupabaseAuthViewModel = hiltViewModel(),


    ) {

    // Collect the UI state from the ViewModel
    val uiLoginState by viewModel.uiLoginState.collectAsState()
    val userState = uiLoginState.userState

    // Observe userState to navigate to the main screen after successful login
    LaunchedEffect(userState) {
        if (userState is UserState.Success) {
            navController.navigate("mainScreen") {
                popUpTo("login") { inclusive = true }
            }
            viewModel.resetUserState()
        }
    }

    // Build the content of the login screen
    LoginScreenContent(
        state = uiLoginState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::login,
        onRegisterClick = { navController.navigate("signUp") },
        onForgotPasswordClick = { /* handle in forget password case */ }
    )

    // Handle the system back button to navigate back to the sign-up screen
    BackHandler {
        navController.navigate("signUp") {
            popUpTo("login") {
                inclusive = true
            } // Opcjonalne: usuwa ekran logowania ze stosu nawigacji
        }
    }
}

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
        //color = Color.White,
        modifier = Modifier
            .fillMaxSize()
        //.background(Color.White)
        //.padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            NormalTextComponent(value = stringResource(id = R.string.hello))
            HeadingTextComponent(
                value = stringResource(id = R.string.welcome),
                modifier = Modifier.padding(16.dp)
            )
            // Email input field with user icon
            MyTextFieldComponent(
                labelValue = stringResource(id = R.string.email),
                painterResource = painterResource(id = R.drawable.user),
                value = state.userEmail,
                onValueChange = onEmailChange
            )
            // Password input field with lock icon
            PasswordTextFieldComponent(
                labelValue = stringResource(id = R.string.password),
                painterResource = painterResource(id = R.drawable.password_locker),
                value = state.userPassword,
                onValueChange = onPasswordChange
            )
            // Display a message based on userState (e.g., error or success message)
            UserStateMessage(userState = state.userState)

            Spacer(modifier = Modifier.height(8.dp))
            // Underlined text for "Forgot your password?" that is clickable
            UnderLinedTextComponent(
                value = stringResource(id = R.string.forgot_your_password),
                onButtonClick = onForgotPasswordClick
            )

            Spacer(modifier = Modifier.height(15.dp))
            // Button for login action
            ButtonComponent(
                value = stringResource(id = R.string.login),
                onButtonClick = onLoginClick
            )

            Spacer(modifier = Modifier.height(10.dp))
            // Divider line with text "or"
            DividerTextComponent()
            // Clickable text to navigate to the register screen
            ClickableTextComponent(
                normalText = "Don't have an acoount yet?",
                clickableText = " Register",
                onTextSelected = onRegisterClick
            )
        }
    }

}

@Composable
private fun UserStateMessage(userState: UserState) {
    // Determine the message to display based on the user state
    val message = when (userState) {
        is UserState.Success -> userState.message
        is UserState.Error -> userState.message
        else -> null
    }

    message?.let {
        Text(text = it, color = Primary)
    }
}