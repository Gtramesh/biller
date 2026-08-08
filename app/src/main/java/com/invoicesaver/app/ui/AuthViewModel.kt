package com.invoicesaver.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.invoicesaver.app.R
import com.invoicesaver.app.data.BillRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface AuthState {
    object Initializing : AuthState
    object LoggedOut : AuthState
    object Guest : AuthState
    data class LoggedIn(val email: String) : AuthState
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = BillRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<Int?>(null)
    val error: StateFlow<Int?> = _error.asStateFlow()

    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            callbackFlow {
                val listener = repository.authStateListener { user ->
                    trySend(user)
                }
                awaitClose { repository.removeAuthStateListener(listener) }
            }.collectLatest { user ->
                _authState.value = if (user != null) AuthState.LoggedIn(user.email ?: "") else AuthState.LoggedOut
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                Tasks.await(repository.signIn(email.trim(), password))
            } catch (e: Exception) {
                _error.value = authErrorMessage(e)
            } finally {
                _busy.value = false
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                Tasks.await(repository.signUp(email.trim(), password))
            } catch (e: Exception) {
                _error.value = authErrorMessage(e)
            } finally {
                _busy.value = false
            }
        }
    }

    fun signOut() {
        val current = _authState.value
        if (current is AuthState.Guest) {
            _authState.value = AuthState.LoggedOut
        } else {
            repository.signOut()
        }
    }

    fun enterGuest() {
        _authState.value = AuthState.Guest
    }

    fun clearMessages() {
        _error.value = null
        _message.value = null
    }

    private fun authErrorMessage(e: Exception): Int {
        if (e is FirebaseAuthException) {
            val code = e.errorCode
            return when {
                code == "ERROR_EMAIL_ALREADY_IN_USE" ||
                        code == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> R.string.email_in_use
                code == "ERROR_NETWORK_REQUEST_FAILED" -> R.string.network_error
                code == "ERROR_INVALID_EMAIL" -> R.string.invalid_email
                code == "ERROR_WEAK_PASSWORD" -> R.string.password_too_short
                else -> R.string.auth_failed
            }
        }
        return if (e.message?.contains("network", true) == true) R.string.network_error else R.string.auth_failed
    }
}
