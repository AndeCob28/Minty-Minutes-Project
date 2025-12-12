package com.example.mintyminutes

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged

/**
 * RegisterActivity implementing MVP View interface
 * Handles UI interactions and delegates business logic to Presenter
 */
class RegisterActivity : AppCompatActivity(), RegisterPresenter.RegisterView {

    companion object {
        private const val REQUEST_CODE_TERMS = 1001
    }

    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText

    private lateinit var nameError: TextView
    private lateinit var emailError: TextView
    private lateinit var passwordError: TextView
    private lateinit var confirmPasswordError: TextView

    private lateinit var termsCheckbox: CheckBox
    private lateinit var termsText: TextView
    private lateinit var termsStatus: TextView
    private lateinit var registerButton: Button
    private lateinit var loginText: TextView

    private lateinit var presenter: RegisterPresenter
    private var isTermsAccepted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        initPresenter()
        setupListeners()
        setupTermsClickListener()
        setupInputListeners()
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
        termsText = findViewById(R.id.termsText)
        termsStatus = findViewById(R.id.termsStatus)
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
            updateRegisterButtonState()
        }

        emailInput.doOnTextChanged { text, _, _, _ ->
            presenter.validateEmail(text.toString())
            updateRegisterButtonState()
        }

        passwordInput.doOnTextChanged { text, _, _, _ ->
            presenter.validatePassword(text.toString())
            presenter.validatePasswordMatch(text.toString(), confirmPasswordInput.text.toString())
            updateRegisterButtonState()
        }

        confirmPasswordInput.doOnTextChanged { text, _, _, _ ->
            presenter.validatePasswordMatch(passwordInput.text.toString(), text.toString())
            updateRegisterButtonState()
        }

        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            // Pass the terms acceptance status to presenter
            presenter.register(name, email, password, confirmPassword, isTermsAccepted)
        }

        loginText.setOnClickListener {
            presenter.onLoginClicked()
        }
    }

    private fun setupTermsClickListener() {
        // Make terms text clickable
        termsText.setOnClickListener {
            // Launch TermsActivity for reading
            val intent = Intent(this, TermsActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_TERMS)
        }

        // Checkbox should only be enabled after terms are accepted
        termsCheckbox.isEnabled = false
        termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            isTermsAccepted = isChecked
            updateRegisterButtonState()

            if (isChecked) {
                termsStatus.text = "Terms accepted ✓"
                termsStatus.setTextColor(ContextCompat.getColor(this, R.color.green_400))
            } else {
                termsStatus.text = "Terms not accepted yet"
                termsStatus.setTextColor(ContextCompat.getColor(this, R.color.red_400))
            }
        }
    }

    private fun setupInputListeners() {
        val inputs = listOf(nameInput, emailInput, passwordInput, confirmPasswordInput)

        inputs.forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    updateRegisterButtonState()
                }
            })
        }
    }

    // Handle the result from TermsActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_TERMS) {
            if (resultCode == RESULT_OK) {
                // User accepted terms in TermsActivity
                val termsAccepted = data?.getBooleanExtra(TermsActivity.EXTRA_TERMS_ACCEPTED, false) ?: false

                if (termsAccepted) {
                    // Enable and check the checkbox
                    termsCheckbox.isEnabled = true
                    termsCheckbox.isChecked = true

                    // Update status message
                    termsStatus.text = "Terms accepted ✓"
                    termsStatus.setTextColor(ContextCompat.getColor(this, R.color.green_400))
                    isTermsAccepted = true
                }
            } else {
                // User pressed back without accepting
                termsStatus.text = "Please read and accept Terms & Conditions"
                termsStatus.setTextColor(ContextCompat.getColor(this, R.color.red_400))
            }
            updateRegisterButtonState()
        }
    }

    // Update register button state based on form validation
    private fun updateRegisterButtonState() {
        // Basic form validation
        val isFormValid = nameInput.text.toString().isNotBlank() &&
                emailInput.text.toString().isNotBlank() &&
                passwordInput.text.toString().isNotBlank() &&
                confirmPasswordInput.text.toString().isNotBlank() &&
                isTermsAccepted

        registerButton.isEnabled = isFormValid

        // Visual feedback
        if (isFormValid) {
            registerButton.alpha = 1f
            registerButton.isEnabled = true
        } else {
            registerButton.alpha = 0.6f
            registerButton.isEnabled = false
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
        updateRegisterButtonState()
    }

    override fun showEmailError(error: String?) {
        if (error != null) {
            emailError.text = error
            emailError.visibility = View.VISIBLE
        } else {
            emailError.visibility = View.GONE
        }
        updateRegisterButtonState()
    }

    override fun showPasswordError(error: String?) {
        if (error != null) {
            passwordError.text = error
            passwordError.visibility = View.VISIBLE
        } else {
            passwordError.visibility = View.GONE
        }
        updateRegisterButtonState()
    }

    override fun showConfirmPasswordError(error: String?) {
        if (error != null) {
            confirmPasswordError.text = error
            confirmPasswordError.visibility = View.VISIBLE
        } else {
            confirmPasswordError.visibility = View.GONE
        }
        updateRegisterButtonState()
    }

    override fun showLoading(show: Boolean) {
        registerButton.isEnabled = !show
        registerButton.text = if (show) "Creating Account..." else "Create Account"
    }

    override fun showRegistrationError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showTermsError() {
        // Update the terms status message
        termsStatus.text = "Please accept Terms & Conditions to continue"
        termsStatus.setTextColor(ContextCompat.getColor(this, R.color.red_400))

        // If terms aren't accepted, launch the TermsActivity
        if (!isTermsAccepted) {
            val intent = Intent(this, TermsActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_TERMS)
        }

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

        // Optional: Add transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun navigateToLogin() {
        finish() // Go back to login screen
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    // Handle back button press
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}