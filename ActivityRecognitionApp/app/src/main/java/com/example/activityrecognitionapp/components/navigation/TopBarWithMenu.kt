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
    val supabaseViewModel: SupabaseAuthViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
    val uiLoginState by supabaseViewModel.uiLoginState.collectAsState()

    var expanded by remember { mutableStateOf(false) }




    TopAppBar(
        title = {
            Text(text = stringResource(id = R.string.hi) + " " + uiLoginState.userName + "!"
            )
        },
        actions = {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu"
                )
            }
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