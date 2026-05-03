package com.basahero.elearning.ui.student.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.model.Student
import kotlinx.coroutines.launch

@Composable
fun StudentLoginScreen(
    viewModel: StudentLoginViewModel,
    onLoginSuccess: (Student) -> Unit,
    onBack: () -> Unit
) {
    val loginState by viewModel.loginState.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var section  by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Tablet: cap content width at 480dp and centre it
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    // Animated entrance
    val enterAlpha  = remember { Animatable(0f) }
    val enterSlideY = remember { Animatable(32f) }
    LaunchedEffect(Unit) {
        launch { enterAlpha.animateTo(1f,  tween(600)) }
        launch { enterSlideY.animateTo(0f, tween(600, easing = EaseOutCubic)) }
    }

    // Bouncing book emoji
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val bookBounce by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "book_bounce"
    )

    LaunchedEffect(loginState) {
        if (loginState is StudentLoginViewModel.LoginState.Success) {
            onLoginSuccess((loginState as StudentLoginViewModel.LoginState.Success).student)
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .then(if (isTablet) Modifier.width(480.dp) else Modifier.fillMaxWidth())
                .graphicsLayer { alpha = enterAlpha.value; translationY = enterSlideY.value }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (isTablet) 40.dp else 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Bouncing book emoji
                Text(
                    text = "📚",
                    fontSize = 64.sp,
                    modifier = Modifier.offset(y = bookBounce.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Welcome, Hero!",
                    fontSize = if (isTablet) 32.sp else 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Enter your name and section to start your adventure",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(40.dp))

                // Full name field
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; viewModel.resetState() },
                    label = { Text("Full Name", fontWeight = FontWeight.Medium) },
                    placeholder = { Text("e.g. Juan dela Cruz") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    isError = loginState is StudentLoginViewModel.LoginState.Error,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(Modifier.height(16.dp))

                // Section field
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it; viewModel.resetState() },
                    label = { Text("Section", fontWeight = FontWeight.Medium) },
                    placeholder = { Text("e.g. Mabini") },
                    leadingIcon = {
                        Icon(Icons.Default.School, null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus(); viewModel.login(fullName, section) }
                    ),
                    isError = loginState is StudentLoginViewModel.LoginState.Error,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                // Error banner
                AnimatedVisibility(
                    visible = loginState is StudentLoginViewModel.LoginState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit  = fadeOut() + shrinkVertically()
                ) {
                    val msg = (loginState as? StudentLoginViewModel.LoginState.Error)?.message ?: ""
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "⚠️ $msg",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Start learning button
                Button(
                    onClick = { viewModel.login(fullName, section) },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(20.dp),
                    enabled = loginState !is StudentLoginViewModel.LoginState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (loginState is StudentLoginViewModel.LoginState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "🚀  Let's Learn!",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                TextButton(onClick = onBack) {
                    Text("← Back to Role Select", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}