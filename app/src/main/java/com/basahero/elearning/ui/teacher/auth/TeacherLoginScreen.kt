package com.basahero.elearning.ui.teacher.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.repository.TeacherProfile

@OptIn(ExperimentalMaterial3Api::class)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle background design elements
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF334155).copy(alpha = 0.03f),
                radius = 600f,
                center = androidx.compose.ui.geometry.Offset(size.width * 1.1f, size.height * -0.1f)
            )
            drawCircle(
                color = Color(0xFF1E293B).copy(alpha = 0.02f),
                radius = 400f,
                center = androidx.compose.ui.geometry.Offset(size.width * -0.2f, size.height * 1.1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Branding
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "BASAhero",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "TEACHER PORTAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(40.dp))

            // Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (viewModel.isSignUpMode) "Create Account" else "Welcome Back",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = if (viewModel.isSignUpMode) "Join the MATATAG learning community" else "Sign in to manage your students",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(32.dp))

                    // Fields
                    AnimatedVisibility(visible = viewModel.isSignUpMode) {
                        Column {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it; viewModel.resetState() },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF64748B)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1E293B),
                                    focusedLabelColor = Color(0xFF1E293B)
                                )
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.resetState() },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF64748B)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E293B),
                            focusedLabelColor = Color(0xFF1E293B)
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.resetState() },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF64748B)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E293B),
                            focusedLabelColor = Color(0xFF1E293B)
                        )
                    )

                    // Error message
                    AnimatedVisibility(visible = authState is TeacherLoginViewModel.AuthState.Error) {
                        val msg = (authState as? TeacherLoginViewModel.AuthState.Error)?.message ?: ""
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    // Action Button
                    Button(
                        onClick = {
                            if (viewModel.isSignUpMode) viewModel.signUp(email, password, fullName)
                            else viewModel.signIn(email, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        enabled = authState !is TeacherLoginViewModel.AuthState.Loading
                    ) {
                        if (authState is TeacherLoginViewModel.AuthState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (viewModel.isSignUpMode) "Create Account" else "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Secondary Actions
            TextButton(onClick = { viewModel.toggleMode() }) {
                Text(
                    text = if (viewModel.isSignUpMode) "Already have an account? Sign In" else "New teacher? Create an account",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(onClick = onBack) {
                Text("← Back to Role Selection", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        }
    }
}
