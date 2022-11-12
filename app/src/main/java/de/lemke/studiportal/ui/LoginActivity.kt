package de.lemke.studiportal.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.ActivityLoginBinding
import de.lemke.studiportal.domain.*
import de.lemke.studiportal.domain.model.Exam
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var successCallback: (exams: List<Exam>) -> Unit = {}
    private var errorCallback: (message: String) -> Unit = {}
    private var loginSuccessCallback: () -> Unit = {}
    private var loginErrorCallback: (message: String) -> Unit = {}
    private var username: String = ""
    private var password: String = ""
    private var time: Long = 0
    private var loginTryCount = 0
    private val loginTryCountMax = 3
    private var loginSuccess = false

    @Inject
    lateinit var getUserSettings: GetUserSettingsUseCase

    @Inject
    lateinit var updateUserSettings: UpdateUserSettingsUseCase

    @Inject
    lateinit var getStudiportalData: GetStudiportalDataUseCase

    @Inject
    lateinit var updateExams: UpdateExamsUseCase

    @Inject
    lateinit var demo: DemoUseCase

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initEditTexts()
        initCallbacks()
        initFooterButton()
        initOnBackPressed()
    }

    private fun initEditTexts() {
        lifecycleScope.launch {
            val userSettings = getUserSettings()
            binding.editTextUsername.setText(userSettings.username)
            binding.editTextPassword.setText(userSettings.password)
            binding.editTextUsername.doAfterTextChanged { username = it.toString() }
            binding.editTextPassword.doAfterTextChanged { password = it.toString() }
        }
    }

    private fun initCallbacks() {
        successCallback = { exams ->
            lifecycleScope.launch {
                updateExams(exams, false)
                openNextActivity()
            }
        }
        errorCallback = { message ->
            if (loginSuccess) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.error)
                    .setMessage(getString(R.string.login_success_observe_error, message))
                    .setPositiveButton(R.string.ok) { _, _ -> openNextActivity() }
                    .create()
                    .show()
            }
        }
        loginSuccessCallback = {
            loginSuccess = true
        }
        loginErrorCallback = { message ->
            loginSuccess = false
            if (loginTryCount < loginTryCountMax) {
                loginTryCount++
                lifecycleScope.launch {
                    getStudiportalData(
                        successCallback = successCallback,
                        errorCallback = errorCallback,
                        loginSuccessCallback = loginSuccessCallback,
                        loginErrorCallback = loginErrorCallback,
                    )
                }
            } else {
                lifecycleScope.launch { updateUserSettings { it.copy(username = "", password = "") } }
                AlertDialog.Builder(this@LoginActivity)
                    .setTitle(getString(R.string.error))
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, null)
                    .create()
                    .show()
                loginTryCount = 0
                binding.loginFooterButtonProgress.visibility = View.GONE
                binding.loginFooterButton.visibility = View.VISIBLE
            }
        }
    }

    private fun initFooterButton() {
        if (resources.configuration.screenWidthDp < 360) {
            binding.loginFooterButton.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.loginFooterButton.setOnClickListener {
            val isDemo = username.equals(demo.username, ignoreCase = true)
            if (isDemo) username = demo.username
            if (username.isNotEmpty() && password.isNotEmpty() || isDemo) {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                    currentFocus?.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
                binding.loginFooterButton.visibility = View.GONE
                binding.loginFooterButtonProgress.visibility = View.VISIBLE
                lifecycleScope.launch {
                    updateUserSettings { it.copy(username = username, password = password) }
                    if (isDemo) {
                        demo.initDemoExams()
                        openNextActivity()
                    } else getStudiportalData(
                        successCallback = successCallback,
                        errorCallback = errorCallback,
                        loginSuccessCallback = loginSuccessCallback,
                        loginErrorCallback = loginErrorCallback,
                    )
                }
            } else {
                Toast.makeText(this, getString(R.string.username_or_password_empty), Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun initOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                lifecycleScope.launch {
                    if (System.currentTimeMillis() - time < 3000) finishAffinity()
                    else {
                        Toast.makeText(this@LoginActivity, resources.getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show()
                        time = System.currentTimeMillis()
                    }
                }
            }
        })
    }

    private fun openNextActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}