package mobi.sevenwinds.app.author

import com.papsign.ktor.openapigen.annotations.type.string.length.Length
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import org.joda.time.DateTime

fun NormalOpenAPIRoute.author() {
    route("/author") {
        route("/add").post<Unit, AuthorRs, AuthorRq>(info("Добавить запись")) { param, body ->
            respond(AuthorService.createAuthor(body))
        }
    }
}

data class AuthorRq(
    @Length(1, max = 255) val fullName: String,
)

data class AuthorRs(
    val id: Int,
    val fullName: String,
    val createdAt: DateTime,
)