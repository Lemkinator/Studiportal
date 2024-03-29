package de.lemke.studiportal.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.TimeoutError
import com.android.volley.toolbox.*
import dagger.hilt.android.qualifiers.ApplicationContext
import de.lemke.studiportal.R
import de.lemke.studiportal.domain.model.Exam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import javax.inject.Inject


class GetStudiportalDataUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getUserSettings: GetUserSettingsUseCase,
) {
    private val parseExamList = ParseStudiportalDataUseCase()
    private val parseStudentData = ParseStudentDataUseCase()
    suspend operator fun invoke(
        successCallback: (data: Pair<Pair<String, String>?, List<Exam>>) -> Unit = { },
        errorCallback: (message: String) -> Unit = { },
        loginSuccessCallback: () -> Unit = { },
        loginErrorCallback: (message: String) -> Unit = { },
        timeoutErrorCallback: () -> Unit = { },
    ): Unit = withContext(Dispatchers.Default) {
        val userSettings = getUserSettings()
        val username = userSettings.username
        val password = userSettings.password
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ) {
            loginErrorCallback(context.getString(R.string.no_internet))
            errorCallback(context.getString(R.string.no_internet))
            return@withContext
        }
        if (connectivityManager.isActiveNetworkMetered && !userSettings.allowMeteredConnection) {
            loginErrorCallback(context.getString(R.string.metered_connection))
            errorCallback(context.getString(R.string.metered_connection))
            return@withContext
        }

        val cookieManager = CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(cookieManager)
        // Instantiate the cache
        val cache = DiskBasedCache(context.cacheDir, 1024 * 1024) // 1MB cap
        // Set up the network to use HttpURLConnection as the HTTP client.
        val network = BasicNetwork(HurlStack())
        // Instantiate the RequestQueue with the cache and network. Start the queue.
        val requestQueue = RequestQueue(cache, network).apply {
            start()
        }

        val asiRequest = StringRequest(
            Request.Method.POST, context.getString(R.string.url_fetch_asi),
            { asiResponse ->
                val error = "error"
                val asi = asiResponse.substringAfter(";asi=", error).substringBefore("\"", error)
                if (asi == error) {
                    onError(context.getString(R.string.error_could_not_get_asi), requestQueue, errorCallback)
                    return@StringRequest
                }
                requestQueue.add(getObserveRequest(asi, requestQueue, successCallback, errorCallback))
            },
            { error -> onError(context.getString(R.string.error_asi, error.message), requestQueue, errorCallback) })

        val loginRequest = object : StringRequest(
            Method.POST, context.getString(R.string.url_login),
            { loginResponse ->
                if (loginResponse.contains(context.getString(R.string.studi_portal_login_failed_message))) {
                    loginErrorCallback(context.getString(R.string.wrong_username_or_password_message))
                } else {
                    loginSuccessCallback()
                    requestQueue.add(asiRequest)
                }
            },
            {
                it.printStackTrace()
                if (it is TimeoutError) {
                    timeoutErrorCallback()
                } else {
                    loginErrorCallback(context.getString(R.string.error_login, it.message))
                    onError(context.getString(R.string.error_login, it.message), requestQueue, errorCallback)
                }
            }) {
            override fun getBodyContentType(): String = "application/x-www-form-urlencoded; charset=UTF-8"

            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["asdf"] = username
                params["fdsa"] = password
                params["submit"] = "Anmelden"
                return params
            }
        }
        requestQueue.add(loginRequest)

    }

    private fun getObserveRequest(
        asi: String,
        requestQueue: RequestQueue,
        onSuccessCallback: (data: Pair<Pair<String, String>?, List<Exam>>) -> Unit,
        onErrorCallback: (message: String) -> Unit
    ) = StringRequest(
        Request.Method.POST, context.getString(R.string.url_observe, asi),
        { observeResponse -> onSuccess(observeResponse, requestQueue, onSuccessCallback) },
        { observeError -> onError(context.getString(R.string.error_observe, observeError.message), requestQueue, onErrorCallback) })

    private fun onError(msg: String, requestQueue: RequestQueue, errorCallback: (message: String) -> Unit) {
        Log.d("Error in GetStudiportalDataUseCase", msg)
        errorCallback(msg)
        logout(requestQueue)
    }

    private fun onSuccess(
        response: String,
        requestQueue: RequestQueue,
        successCallback: (data: Pair<Pair<String, String>?, List<Exam>>) -> Unit
    ) {
        val exams = parseExamList(response)
        val studentData = parseStudentData(response)
        successCallback(Pair(studentData, exams))
        logout(requestQueue)
    }

    private fun logout(requestQueue: RequestQueue) {
        val logoutRequest = StringRequest(
            Request.Method.GET, context.getString(R.string.url_logout),
            { Log.d("GetStudiportalDataUseCase", "logout success") },
            { Log.d("GetStudiportalDataUseCase", "Fehler beim Logout: ${it.message}") })
        requestQueue.add(logoutRequest)
    }
}
