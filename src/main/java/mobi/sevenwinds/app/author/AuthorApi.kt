package mobi.sevenwinds.app.author

import org.joda.time.DateTime


data class AuthorRecord(
    val id: Int,
    val fullName: String,
    val createdAt: DateTime
)