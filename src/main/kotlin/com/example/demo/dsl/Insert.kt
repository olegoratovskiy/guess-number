package com.example.demo.dsl

import org.springframework.jdbc.core.JdbcTemplate

class InsertBuilder(private val table: Table, val columns: List<Column>) {
    private val rows = mutableListOf<List<Any?>>()

    fun addRow(row: List<Any?>) {
        rows.add(row)
    }

    fun getSQL(): String {
        return "insert into ${table.getName()} (${columns.joinToString()}) " +
                "values ${rows.joinToString { row -> "(${row.joinToString { if (it is String) "'$it'" else it.toString() }})" }};"
    }

    fun execute(db: JdbcTemplate): Int {
        return db.update(getSQL())
    }

}

class InsertIntoCommandResult(override val insertBuilder: InsertBuilder) : InsertValuesStep

class ValuesCommandResult(override val insertBuilder: InsertBuilder) : InsertValuesStep, InsertResultStep

interface InsertValuesStep {
    val insertBuilder: InsertBuilder

    fun values(vararg values: Any?): ValuesCommandResult {
        values.forEachIndexed { ind, it ->
            if (!sqlKotlinTypesMapping.containsKey(insertBuilder.columns[ind].getType()) ||
                it != null && it::class != sqlKotlinTypesMapping[insertBuilder.columns[ind].getType()]
            )
                throw IllegalArgumentException("Value types in VALUES clause should match column types")
        }
        insertBuilder.addRow(values.toList())
        return ValuesCommandResult(insertBuilder)
    }
}

interface InsertResultStep {
    val insertBuilder: InsertBuilder

    fun execute(db: JdbcTemplate): Int {
        return insertBuilder.execute(db)
    }

    fun getSQL(): String {
        return insertBuilder.getSQL()
    }
}

fun insertInto(table: Table, vararg columns: Column): InsertValuesStep {
    columns.forEach {
        if (it.getTable() !== table)
            throw IllegalArgumentException("INSERT INTO clause should contain columns from the table")
    }
    if (columns.distinct().toList().size != columns.size) {
        throw IllegalArgumentException("INSERT INTO clause should contain distinct columns")
    }
    return InsertIntoCommandResult(InsertBuilder(table, columns.toList()))
}