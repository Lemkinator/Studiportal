package de.lemke.studiportal.domain

import de.lemke.studiportal.domain.model.Exam
import javax.inject.Inject

class AddSeparatorToExamsUseCase @Inject constructor() {
    operator fun invoke(exams: List<Exam>): MutableList<Pair<Exam?, String>> {
        val examsWithOutSeparator = exams.map { Pair(it, it.category) }.toMutableList()
        val examsWithSeparator: MutableList<Pair<Exam?, String>> = examsWithOutSeparator.toMutableList()
        var offset = 0
        var oldCategory: String? = null
        examsWithOutSeparator.forEachIndexed { index, pair ->
            if (oldCategory == null || oldCategory != pair.second) {
                examsWithSeparator.add(index + offset, Pair(null, pair.second))
                oldCategory = pair.second
                offset++
            }
        }
        return examsWithSeparator
    }
}
