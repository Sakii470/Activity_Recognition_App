package com.example.activityrecognitionapp.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.activityrecognitionapp.presentation.states.LoginUiState
import com.example.activityrecognitionapp.presentation.states.UserState
import com.example.activityrecognitionapp.presentation.theme.Primary
import com.example.activityrecognitionapp.presentation.viewmodels.SupabaseAuthViewModel

/**
 * Composable function for the Sign-Up screen.
 *
 * @param navController Navigation controller to handle screen transitions.
 * @param viewModel ViewModel handling authentication logic.
 */
@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: SupabaseAuthViewModel = hiltViewModel(),
) {
    // Observe the current UI state from the ViewModel
    val loginUiState by viewModel.uiLoginState.collectAsState()

    // Render the sign-up content with appropriate callbacks
    SignUpScreenContent(
        loginUiState = loginUiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onNameChange = viewModel::onNameChange,
        onSignUpClick = {
            viewModel.signUp(
                loginUiState.userName,
                loginUiState.userEmail,
                loginUiState.userPassword
            )
        },
        onLoginClick = { navController.navigate("login") }
    )
}

/**
 * Composable function that builds the content of the Sign-Up screen.
 *
 * @param loginUiState Current state of the login form.
 * @param onEmailChange Callback for email input changes.
 * @param onPasswordChange Callback for password input changes.
 * @param onNameChange Callback for name input changes.
 * @param onSignUpClick Callback when the sign-up button is clicked.
 * @param onLoginClick Callback to navigate to the login screen.
 */
@Composable
fun SignUpScreenContent(
    loginUiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit
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

            // Heading for creating an account
            HeadingTextComponent(
                value = stringResource(id = R.string.create_account),
                modifier = Modifier.padding(16.dp)
            )

            // Name input field
            MyTextFieldComponent(
                labelValue = stringResource(id = R.string.name),
                painterResource = painterResource(id = R.drawable.user),
                value = loginUiState.userName,
                onValueChange = onNameChange
            )

            // Email input field
            MyTextFieldComponent(
                labelValue = stringResource(id = R.string.email),
                painterResource = painterResource(id = R.drawable.user),
                value = loginUiState.userEmail,
                onValueChange = onEmailChange
            )

            // Password input field
            PasswordTextFieldComponent(
                labelValue = stringResource(id = R.string.password),
                painterResource = painterResource(id = R.drawable.password_locker),
                value = loginUiState.userPassword,
                onValueChange = onPasswordChange
            )

            // Display success or error message
            UserloginUiStateMessage(userloginUiState = loginUiState.userState)

            Spacer(modifier = Modifier.height(50.dp))

            // Register button
            ButtonComponent(
                value = stringResource(id = R.string.register),
                onButtonClick = onSignUpClick
            )

            // Divider and login navigation
            DividerTextComponent()
            ClickableTextComponent(
                normalText = "",
                clickableText = stringResource(id = R.string.login),
                onTextSelected = onLoginClick
            )
        }
    }
}

/**
 * Composable to display messages based on user state (success or error).
 *
 * @param userloginUiState Current user state.
 */
@Composable
private fun UserloginUiStateMessage(userloginUiState: UserState) {
    // Determine the message to display based on the user state
    val message = when (userloginUiState) {
        is UserState.Success -> userloginUiState.message
        is UserState.Error -> userloginUiState.message
        else -> null
    }

    // Show the message if it exists
    message?.let {
        Text(text = it, color = Primary)
    }
}
