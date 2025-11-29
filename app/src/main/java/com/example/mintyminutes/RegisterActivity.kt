package com.example.mintyminutes

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged


/**
 * RegisterActivity implementing MVP View interface
 * Handles UI interactions and delegates business logic to Presenter
 */
class RegisterActivity : AppCompatActivity(), RegisterPresenter.RegisterView {

    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText

    private lateinit var nameError: TextView
    private lateinit var emailError: TextView
    private lateinit var passwordError: TextView
    private lateinit var confirmPasswordError: TextView

    private lateinit var termsCheckbox: CheckBox
    private lateinit var registerButton: Button
    private lateinit var loginText: TextView

    private lateinit var presenter: RegisterPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        initPresenter()
        setupListeners()
    }

    private fun initViews() {
        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)

        nameError = findViewById(R.id.nameError)
        emailError = findViewById(R.id.emailError)
        passwordError = findViewById(R.id.passwordError)
        confirmPasswordError = findViewById(R.id.confirmPasswordError)

        termsCheckbox = findViewById(R.id.termsCheckbox)
        registerButton = findViewById(R.id.registerButton)
        loginText = findViewById(R.id.loginText)
    }

    private fun initPresenter() {
        presenter = RegisterPresenter()
        presenter.attachView(this)
    }

    private fun setupListeners() {
        nameInput.doOnTextChanged { text, _, _, _ ->
            presenter.validateName(text.toString())
        }

        emailInput.doOnTextChanged { text, _, _, _ ->
            presenter.validateEmail(text.toString())
        }

        passwordInput.doOnTextChanged { text, _, _, _ ->
            presenter.validatePassword(text.toString())
            presenter.validatePasswordMatch(text.toString(), confirmPasswordInput.text.toString())
        }

        confirmPasswordInput.doOnTextChanged { text, _, _, _ ->
            presenter.validatePasswordMatch(passwordInput.text.toString(), text.toString())
        }

        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()
            val termsAccepted = termsCheckbox.isChecked

            presenter.register(name, email, password, confirmPassword, termsAccepted)
        }

        loginText.setOnClickListener {
            presenter.onLoginClicked()
        }
    }

    // MVP View interface implementations
    override fun showNameError(error: String?) {
        if (error != null) {
            nameError.text = error
            nameError.visibility = View.VISIBLE
        } else {
            nameError.visibility = View.GONE
        }
    }

    override fun showEmailError(error: String?) {
        if (error != null) {
            emailError.text = error
            emailError.visibility = View.VISIBLE
        } else {
            emailError.visibility = View.GONE
        }
    }

    override fun showPasswordError(error: String?) {
        if (error != null) {
            passwordError.text = error
            passwordError.visibility = View.VISIBLE
        } else {
            passwordError.visibility = View.GONE
        }
    }

    override fun showConfirmPasswordError(error: String?) {
        if (error != null) {
            confirmPasswordError.text = error
            confirmPasswordError.visibility = View.VISIBLE
        } else {
            confirmPasswordError.visibility = View.GONE
        }
    }

    override fun showLoading(show: Boolean) {
        registerButton.isEnabled = !show
        registerButton.text = if (show) "Creating Account..." else "Create Account"
    }

    override fun showRegistrationError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showTermsError() {
        Toast.makeText(this, "Please accept Terms & Conditions", Toast.LENGTH_SHORT).show()
    }

    override fun onRegistrationSuccess(name: String, email: String) {
        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_LONG).show()

        // Navigate back to login with name and email pre-filled
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("registered_name", name)
        intent.putExtra("registered_email", email)
        startActivity(intent)
        finish()
    }

    override fun navigateToLogin() {
        finish() // Go back to login screen
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }
}