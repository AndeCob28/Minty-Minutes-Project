package com.example.mintyminutes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Presenter class that handles registration business logic and mediates between View and Model
 */
class RegisterPresenter(private val model: RegisterModel = RegisterModel()) {

    interface RegisterView {
        fun showNameError(error: String?)
        fun showEmailError(error: String?)
        fun showPasswordError(error: String?)
        fun showConfirmPasswordError(error: String?)
        fun showLoading(show: Boolean)
        fun showRegistrationError(message: String)
        fun showTermsError()
        fun onRegistrationSuccess(name: String, email: String) // Updated to include name
        fun navigateToLogin()
    }

    private var view: RegisterView? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + Job())

    fun attachView(view: RegisterView) {
        this.view = view
    }

    fun detachView() {
        this.view = null
    }

    fun validateName(name: String) {
        val isValid = model.isValidName(name)
        view?.showNameError(if (name.isNotEmpty() && !isValid) "Name must be at least 2 characters" else null)
    }

    fun validateEmail(email: String) {
        val isValid = model.isValidEmail(email)
        view?.showEmailError(if (email.isNotEmpty() && !isValid) "Invalid email format" else null)
    }

    fun validatePassword(password: String) {
        val isValid = model.isValidPassword(password)
        view?.showPasswordError(if (password.isNotEmpty() && !isValid) "Password must be at least 6 characters" else null)
    }

    fun validatePasswordMatch(password: String, confirmPassword: String) {
        val doMatch = model.doPasswordsMatch(password, confirmPassword)
        view?.showConfirmPasswordError(if (confirmPassword.isNotEmpty() && !doMatch) "Passwords don't match" else null)
    }

    fun register(name: String, email: String, password: String, confirmPassword: String, termsAccepted: Boolean) {
        // Final validation before registration
        if (!isValidForm(name, email, password, confirmPassword, termsAccepted)) {
            return
        }

        view?.showLoading(true)

        presenterScope.launch {
            val result = withContext(Dispatchers.IO) {
                model.registerUser(name, email, password)
            }

            view?.showLoading(false)

            when (result) {
                is RegisterModel.RegisterResult.Success -> {
                    view?.onRegistrationSuccess(name, email) // Pass both name and email
                }
                is RegisterModel.RegisterResult.Error -> {
                    view?.showRegistrationError(result.message)
                }
            }
        }
    }

    fun onLoginClicked() {
        view?.navigateToLogin()
    }

    private fun isValidForm(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        termsAccepted: Boolean
    ): Boolean {
        var isValid = true

        if (!model.isValidName(name)) {
            view?.showNameError("Name is required")
            isValid = false
        }

        if (!model.isValidEmail(email)) {
            view?.showEmailError("Valid email is required")
            isValid = false
        }

        if (!model.isValidPassword(password)) {
            view?.showPasswordError("Password must be at least 6 characters")
            isValid = false
        }

        if (!model.doPasswordsMatch(password, confirmPassword)) {
            view?.showConfirmPasswordError("Passwords don't match")
            isValid = false
        }

        if (!termsAccepted) {
            view?.showTermsError()
            isValid = false
        }

        return isValid
    }
}