package mobi.sevenwinds.app.author

import io.restassured.RestAssured
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthorApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction { AuthorTable.deleteAll() }
    }

    @Test
    fun testCreateAuthor() {
        val request = AuthorRq("John Doe")

        val response = RestAssured.given()
            .jsonBody(request)
            .post("/author/add")
            .toResponse<AuthorRs>()

        Assert.assertEquals(request.fullName, response.fullName)
        Assert.assertNotNull(response.id)
        Assert.assertNotNull(response.createdAt)
    }

    @Test
    fun testCreateAuthorWithInvalidName() {
        val request = AuthorRq("")

        RestAssured.given()
            .jsonBody(request)
            .post("/author/add")
            .then()
            .statusCode(400)
    }

    @Test
    fun testCreateAuthorWithLongName() {
        val request = AuthorRq("A".repeat(256))

        RestAssured.given()
            .jsonBody(request)
            .post("/author/add")
            .then()
            .statusCode(400)
    }
}