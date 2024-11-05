package com.example.activityrecognitionapp.screens

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
import com.example.activityrecognitionapp.data.model.LoginUiState
import com.example.activityrecognitionapp.data.model.UserState
import com.example.activityrecognitionapp.ui.theme.Primary
import com.example.activityrecognitionapp.viewmodels.SupabaseAuthViewModel

@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: SupabaseAuthViewModel = hiltViewModel(),
) {
    // Collect the state from ViewModel to observe user input and authentication status
    val uiLoginUiState by viewModel.uiLoginState.collectAsState()
    // Display the sign-up screen content
    SignUpScreenContent(
        state = uiLoginUiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSignUpClick = viewModel::signUp,
        onLoginClick = { navController.navigate("login") }
    )

}

@Composable
fun SignUpScreenContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Surface(
        //color = Color.White,
        modifier = Modifier
            .fillMaxSize()

    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
        ) {
            NormalTextComponent(value = stringResource(id = R.string.hello))
            HeadingTextComponent(
                value = stringResource(id = R.string.create_account),
                modifier = Modifier.padding(16.dp)
            )
            // Email input field with a user icon
            MyTextFieldComponent(
                labelValue = stringResource(id = R.string.email),
                painterResource = painterResource(id = R.drawable.user),
                value = state.userEmail,
                onValueChange = onEmailChange
            )
            // Password input field with a lock icon
            PasswordTextFieldComponent(
                labelValue = stringResource(id = R.string.password),
                painterResource = painterResource(id = R.drawable.password_locker),
                value = state.userPassword,
                onValueChange = onPasswordChange
            )
            // Display a message based on the current user state, such as success or error
            UserStateMessage(userState = state.userState)

            Spacer(modifier = Modifier.height(50.dp))
            // Button for the registration action
            ButtonComponent(
                value = stringResource(id = R.string.register),
                onButtonClick = onSignUpClick
            )


            DividerTextComponent()
            // Clickable text for users to navigate to the login screen if they already have an account
            ClickableTextComponent(normalText = "Already have an account? ",
                clickableText = stringResource(
                    id = R.string.login
                ),
                onTextSelected = onLoginClick
            )

        }

    }

}


@Composable
private fun UserStateMessage(userState: UserState) {
    // Display a message based on the user state (e.g., success or error message)
    val message = when (userState) {
        is UserState.Success -> userState.message
        is UserState.Error -> userState.message
        else -> null
    }

    message?.let {
        Text(text = it, color = Primary)
    }
}