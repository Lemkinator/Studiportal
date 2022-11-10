package de.lemke.studiportal.domain

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
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
    private val parseExamList: ParseExamListUseCase,
) {
    suspend operator fun invoke(studiportalListener: StudiportalListener?): Unit = withContext(Dispatchers.Default) {
        val userSettings = getUserSettings()
        val username = userSettings.username
        val password = userSettings.password

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
                    onError("ASI konnte nicht ermittelt werden.", studiportalListener, requestQueue)
                    return@StringRequest
                }
                val observeRequest = StringRequest(
                    Request.Method.POST, context.getString(R.string.url_observe, asi),
                    { observeResponse ->
                        val examStart: Int = observeResponse
                            .indexOf("<table cellspacing=\"0\" cellpadding=\"5\" border=\"0\" align=\"center\" width=\"100%\">")
                        val table: String = observeResponse.substring(examStart, observeResponse.indexOf("</table>", examStart))
                        onSuccess(parseExamList(table), studiportalListener, requestQueue)
                    },
                    { observeError ->
                        onError("Fehler beim Abrufen der Prüfungen: ${observeError.message}", studiportalListener, requestQueue)
                    })
                requestQueue.add(observeRequest)
            },
            { error ->
                onError("Fehler beim Abrufen des ASI: ${error.message}", studiportalListener, requestQueue)
            })

        val loginRequest = StringRequest(
            Request.Method.POST, context.getString(R.string.url_login, username, password),
            { loginResponse ->
                if (loginResponse.contains("Anmeldung fehlgeschlagen")) {
                    onError("Anmeldung fehlgeschlagen.", studiportalListener, requestQueue)
                } else {
                    Log.d("login success", "Anmeldung erfolgreich")
                    requestQueue.add(asiRequest)
                }
            },
            { onError("Fehler beim Login: ${it.message}", studiportalListener, requestQueue) })
        requestQueue.add(loginRequest)

    }

    private fun onError(msg: String, studiportalListener: StudiportalListener?, requestQueue: RequestQueue) {
        Log.d("Error in GetStudiportalDataUseCase", msg)
        studiportalListener?.onError(msg)
        logout(requestQueue)
    }

    private fun onSuccess(exams: List<Exam>, studiportalListener: StudiportalListener?, requestQueue: RequestQueue) {
        Log.d("Success in GetStudiportalDataUseCase", exams.toString())
        studiportalListener?.onSuccess(exams)
        logout(requestQueue)
    }

    private fun logout(requestQueue: RequestQueue) {
        val logoutRequest = StringRequest(
            Request.Method.GET, context.getString(R.string.url_logout),
            { Log.d("logout success", "Logout erfolgreich") },
            { Log.d("GetStudiportalDataUseCase", "Fehler beim Logout: ${it.message}") })
        requestQueue.add(logoutRequest)
    }

    private fun examContainsKeywords(exam: Exam, keywords: Set<String>): Boolean {
        for (search in keywords) {
            if (exam.name.contains(search, ignoreCase = true) || //TODO
                exam.comment.contains(search, ignoreCase = true)
            ) return true
        }
        return false
    }
}

interface StudiportalListener {
    fun onSuccess(newExams: List<Exam>)
    fun onError(message: String)
}
