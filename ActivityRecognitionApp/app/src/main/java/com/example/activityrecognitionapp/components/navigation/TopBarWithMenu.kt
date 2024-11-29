package com.example.activityrecognitionapp.components.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.activityrecognitionapp.R
import com.example.activityrecognitionapp.presentation.viewmodels.SupabaseAuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithMenu(onLogoutClicked: () -> Unit) {
    // Obtain the SupabaseAuthViewModel using Hilt
    val supabaseViewModel: SupabaseAuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
    // Collect the current login state from the ViewModel
    val uiLoginState by supabaseViewModel.uiLoginState.collectAsState()

    // State to control the visibility of the dropdown menu
    var expanded by remember { mutableStateOf(false) }

    // Top app bar with title and menu actions
    TopAppBar(
        title = {
            // Display a greeting with the user's name
            Text(
                text = stringResource(id = R.string.hi) + " " + uiLoginState.userName + "!"
            )
        },
        actions = {
            // Icon button to toggle the dropdown menu
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu"
                )
            }
            // Dropdown menu with logout option
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Logout") },
                    onClick = {
                        expanded = false
                        onLogoutClicked()
                    }
                )
            }
        }
    )
}
