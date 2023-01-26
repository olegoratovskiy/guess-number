package com.example.demo.dsl

import org.springframework.jdbc.core.JdbcTemplate

class UpdateBuilder(val table: Table) {
    private val updates = mutableListOf<Pair<Column, Any?>>()

    private lateinit var cond: Condition

    fun setCond(cond: Condition) {
        this.cond = cond
    }

    fun addUpdates(update: Pair<Column, Any?>) {
        updates.add(update)
    }

    fun getSQL(): String {
        return "update ${table.getName()} set " +
                "${updates.joinToString { "${it.first} = " +
                        if (it.second is String) "'${it.second}'" else it.second.toString()
                }} where $cond;"
    }

    fun execute(db: JdbcTemplate): Int {
        return db.update(getSQL())
    }
}

private fun checkColumnsInCondition(updateBuilder: UpdateBuilder, condition: Condition) {
    if (condition is Comparison) {
        if (condition.column.getTable() !== updateBuilder.table  ||
            condition.value is Column &&
            (condition.value as Column).getTable() !== updateBuilder.table
        ) {
            throw IllegalArgumentException("In WHERE clause only columns from FROM table are allowed")
        }
        return
    }
    if (condition is BinaryCondition) {
        checkColumnsInCondition(updateBuilder, condition.left)
        checkColumnsInCondition(updateBuilder, condition.right)
    }
}

class UpdateCommandResult(override val updateBuilder: UpdateBuilder): UpdateSetStep

class UpdateSetCommandResult(override val updateBuilder: UpdateBuilder): UpdateWhereStep, UpdateSetStep

class UpdateWhereCommandResult(override val updateBuilder: UpdateBuilder): UpdateResultStep

interface UpdateSetStep {
    val updateBuilder: UpdateBuilder

    fun set(column: Column, value: Any?): UpdateSetCommandResult {
        if (value != null && (!sqlKotlinTypesMapping.containsKey(column.getType()) ||
                    value::class != sqlKotlinTypesMapping[column.getType()])) {
            throw IllegalArgumentException("SET clause should contain column and value with equal types")
        }
        updateBuilder.addUpdates(Pair(column, value))
        return UpdateSetCommandResult(updateBuilder)
    }
}

interface UpdateWhereStep {
    val updateBuilder: UpdateBuilder

    fun where(cond: Condition): UpdateWhereCommandResult {
        checkColumnsInCondition(updateBuilder, cond)
        updateBuilder.setCond(cond)
        return UpdateWhereCommandResult(updateBuilder)
    }
}

interface UpdateResultStep {
    val updateBuilder: UpdateBuilder

    fun execute(db: JdbcTemplate): Int {
        return updateBuilder.execute(db)
    }

    fun getSQL(): String {
        return updateBuilder.getSQL()
    }
}

fun update(table: Table): UpdateSetStep {
    return UpdateCommandResult(UpdateBuilder(table))
}