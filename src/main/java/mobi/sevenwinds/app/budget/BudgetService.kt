package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorEntity
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRq): BudgetRs = withContext(Dispatchers.IO) {
        transaction {
            val author = body.authorId?.let { AuthorEntity.findById(it) }

            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.author = author
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val query = BudgetTable
                .select { BudgetTable.year eq param.year }

            val total = query.count()

            val allData = BudgetEntity.wrapRows(query)
                .orderBy(BudgetTable.month to SortOrder.ASC, BudgetTable.amount to SortOrder.DESC)
                .map { it.toResponse() }

            val sumByType = allData
                .groupBy { it.type.name }
                .mapValues { type ->
                    type.value.sumOf { it.amount }
                }

            val paginatedData = allData
                .drop(param.offset)
                .take(param.limit)

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = paginatedData
            )
        }
    }
}