package de.lemke.studiportal.domain

import de.lemke.studiportal.domain.model.Exam
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ParseStudiportalDataUseCase {
    operator fun invoke(response: String): List<Exam> {
        val examList = mutableListOf<Exam>()
        val examStart: Int = response.indexOf("<table cellspacing=\"0\" cellpadding=\"5\" border=\"0\" align=\"center\" width=\"100%\">")
        val examEnd: Int = response.indexOf("</table>", examStart)
        if (examStart == -1 || examEnd == -1) return examList
        val table = Jsoup.parse(response.substring(examStart, examEnd))
        val rows = table.getElementsByTag("tr")
        var currentCategory = "Unmatched"
        //Iterate over rows
        for ((index, row) in rows.withIndex()) {
            //If the rows contains a th element -> New category
            val thCount = row.getElementsByTag("th").size
            when {
                thCount > 1 -> continue //More than 1x th? Must be the real header row -> skip
                thCount > 0 -> {
                    val name = row.text()
                    if (name.isNullOrBlank()) examList.add(Exam.createSeparator(index, currentCategory)) //Blank name? -> Separator
                    else currentCategory = getCategoryString(row.text()) //Just one th -> create new category
                }
                else -> examList.add(createExam(row, currentCategory))
            }
        }
        return examList
    }

    private fun trimString(s: String): String {
        return s.replace(160.toChar().toString(), " ").trim()
    }

    private fun createExam(row: Element, category: String): Exam {
        val cols = row.getElementsByTag("td")
        val examNo = trimString(cols[0].text())
        var name: String? = null
        var bonus: String? = null
        var malus: String? = null
        var ects: String? = null
        var sws: String? = null
        var semester: String? = null
        var kind: String? = null
        var tryCount: String? = null
        var grade: String? = null
        var state: String? = null
        var comment: String? = null
        var resignation: String? = null
        var note: String? = null


        //Extract data
        var offset = 0
        for (i in cols.indices) {
            //Save current col
            val col = cols[i]

            //If the col contains colspan attribute, skip this and all spanned columns
            val colspan = col.attr("colspan")
            if (colspan.isNotEmpty()) {
                val colspanInt = col.attr("colspan").toInt()
                //Add the colspan to i (substract -1 because i++ on next iteration)
                offset += colspanInt - 1
                continue
            }

            //Get text and remove leading and trailing whitespaces and espacially the #160
            val text = trimString(col.text())
            when (i + offset) {
                1 -> name = text
                2 -> bonus = text
                3 -> malus = text
                4 -> ects = text
                5 -> sws = text
                6 -> semester = text
                7 -> kind = text
                8 -> tryCount = text
                9 -> grade = text
                10 -> state = text
                11 -> comment = text
                12 -> resignation = text
                13 -> note = text
            }
        }
        return Exam.create(
            examNumber = examNo,
            name = name,
            bonus = bonus,
            malus = malus,
            ects = ects,
            sws = sws,
            semester = semester,
            kind = kind,
            tryCount = tryCount,
            grade = grade,
            state = state,
            comment = comment,
            resignation = resignation,
            note = note,
            category = category
        )
    }

    private fun getCategoryString(category: String): String = category
        .replace(":", "")
        .replace("*", "")
        .replace("Module/Teilmodule", "Module")
        .replace("(ECTS) ", "")
        .replace("Bestandene Module", "Bestanden")
        .trim()

}
