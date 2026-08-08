package com.invoicesaver.app.ui

import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.invoicesaver.app.R

@Composable
fun InvoiceSaverRoot() {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val billViewModel: BillViewModel = viewModel()

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val authError by authViewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(authError) {
        authError?.let {
            Toast.makeText(context, context.getString(it), Toast.LENGTH_LONG).show()
            authViewModel.clearMessages()
        }
    }

    when (authState) {
        is AuthState.Initializing -> {
            Surface(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }
        }
        is AuthState.LoggedOut -> AuthFlow(authViewModel)
        is AuthState.Guest -> MainFlow(authViewModel, billViewModel, isGuest = true)
        is AuthState.LoggedIn -> MainFlow(authViewModel, billViewModel, isGuest = false)
    }
}

@Composable
private fun AuthFlow(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val busy by authViewModel.busy.collectAsStateWithLifecycle()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                busy = busy,
                onSignIn = { email, password -> authViewModel.signIn(email, password) },
                onGoSignUp = { navController.navigate("signup") },
                onGuest = { authViewModel.enterGuest() }
            )
        }
        composable("signup") {
            SignUpScreen(
                busy = busy,
                onSignUp = { email, password -> authViewModel.signUp(email, password) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun MainFlow(
    authViewModel: AuthViewModel,
    billViewModel: BillViewModel,
    isGuest: Boolean
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                billViewModel = billViewModel,
                isGuest = isGuest,
                onLogout = { authViewModel.signOut() },
                onHistory = { navController.navigate("history") }
            )
        }
        composable("history") {
            HistoryScreen(
                billViewModel = billViewModel,
                isGuest = isGuest,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun LanguageToggleButton() {
    val locales = remember {
        AppCompatDelegate.getApplicationLocales()
    }
    val isTamil = locales.toLanguageTags().contains("ta")
    TextButton(onClick = {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(if (isTamil) "en" else "ta")
        )
    }) {
        Text(text = stringResource(R.string.language))
    }
}
