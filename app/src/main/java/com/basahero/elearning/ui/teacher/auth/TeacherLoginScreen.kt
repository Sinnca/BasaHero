package com.basahero.elearning.ui.teacher.auth

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.repository.TeacherProfile

@Composable
fun TeacherLoginScreen(
    viewModel: TeacherLoginViewModel,
    onLoginSuccess: (TeacherProfile) -> Unit,
    onBack: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(authState) {
        if (authState is TeacherLoginViewModel.AuthState.Success) {
            onLoginSuccess((authState as TeacherLoginViewModel.AuthState.Success).teacher)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (viewModel.isSignUpMode) "Create Teacher Account" else "Teacher Login",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "BASAhero — Teacher Dashboard",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(36.dp))

        // Full name field (sign up only)
        AnimatedVisibility(visible = viewModel.isSignUpMode) {
            Column {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; viewModel.resetState() },
                    label = { Text("Full Name") },
                    placeholder = { Text("e.g. Ma. Santos") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; viewModel.resetState() },
            label = { Text("Email Address") },
            placeholder = { Text("teacher@school.edu.ph") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            isError = authState is TeacherLoginViewModel.AuthState.Error
        )

        Spacer(Modifier.height(12.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.resetState() },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (viewModel.isSignUpMode) viewModel.signUp(email, password, fullName)
                else viewModel.signIn(email, password)
            }),
            isError = authState is TeacherLoginViewModel.AuthState.Error
        )

        // Error message
        AnimatedVisibility(visible = authState is TeacherLoginViewModel.AuthState.Error) {
            val msg = (authState as? TeacherLoginViewModel.AuthState.Error)?.message ?: ""
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ErrorOutline, null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(28.dp))

        // Primary action button
        Button(
            onClick = {
                if (viewModel.isSignUpMode) viewModel.signUp(email, password, fullName)
                else viewModel.signIn(email, password)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = authState !is TeacherLoginViewModel.AuthState.Loading
        ) {
            if (authState is TeacherLoginViewModel.AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (viewModel.isSignUpMode) "Create Account" else "Sign In",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Toggle sign in / sign up
        TextButton(onClick = { viewModel.toggleMode() }) {
            Text(
                text = if (viewModel.isSignUpMode) "Already have an account? Sign In"
                       else "New teacher? Create an account",
                fontSize = 13.sp
            )
        }

        TextButton(onClick = onBack) { Text("← Back", fontSize = 13.sp) }
    }
}
