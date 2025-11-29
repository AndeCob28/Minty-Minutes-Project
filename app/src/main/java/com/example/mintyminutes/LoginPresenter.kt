package com.example.mintyminutes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginPresenter(private val model: LoginModel = LoginModel()) {

    interface LoginView {
        fun showEmailError(error: String?)
        fun showPasswordError(error: String?)
        fun showLoading(show: Boolean)
        fun showLoginError(message: String)
        fun onLoginSuccess(userName: String?) // Updated to include user name
    }

    private var view: LoginView? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + Job())

    fun attachView(view: LoginView) {
        this.view = view
    }

    fun detachView() {
        this.view = null
    }

    fun validateEmail(email: String) {
        val isValid = model.isValidEmail(email)
        view?.showEmailError(if (email.isNotEmpty() && !isValid) "Invalid email" else null)
    }

    fun validatePassword(password: String) {
        val isValid = model.isValidPassword(password)
        view?.showPasswordError(if (password.isNotEmpty() && !isValid) "Password too short" else null)
    }

    fun login(email: String, password: String, userName: String? = null) {
        // Final validation before login
        if (!model.isValidEmail(email) || !model.isValidPassword(password)) {
            if (!model.isValidEmail(email)) {
                view?.showEmailError("Valid email is required")
            }
            if (!model.isValidPassword(password)) {
                view?.showPasswordError("Password must be at least 6 characters")
            }
            return
        }

        view?.showLoading(true)

        presenterScope.launch {
            val result = withContext(Dispatchers.IO) {
                model.authenticateUser(email, password, userName)
            }

            view?.showLoading(false)

            when (result) {
                is LoginModel.LoginResult.Success -> {
                    view?.onLoginSuccess(result.userName)
                }
                is LoginModel.LoginResult.Error -> {
                    view?.showLoginError(result.message)
                }
            }
        }
    }
}