package com.example.mintyminutes

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity(), LoginPresenter.LoginView {

    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton

    private lateinit var presenter: LoginPresenter
    private var registeredName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Get the registered name and email from registration
        registeredName = intent.getStringExtra("registered_name")
        val registeredEmail = intent.getStringExtra("registered_email")

        initViews()
        initPresenter()
        setupListeners()

        // Pre-fill email if coming from registration
        registeredEmail?.let { email ->
            emailInput.setText(email)
        }
    }

    private fun initViews() {
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
    }

    private fun initPresenter() {
        presenter = LoginPresenter()
        presenter.attachView(this)
    }

    private fun setupListeners() {
        emailInput.doOnTextChanged { text, _, _, _ ->
            presenter.validateEmail(text.toString())
        }

        passwordInput.doOnTextChanged { text, _, _, _ ->
            presenter.validatePassword(text.toString())
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            presenter.login(email, password, registeredName)
        }

        val signUpText = findViewById<TextView>(R.id.signUp)
        signUpText.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPassword)
        forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    // MVP View interface implementations
    override fun showEmailError(error: String?) {
        emailLayout.error = error
    }

    override fun showPasswordError(error: String?) {
        passwordLayout.error = error
    }

    override fun showLoading(show: Boolean) {
        loginButton.isEnabled = !show
        loginButton.text = if (show) "Logging in..." else "Login"
    }

    override fun showLoginError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onLoginSuccess(userName: String?) {
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, HomeActivity::class.java)
        // Pass the user's name to HomeActivity
        intent.putExtra("user_name", userName ?: "User")
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }
}