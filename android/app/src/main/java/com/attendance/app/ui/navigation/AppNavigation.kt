package com.attendance.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.attendance.app.data.repository.AuthRepository
import com.attendance.app.ui.screens.CheckInScreen
import com.attendance.app.ui.screens.CourseSelectionScreen
import com.attendance.app.ui.screens.EnrollmentScreen
import com.attendance.app.ui.screens.HistoryScreen
import com.attendance.app.ui.screens.LoginScreen
import com.attendance.app.ui.screens.RegisterScreen
import com.attendance.app.ui.screens.WelcomeScreen

sealed class Screen(val route: String) {
    data object Splash    : Screen("splash")
    data object Welcome   : Screen("welcome")
    data object Login     : Screen("login")
    data object Register  : Screen("register")
    /** Enrollment immediately after registration — on complete, go to Login. */
    data object EnrollAfterRegister : Screen("enroll_after_register")
    data object CourseSelection : Screen("course_selection")
    data object Enrollment : Screen("enrollment")
    data object CheckIn   : Screen("check_in/{courseId}") {
        fun createRoute(courseId: String) = "check_in/$courseId"
    }
    data object History   : Screen("history")
}

@Composable
fun AppNavigation(authRepository: AuthRepository) {
    val navController = rememberNavController()

    // Always start on the splash route — navigate away once DataStore emits.
    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        // ── Splash / Auth gate ────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            // Read the first emitted value from DataStore (suspends briefly until ready)
            LaunchedEffect(Unit) {
                val isLoggedIn  = authRepository.isLoggedIn.first()
                val destination = if (isLoggedIn) Screen.CourseSelection.route
                                  else             Screen.Welcome.route
                navController.navigate(destination) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // ── Auth ──────────────────────────────────────────────────────────────

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onSignIn   = { navController.navigate(Screen.Login.route) },
                onRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.CourseSelection.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.EnrollAfterRegister.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EnrollAfterRegister.route) {
            EnrollmentScreen(
                onEnrollmentComplete = {
                    // After face registration, go to Sign In
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = false }
                    }
                },
                onNavigateBack = {
                    // Skip face enrollment → still go to Sign In
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = false }
                    }
                }
            )
        }

        // ── Main app ──────────────────────────────────────────────────────────

        composable(Screen.CourseSelection.route) {
            CourseSelectionScreen(
                onCourseSelected  = { courseId ->
                    navController.navigate(Screen.CheckIn.createRoute(courseId))
                },
                onEnrollmentClick = { navController.navigate(Screen.Enrollment.route) },
                onHistoryClick    = { navController.navigate(Screen.History.route) },
                onLogout          = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Enrollment.route) {
            EnrollmentScreen(
                onEnrollmentComplete = { navController.popBackStack() },
                onNavigateBack       = { navController.popBackStack() }
            )
        }

        composable(Screen.CheckIn.route) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            CheckInScreen(
                courseId          = courseId,
                onCheckInComplete = { navController.popBackStack() },
                onNavigateBack    = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
