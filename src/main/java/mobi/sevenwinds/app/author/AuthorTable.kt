package mobi.sevenwinds.app.author

import mobi.sevenwinds.app.budget.BudgetEntity
import mobi.sevenwinds.app.budget.BudgetEntity.Companion.referrersOn
import mobi.sevenwinds.app.budget.BudgetTable
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.joda.time.DateTime

object AuthorTable : IntIdTable("author") {
    val fullName = varchar("full_name", 255)
    val createdAt = datetime("created_at").default(DateTime.now())
}

class AuthorEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AuthorEntity>(AuthorTable)

    var fullName by AuthorTable.fullName
    var createdAt by AuthorTable.createdAt
//    val budgets by BudgetEntity optionalReferrersOn BudgetTable.authorId  // Uncomment if needed

    fun toResponse(): AuthorRs {
        return AuthorRs(id.value, fullName, createdAt)
    }
}
