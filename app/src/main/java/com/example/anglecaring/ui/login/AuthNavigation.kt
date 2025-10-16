package com.example.anglecaring.ui.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

enum class AuthScreen {
    LOGIN,
    SIGNUP
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthNavigation(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: (String?, String) -> Unit,
    onSignupSuccess: (String, String?, String) -> Unit
) {
    var currentScreen by remember { mutableStateOf(AuthScreen.LOGIN) }
    
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            val duration = 300
            if (targetState == AuthScreen.SIGNUP) {
                // Going to signup - slide up
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(duration)
                ) + fadeIn(animationSpec = tween(duration)) togetherWith
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(duration)
                ) + fadeOut(animationSpec = tween(duration))
            } else {
                // Going to login - slide down
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(duration)
                ) + fadeIn(animationSpec = tween(duration)) togetherWith
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(duration)
                ) + fadeOut(animationSpec = tween(duration))
            }
        },
        label = "Auth Screen Transition"
    ) { screen ->
        when (screen) {
            AuthScreen.LOGIN -> {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = onLoginSuccess,
                    onSignUpClick = {
                        currentScreen = AuthScreen.SIGNUP
                    }
                )
            }
            AuthScreen.SIGNUP -> {
                SignupScreen(
                    viewModel = authViewModel,
                    onSignupSuccess = onSignupSuccess,
                    onLoginClick = {
                        currentScreen = AuthScreen.LOGIN
                    }
                )
            }
        }
    }
} 