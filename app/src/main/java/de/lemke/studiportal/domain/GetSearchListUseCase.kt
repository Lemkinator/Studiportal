package de.lemke.studiportal.domain

import android.content.Context
import dagger.hilt.android.qualifiers.ActivityContext
import de.lemke.studiportal.domain.model.Exam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetSearchListUseCase @Inject constructor(
    @ActivityContext private val context: Context,
    private val getExams: GetExamsUseCase,
) {
    suspend operator fun invoke(search: String?): List<Exam> = withContext(Dispatchers.Default) {
        when {
            search.isNullOrBlank() -> return@withContext emptyList()
            else -> return@withContext getExams().filter {
                it.containsKeywords(
                    context,
                    if (search.startsWith("\"") && search.endsWith("\"") && search.length > 2) {
                        search.substring(1, search.length - 1).trim().split(" ").toSet()
                    } else setOf(search)
                )
            }
        }
    }
}
