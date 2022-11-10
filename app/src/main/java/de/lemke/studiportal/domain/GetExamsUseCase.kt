package de.lemke.studiportal.domain

import de.lemke.studiportal.data.ExamsRepository
import de.lemke.studiportal.domain.model.Exam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetExamsUseCase @Inject constructor(
    private val examsRepository: ExamsRepository,
) {
    suspend operator fun invoke(category: String? = null): List<Exam> = withContext(Dispatchers.Default) {
        if (category == null) examsRepository.getExams()
        else examsRepository.getExamsWithCategory(category)
    }
}
