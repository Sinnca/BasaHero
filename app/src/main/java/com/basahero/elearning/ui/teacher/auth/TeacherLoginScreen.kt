package com.basahero.elearning.ui.teacher.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.repository.TeacherProfile
import com.basahero.elearning.data.remote.SupabaseClient
import com.basahero.elearning.ui.theme.fredokaFontFamily
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle

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
    val scrollState = rememberScrollState()

    // ── Google Sign-In action ────────────────────────────────────────────────
    val googleSignInAction = SupabaseClient.client.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                is NativeSignInResult.Success -> {
                    viewModel.handleGoogleSignInSuccess()
                }
                is NativeSignInResult.Error -> {
                    viewModel.handleGoogleSignInError(
                        result.message ?: "Google sign-in failed. Please try again."
                    )
                }
                is NativeSignInResult.ClosedByUser -> {
                    // User cancelled — do nothing
                }
                is NativeSignInResult.NetworkError -> {
                    viewModel.handleGoogleSignInError(
                        "No internet connection. Please check your Wi-Fi."
                    )
                }
                else -> {
                    viewModel.handleGoogleSignInError("Sign-in was interrupted.")
                }
            }
        }
    )

    LaunchedEffect(authState) {
        if (authState is TeacherLoginViewModel.AuthState.Success) {
            onLoginSuccess((authState as TeacherLoginViewModel.AuthState.Success).teacher)
        }
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

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
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = if (isTablet) 24.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(if (isTablet) 100.dp else 12.dp))
            // Branding
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(if (isTablet) 120.dp else 56.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFF1E293B), Color(0xFF334155))
                            ), 
                            RoundedCornerShape(if (isTablet) 28.dp else 14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(if (isTablet) 60.dp else 28.dp)
                    )
                }
                Spacer(Modifier.height(if (isTablet) 28.dp else 8.dp))
                Text(
                    text = "BASAhero",
                    fontSize = if (isTablet) 56.sp else 28.sp,
                    fontFamily = fredokaFontFamily,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B),
                    letterSpacing = if (isTablet) (-1.5).sp else (-0.5).sp
                )
                Text(
                    text = "TEACHER PORTAL",
                    fontSize = if (isTablet) 15.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = if (isTablet) 4.sp else 1.sp
                )
            }

            Spacer(Modifier.height(if (isTablet) 40.dp else 12.dp))

            // Login Card
            Card(
                modifier = Modifier
                    .widthIn(max = if (isTablet) 500.dp else 380.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(if (isTablet) 48.dp else 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isTablet) 16.dp else 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(if (isTablet) 48.dp else 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (viewModel.isSignUpMode) "Create Account" else "Welcome",
                        fontSize = if (isTablet) 36.sp else 22.sp,
                        fontFamily = fredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = if (viewModel.isSignUpMode) "Create your teacher account" else "Sign in to manage your students",
                        fontSize = if (isTablet) 17.sp else 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(if (isTablet) 48.dp else 14.dp))

                    // Fields
                    AnimatedVisibility(visible = viewModel.isSignUpMode) {
                        Column {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it; viewModel.resetState() },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF64748B)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1E293B),
                                    focusedLabelColor = Color(0xFF1E293B),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            Spacer(Modifier.height(if (isTablet) 16.dp else 6.dp))
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.resetState() },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF64748B)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E293B),
                            focusedLabelColor = Color(0xFF1E293B),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    Spacer(Modifier.height(if (isTablet) 16.dp else 6.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.resetState() },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { 
                            if (viewModel.isSignUpMode) viewModel.signUp(email, password, fullName)
                            else viewModel.signIn(email, password)
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E293B),
                            focusedLabelColor = Color(0xFF1E293B),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
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

                    Spacer(Modifier.height(24.dp))

                    // Action Button
                    Button(
                        onClick = {
                            if (viewModel.isSignUpMode) viewModel.signUp(email, password, fullName)
                            else viewModel.signIn(email, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isTablet) 72.dp else 44.dp),
                        shape = RoundedCornerShape(if (isTablet) 20.dp else 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isTablet) 6.dp else 2.dp),
                        enabled = authState !is TeacherLoginViewModel.AuthState.Loading
                    ) {
                        if (authState is TeacherLoginViewModel.AuthState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(if (isTablet) 28.dp else 20.dp), color = Color.White, strokeWidth = if (isTablet) 4.dp else 2.dp)
                        } else {
                            Text(
                                text = if (viewModel.isSignUpMode) "Create Account" else "Sign In",
                                fontSize = if (isTablet) 20.sp else 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(if (isTablet) 24.dp else 16.dp))

                    // ── Divider ──────────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = "  or continue with  ",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFE2E8F0)
                        )
                    }

                    Spacer(Modifier.height(if (isTablet) 24.dp else 16.dp))

                    // ── Google Sign-In Button ────────────────────────────────
                    OutlinedButton(
                        onClick = { googleSignInAction.startFlow() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isTablet) 68.dp else 44.dp),
                        shape = RoundedCornerShape(if (isTablet) 20.dp else 10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isTablet) 2.5.dp else 1.5.dp, Color(0xFFE2E8F0)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White
                        ),
                        enabled = authState !is TeacherLoginViewModel.AuthState.Loading
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Google "G" logo drawn with Canvas
                            Canvas(modifier = Modifier.size(if (isTablet) 24.dp else 18.dp)) {
                                // Red arc (top-right)
                                drawArc(
                                    color = Color(0xFFEA4335),
                                    startAngle = -10f, sweepAngle = -110f,
                                    useCenter = true,
                                    size = size
                                )
                                // Yellow arc (bottom-right)
                                drawArc(
                                    color = Color(0xFFFBBC05),
                                    startAngle = -120f, sweepAngle = -80f,
                                    useCenter = true,
                                    size = size
                                )
                                // Green arc (bottom-left)
                                drawArc(
                                    color = Color(0xFF34A853),
                                    startAngle = -200f, sweepAngle = -80f,
                                    useCenter = true,
                                    size = size
                                )
                                // Blue arc (top-left + bar)
                                drawArc(
                                    color = Color(0xFF4285F4),
                                    startAngle = -280f, sweepAngle = -90f,
                                    useCenter = true,
                                    size = size
                                )
                                // White center to make it look like a "G"
                                drawCircle(
                                    color = Color.White,
                                    radius = size.minDimension * 0.32f
                                )
                                // Blue horizontal bar
                                drawRect(
                                    color = Color(0xFF4285F4),
                                    topLeft = androidx.compose.ui.geometry.Offset(
                                        size.width * 0.48f, size.height * 0.38f
                                    ),
                                    size = androidx.compose.ui.geometry.Size(
                                        size.width * 0.52f, size.height * 0.24f
                                    )
                                )
                            }
                            Spacer(Modifier.width(if (isTablet) 16.dp else 12.dp))
                            Text(
                                text = "Continue with Google",
                                fontSize = if (isTablet) 18.sp else 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(if (isTablet) 40.dp else 12.dp))

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
