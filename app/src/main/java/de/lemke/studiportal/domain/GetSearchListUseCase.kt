package de.lemke.studiportal.domain

import de.lemke.studiportal.domain.model.Exam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetSearchListUseCase @Inject constructor(
    private val getUserSettings: GetUserSettingsUseCase
) {
    suspend operator fun invoke(search: String?): List<Exam> = withContext(Dispatchers.Default) {
        when {
            search.isNullOrBlank() -> return@withContext emptyList()
            else -> return@withContext listOf<Exam>().filter {
                examContainsKeywords(
                    it,
                    if (search.startsWith("\"") && search.endsWith("\"") && search.length > 2) {
                        search.substring(1, search.length - 1).trim().split(" ").toSet()
                    } else setOf(search)
                )
            }
        }
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
