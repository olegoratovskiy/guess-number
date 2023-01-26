package com.example.demo.dsl

import org.springframework.jdbc.core.JdbcTemplate

class DeleteBuilder(val table: Table) {
    private lateinit var cond: Condition

    fun setCond(cond: Condition) {
        this.cond = cond
    }

    fun getSQL(): String {
        return "delete ${table.getName()} where $cond;"
    }

    fun execute(db: JdbcTemplate): Int {
        return db.update(getSQL())
    }
}

class DeleteCommandResult(override val deleteBuilder: DeleteBuilder): DeleteWhereStep

class DeleteWhereCommandResult(override val deleteBuilder: DeleteBuilder): DeleteResultStep

private fun checkColumnsInCondition(deleteBuilder: DeleteBuilder, condition: Condition) {
    if (condition is Comparison) {
        if (condition.column.getTable() !== deleteBuilder.table  ||
            condition.value is Column &&
            (condition.value as Column).getTable() !== deleteBuilder.table
        ) {
            throw IllegalArgumentException("In WHERE clause only columns from FROM table are allowed")
        }
        return
    }
    if (condition is BinaryCondition) {
        checkColumnsInCondition(deleteBuilder, condition.left)
        checkColumnsInCondition(deleteBuilder, condition.right)
    }
}

interface DeleteWhereStep {
    val deleteBuilder: DeleteBuilder

    fun where(cond: Condition): DeleteWhereCommandResult {
        checkColumnsInCondition(deleteBuilder, cond)
        deleteBuilder.setCond(cond)
        return DeleteWhereCommandResult(deleteBuilder)
    }
}

interface DeleteResultStep {
    val deleteBuilder: DeleteBuilder

    fun execute(db: JdbcTemplate): Int {
        return deleteBuilder.execute(db)
    }

    fun getSQL(): String {
        return deleteBuilder.getSQL()
    }
}

fun delete(table: Table): DeleteWhereStep {
    return DeleteCommandResult(DeleteBuilder(table))
}