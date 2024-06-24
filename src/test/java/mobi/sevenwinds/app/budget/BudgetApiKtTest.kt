package mobi.sevenwinds.app.budget

import io.restassured.RestAssured
import mobi.sevenwinds.app.author.AuthorRq
import mobi.sevenwinds.app.author.AuthorRs
import mobi.sevenwinds.app.author.AuthorTable
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BudgetApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction {
            BudgetTable.deleteAll()
            AuthorTable.deleteAll()
        }
    }

    @Test
    fun testBudgetPagination() {
        addRecord(BudgetRq(2020, 5, 10, BudgetType.Приход))
        addRecord(BudgetRq(2020, 5, 5, BudgetType.Приход))
        addRecord(BudgetRq(2020, 5, 20, BudgetType.Приход))
        addRecord(BudgetRq(2020, 5, 30, BudgetType.Приход))
        addRecord(BudgetRq(2020, 5, 40, BudgetType.Приход))
        addRecord(BudgetRq(2030, 1, 1, BudgetType.Расход))

        RestAssured.given()
            .queryParam("limit", 3)
            .queryParam("offset", 1)
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                Assert.assertEquals(5, response.total)
                Assert.assertEquals(3, response.items.size)
                Assert.assertEquals(105, response.totalByType[BudgetType.Приход.name])
            }
    }

    @Test
    fun testStatsSortOrder() {
        addRecord(BudgetRq(2020, 5, 100, BudgetType.Приход))
        addRecord(BudgetRq(2020, 1, 5, BudgetType.Приход))
        addRecord(BudgetRq(2020, 5, 50, BudgetType.Приход))
        addRecord(BudgetRq(2020, 1, 30, BudgetType.Приход))
        addRecord(BudgetRq(2020, 5, 400, BudgetType.Приход))

        // expected sort order - month ascending, amount descending

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assert.assertEquals(30, response.items[0].amount)
                Assert.assertEquals(5, response.items[1].amount)
                Assert.assertEquals(400, response.items[2].amount)
                Assert.assertEquals(100, response.items[3].amount)
                Assert.assertEquals(50, response.items[4].amount)
            }
    }

    @Test
    fun testInvalidMonthValues() {
        RestAssured.given()
            .jsonBody(BudgetRs(2020, -5, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)

        RestAssured.given()
            .jsonBody(BudgetRs(2020, 15, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)
    }

    @Test
    fun testBudgetPaginationWithAuthor() {
        val author = createAuthor("John Doe")
        addRecord(BudgetRq(2020, 5, 10, BudgetType.Приход, author.id))
        addRecord(BudgetRq(2020, 5, 5, BudgetType.Приход))
        addRecord(BudgetRq(2020, 5, 20, BudgetType.Приход))
        addRecord(BudgetRq(2020, 5, 30, BudgetType.Приход))
        addRecord(BudgetRq(2020, 5, 40, BudgetType.Приход))
        addRecord(BudgetRq(2030, 1, 1, BudgetType.Расход))

        RestAssured.given()
            .queryParam("limit", 3)
            .queryParam("offset", 1)
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                Assert.assertEquals(5, response.total)
                Assert.assertEquals(3, response.items.size)
                Assert.assertEquals(105, response.totalByType[BudgetType.Приход.name])

                Assert.assertEquals(1, response.items.filter { it.author != null }.size)

                val responseAuthor = response.items.first { it.author != null }.author!!
                Assert.assertEquals("John Doe", author.fullName)

            }
    }

    @Test
    fun testStatsSortOrderWithAuthor() {
        val author1 = createAuthor("John Doe")
        val author2 = createAuthor("Jane Doe")
        addRecord(BudgetRq(2020, 5, 100, BudgetType.Приход, author1.id))
        addRecord(BudgetRq(2020, 1, 5, BudgetType.Приход, author2.id))
        addRecord(BudgetRq(2020, 5, 50, BudgetType.Приход))
        addRecord(BudgetRq(2020, 1, 30, BudgetType.Приход, author1.id))
        addRecord(BudgetRq(2020, 5, 400, BudgetType.Приход, author2.id))

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                Assert.assertEquals(30, response.items[0].amount)
                Assert.assertEquals("John Doe", response.items[0].author?.fullName)
                Assert.assertEquals(5, response.items[1].amount)
                Assert.assertEquals("Jane Doe", response.items[1].author?.fullName)
                Assert.assertEquals(400, response.items[2].amount)
                Assert.assertEquals("Jane Doe", response.items[2].author?.fullName)
                Assert.assertEquals(100, response.items[3].amount)
                Assert.assertEquals("John Doe", response.items[3].author?.fullName)
                Assert.assertEquals(50, response.items[4].amount)
                Assert.assertNull(response.items[4].author)
            }
    }

    private fun createAuthor(fullName: String): AuthorRs {
        return RestAssured.given()
            .jsonBody(AuthorRq(fullName))
            .post("/author/add")
            .toResponse()
    }

    private fun addRecord(record: BudgetRq) {
        RestAssured.given()
            .jsonBody(record)
            .post("/budget/add")
            .toResponse<BudgetRs>().let { response ->
                Assert.assertEquals(record.year, response.year)
                Assert.assertEquals(record.month, response.month)
                Assert.assertEquals(record.amount, response.amount)
                Assert.assertEquals(record.type, response.type)
                if (record.authorId != null) {
                    Assert.assertNotNull(response.author)
                    Assert.assertEquals(record.authorId, response.author?.id)
                } else {
                    Assert.assertNull(response.author)
                }
            }
    }
}