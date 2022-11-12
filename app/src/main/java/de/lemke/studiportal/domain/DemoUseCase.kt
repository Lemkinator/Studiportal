package de.lemke.studiportal.domain

import de.lemke.studiportal.data.ExamsRepository
import de.lemke.studiportal.domain.model.Exam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.Random
import java.util.UUID
import javax.inject.Inject

class DemoUseCase @Inject constructor(
    private val examsRepository: ExamsRepository,
    private val sendNotification: SendNotificationUseCase,
    private val updateUserSettings: UpdateUserSettingsUseCase,
) {
    private val random = Random()
    val username = "DEMO"
    suspend operator fun invoke(exams: List<Exam>, notifyAboutChanges: Boolean): Boolean = withContext(Dispatchers.Default) {
        updateUserSettings { it.copy(lastRefresh = LocalDateTime.now()) }
        val oldExams = examsRepository.getExams()
        examsRepository.updateExams(exams)
        val changedExams = exams.filter { exam ->
            oldExams.find { it == exam } == null
        }
        if (notifyAboutChanges) {
            changedExams.forEach { sendNotification(it, notifyAboutChanges) }
        }
        return@withContext changedExams.isNotEmpty()
    }

    private fun List<Exam>.withRandomExamAdded(exam: Exam = getRandomExam()): MutableList<Exam> {
        val newExams = this.toMutableList()
        val index = this.indexOfLast { it.category == exam.category }
        if (index == -1) newExams.add(exam)
        else newExams.add(index + 1, exam)
        return newExams
    }

    suspend fun updateExams(notifyAboutChanges: Boolean, addNewExam: Boolean = random.nextBoolean()): Boolean = withContext(Dispatchers.Default) {
        updateUserSettings { it.copy(lastRefresh = LocalDateTime.now()) }
        val exams = if (addNewExam) examsRepository.getExams().withRandomExamAdded()
        else examsRepository.getExams()
        val oldExams = examsRepository.getExams()
        examsRepository.updateExams(exams)
        val changedExams = exams.filter { exam ->
            oldExams.find { it == exam } == null
        }
        if (notifyAboutChanges) {
            changedExams.forEach { sendNotification(it, notifyAboutChanges) }
        }
        return@withContext changedExams.isNotEmpty()
    }

    suspend fun initDemoExams() = withContext(Dispatchers.Default) {
        updateUserSettings { it.copy(lastRefresh = LocalDateTime.now()) }
        examsRepository.updateExams(demoExams)
    }

    private fun getDemoExamName(name: String) = "Demo $name ${UUID.randomUUID()}"
    private val demoExams = List(14) {
        when (it) {
            0 -> Exam.create(
                examNumber = getDemoExamName("Account"),
                name = "Demo Account",
                kind = "KO",
                category = "Accounts",
                bonus = "60.0",
            )
            1 -> Exam.create(
                examNumber = getDemoExamName("Account"),
                name = "Demo Account",
                kind = "KO",
                category = "Accounts",
                malus = "9.0",
            )
            2 -> Exam.create(
                examNumber = getDemoExamName("Account"),
                name = "Demo Account",
                kind = "KO",
                category = "Accounts",
            )
            3 -> Exam.create(
                examNumber = getDemoExamName("PL"),
                name = "Demo PL",
                kind = "PL",
                grade = "1.0",
                state = "BE",
                ects = "6.0",
                sws = "3.0",
                tryCount = "1",
                semester = "SoSe 21",
                category = "Category 1"
            )
            4 -> Exam.create(
                examNumber = getDemoExamName("PL"),
                name = "Demo PL",
                kind = "PL",
                state = "AN",
                ects = "3.0",
                sws = "2.0",
                tryCount = "1",
                semester = "SoSe 21",
                resignation = "1",
                note = "GR",
                category = "Category 1"
            )
            5 -> Exam.create(
                examNumber = getDemoExamName("PL"),
                name = "Demo PL",
                kind = "PL",
                malus = "3.0",
                grade = "5.0",
                state = "NB",
                ects = "3.0",
                sws = "2.0",
                tryCount = "1",
                semester = "WiSe 21/22",
                category = "Category 1"
            )
            6 -> Exam.create(
                examNumber = getDemoExamName("PL"),
                name = "Demo PL",
                kind = "PL",
                grade = "1.0",
                state = "BE",
                ects = "3.0",
                sws = "2.0",
                tryCount = "2",
                semester = "SoSe 22",
                category = "Category 1"
            )
            7 -> Exam.create(
                examNumber = getDemoExamName("SL"),
                name = "Demo SL",
                kind = "SL",
                state = "BE",
                ects = "6.0",
                sws = "3.0",
                tryCount = "1",
                semester = "SoSe 21",
                category = "Category 1"
            )
            8 -> Exam.create(
                examNumber = getDemoExamName("SL"),
                name = "Demo SL",
                kind = "SL",
                state = "AN",
                ects = "3.0",
                sws = "2.0",
                tryCount = "1",
                semester = "SoSe 21",
                resignation = "1",
                note = "GR",
                category = "Category 1"
            )
            9 -> Exam.create(
                examNumber = getDemoExamName("SL"),
                name = "Demo SL",
                kind = "SL",
                malus = "3.0",
                state = "NB",
                ects = "3.0",
                sws = "2.0",
                tryCount = "1",
                semester = "WiSe 21/22",
                category = "Category 1"
            )
            10 -> Exam.create(
                examNumber = getDemoExamName("SL"),
                name = "Demo SL",
                kind = "SL",
                state = "BE",
                ects = "3.0",
                sws = "2.0",
                tryCount = "2",
                semester = "SoSe 22",
                category = "Category 1"
            )
            11 -> Exam.create(
                examNumber = getDemoExamName("P"),
                name = "Demo P",
                kind = "P",
                category = "Category 2"
            )
            12 -> Exam.create(
                examNumber = getDemoExamName("G"),
                name = "Demo G",
                kind = "G",
                bonus = "6.0",
                ects = "6.0",
                semester = "SoSe 21",
                grade = "1.0",
                state = "BE",
                category = "Category 2"
            )
            13 -> Exam.create(
                examNumber = getDemoExamName("G"),
                name = "Demo G",
                kind = "G",
                malus = "3.0",
                ects = "3.0",
                semester = "SoSe 21",
                grade = "5.0",
                state = "NB",
                category = "Category 2"
            )
            else -> getRandomExam()
        }
    }

    private fun getDemoExam(kind: String): Exam = when (kind) {
        "KO" -> Exam.create(
            examNumber = getDemoExamName("Account"),
            name = "Demo Account",
            kind = "KO",
            category = "Accounts",
        )
        "PL" -> Exam.create(
            examNumber = getDemoExamName("PL"),
            name = "Demo PL",
            kind = "PL",
            category = "Category 1"
        )
        "SL" -> Exam.create(
            examNumber = getDemoExamName("SL"),
            name = "Demo SL",
            kind = "SL",
            category = "Category 1"
        )
        "P" -> Exam.create(
            examNumber = getDemoExamName("P"),
            name = "Demo P",
            kind = "P",
            category = "Category 2"
        )
        "G" -> Exam.create(
            examNumber = getDemoExamName("G"),
            name = "Demo G",
            kind = "G",
            category = "Category 2"
        )
        else -> getDemoExam("PL")
    }

    private fun getRandomExam(kind: String = listOf("KO", "PL", "SL", "P", "G").random(), isBonus: Boolean = random.nextBoolean()): Exam =
        when (kind) {
            "KO" -> getDemoExam("KO").copy(
                examNumber = getDemoExamName("Account"),
                bonus = if (isBonus) listOf(Exam.UNDEFINED, "10.0", "20.0", "30.0", "40.0").random()
                else Exam.UNDEFINED,
                malus = if (isBonus) Exam.UNDEFINED
                else listOf("5.0", "9.0", "12.0", "15.0").random(),
            )
            "PL" -> getDemoExam("PL").copy(
                examNumber = getDemoExamName("PL"),
                malus = if (isBonus) Exam.UNDEFINED
                else listOf("2.0", "3.0", "6.0").random(),
                grade = if (isBonus) listOf("1.0", "1.3", "1.7", "2.0", "2.3", "2.7", "3.0", "3.3", "3.7", "4.0").random()
                else "5.0",
                state = if (isBonus) Exam.State.BE
                else Exam.State.NB,
                ects = listOf("2.0", "3.0", "6.0").random(),
                sws = listOf("1.0", "2.0", "3.0").random(),
                tryCount = listOf("1", "2", "3").random(),
                semester = listOf("SoSe 21", "WiSe 21/22", "SoSe 22").random(),
            )
            "SL" -> getDemoExam("SL").copy(
                examNumber = getDemoExamName("SL"),
                malus = if (isBonus) Exam.UNDEFINED
                else listOf("2.0", "3.0", "6.0").random(),
                state = if (isBonus) Exam.State.BE
                else Exam.State.NB,
                ects = listOf("2.0", "3.0", "6.0").random(),
                sws = listOf("1.0", "2.0", "3.0").random(),
                tryCount = listOf("1", "2", "3").random(),
                semester = listOf("SoSe 21", "WiSe 21/22", "SoSe 22").random(),
            )
            "P" -> getDemoExam("P").copy(examNumber = getDemoExamName("P"))
            "G" -> getDemoExam("G").copy(
                examNumber = getDemoExamName("G"),
                bonus = if (isBonus) listOf(Exam.UNDEFINED, "2.0", "3.0", "6.0", "12.0").random()
                else Exam.UNDEFINED,
                malus = if (isBonus) Exam.UNDEFINED
                else listOf("2.0", "3.0", "6.0", "12.0").random(),
                grade = if (isBonus) listOf("1.0", "1.3", "1.7", "2.0", "2.3", "2.7", "3.0", "3.3", "3.7", "4.0").random()
                else "5.0",
                state = if (isBonus) Exam.State.BE
                else Exam.State.NB,
                ects = listOf("2.0", "3.0", "6.0", "12.0").random(),
                semester = listOf("SoSe 21", "WiSe 21/22", "SoSe 22").random(),
            )
            else -> getRandomExam("PL")
        }
}
