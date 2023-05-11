package de.lemke.studiportal.domain

import org.jsoup.Jsoup

class ParseStudentDataUseCase {
    operator fun invoke(response: String): Pair<String, String>? {
        val studentDataStart: Int =
            response.indexOf("<table align=\"left\" style=\"border-width:1px; border-color:#007f4c; border-style:solid; padding:5px;\"  cellspacing=\"1\" cellpadding=\"8\" bgcolor=\"#FAFAFA\">")
        val studentDataEnd: Int = response.indexOf("</table>", studentDataStart)
        if (studentDataStart == -1 || studentDataEnd == -1) return null
        val rows = Jsoup.parse(response.substring(studentDataStart, studentDataEnd)).getElementsByTag("tr")
        val name = rows.getOrNull(0)?.getElementsByTag("th")?.getOrNull(0)?.text() ?: return null
        val data = rows.getOrNull(1)?.getElementsByTag("th")?.getOrNull(0)?.text() ?: return null
        val matrikelNummer = data.split(" ").first()
        return Pair(
            name + if (matrikelNummer.isBlank()) "" else " ($matrikelNummer)",
            data.split(" ").drop(1).joinToString(" ")
        )
    }
}