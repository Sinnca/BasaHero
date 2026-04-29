package com.basahero.elearning.ui.student.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.model.Student

// ─────────────────────────────────────────────────────────────────────────────
// StudentLoginScreen
// Simple name + section login. No password needed.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StudentLoginScreen(
    viewModel: StudentLoginViewModel,
    onLoginSuccess: (Student) -> Unit,
    onBack: () -> Unit
) {
    val loginState by viewModel.loginState.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Navigate on success
    LaunchedEffect(loginState) {
        if (loginState is StudentLoginViewModel.LoginState.Success) {
            onLoginSuccess((loginState as StudentLoginViewModel.LoginState.Success).student)
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Header
        Text(
            text = "Student Login",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your name and section to start learning",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Full name field
        OutlinedTextField(
            value = fullName,
            onValueChange = {
                fullName = it
                viewModel.resetState()
            },
            label = { Text("Full Name") },
            placeholder = { Text("e.g. Juan dela Cruz") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            isError = loginState is StudentLoginViewModel.LoginState.Error
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section field
        OutlinedTextField(
            value = section,
            onValueChange = {
                section = it
                viewModel.resetState()
            },
            label = { Text("Section") },
            placeholder = { Text("e.g. Mabini") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.login(fullName, section)
                }
            ),
            isError = loginState is StudentLoginViewModel.LoginState.Error
        )

        // Error message
        AnimatedVisibility(visible = loginState is StudentLoginViewModel.LoginState.Error) {
            val msg = (loginState as? StudentLoginViewModel.LoginState.Error)?.message ?: ""
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Login button
        Button(
            onClick = { viewModel.login(fullName, section) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = loginState !is StudentLoginViewModel.LoginState.Loading
        ) {
            if (loginState is StudentLoginViewModel.LoginState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Start Learning",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("← Back")
        }
    }
}