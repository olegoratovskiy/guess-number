package com.example.demo.dsl

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper

class SelectBuilder(val columns: List<Column>) {
    var areColumnsChecked = false
    lateinit var table: Table
    private var whereCondition: Condition? = null
    var tableToJoin: Table? = null
    private var joinCondition: Eq? = null
    private val orderByColumns = mutableListOf<Pair<Column, SortingOrder>>()

    fun setFrom(table: Table) {
        this.table = table
    }

    fun setWhere(cond: Condition) {
        whereCondition = cond
    }

    fun setJoin(table: Table) {
        tableToJoin = table
    }

    fun setOn(cond: Eq) {
        joinCondition = cond
    }

    fun setOrderBy(columnsToSortBy: List<Column>) {
        columnsToSortBy.forEach {
            orderByColumns.add(Pair(it, if (it is SortedColumn) it.getSortingOrder() else SortingOrder.DEFAULT))
        }
    }

    fun getSQL(): String {
        val columnsToSelectPart =
            if (columns.isEmpty()) "*" else columns.joinToString()
        val joinPart =
            if (tableToJoin != null && joinCondition != null) "join ${tableToJoin?.getName()} on $joinCondition" else ""
        val wherePart = if (whereCondition != null) "where $whereCondition" else ""
        val orderByPart = if (orderByColumns.isNotEmpty()) "order by ${
            orderByColumns.joinToString {
                it.first.getName() + " " + it.second
            }
        }" else ""
        return "select $columnsToSelectPart from ${table.getName()} $joinPart $wherePart $orderByPart;"
    }

    fun <T> execute(db: JdbcTemplate, rowMapper: RowMapper<T>): List<T> {
        return db.query(getSQL(), rowMapper)
    }
}

private fun checkCorrectSelectColumns(selectBuilder: SelectBuilder) {
    if (selectBuilder.areColumnsChecked) {
        return
    }
    val tablesWhereColumns = selectBuilder.columns.map { it.getTable() }.distinct().toList()
    if (!setOf(selectBuilder.table, selectBuilder.tableToJoin).containsAll(tablesWhereColumns)) {
        throw IllegalArgumentException("SELECT clause is allowed to contain columns only from FROM table and JOIN table")
    }
    selectBuilder.areColumnsChecked = true
}

private fun checkColumnsInCondition(selectBuilder: SelectBuilder, condition: Condition) {
    if (condition is Comparison) {
        if (condition.column.getTable() !== selectBuilder.table &&
            condition.column.getTable() !== selectBuilder.tableToJoin ||
            condition.value is Column &&
            (condition.value as Column).getTable() !== selectBuilder.table &&
            (condition.value as Column).getTable() !== selectBuilder.tableToJoin
        ) {
            throw IllegalArgumentException("In WHERE clause only columns from FROM table and JOIN table are allowed")
        }
        return
    }
    if (condition is BinaryCondition) {
        checkColumnsInCondition(selectBuilder, condition.left)
        checkColumnsInCondition(selectBuilder, condition.right)
    }
}

class SelectCommandResult(override val selectBuilder: SelectBuilder) : SelectFromStep

class SelectFromCommandResult(override val selectBuilder: SelectBuilder) : SelectJoinStep, SelectWhereStep,
    SelectOrderByStep,
    SelectResultStep

class SelectJoinCommandResult(override val selectBuilder: SelectBuilder) : SelectOnStep

class SelectOnCommandResult(override val selectBuilder: SelectBuilder) : SelectWhereStep, SelectOrderByStep,
    SelectResultStep

class SelectWhereCommandResult(override val selectBuilder: SelectBuilder) : SelectOrderByStep, SelectResultStep

class SelectOrderByCommandResult(override val selectBuilder: SelectBuilder) : SelectResultStep

interface SelectFromStep {
    val selectBuilder: SelectBuilder

    fun from(table: Table): SelectFromCommandResult {
        selectBuilder.setFrom(table)
        return SelectFromCommandResult(selectBuilder)
    }
}

interface SelectJoinStep {
    val selectBuilder: SelectBuilder

    fun join(table: Table): SelectJoinCommandResult {
        if (table === selectBuilder.table) {
            throw IllegalArgumentException("Join is possible only with another table")
        }
        selectBuilder.setJoin(table)
        checkCorrectSelectColumns(selectBuilder)
        return SelectJoinCommandResult(selectBuilder)
    }
}

interface SelectOnStep {
    val selectBuilder: SelectBuilder

    fun on(cond: Eq): SelectOnCommandResult {
        if (cond.value !is Column ||
            !(cond.column.getTable() == selectBuilder.table && cond.value.getTable() == selectBuilder.tableToJoin ||
                    cond.column.getTable() == selectBuilder.tableToJoin && cond.value.getTable() == selectBuilder.table)
        ) {
            throw IllegalArgumentException("In ON statement you should compare columns from FROM table and JOIN table")
        }
        if (cond.value.getType() != cond.column.getType()) {
            throw IllegalArgumentException("In ON statement you should compare columns of the same type")
        }
        selectBuilder.setOn(cond)
        return SelectOnCommandResult(selectBuilder)
    }
}

interface SelectWhereStep {
    val selectBuilder: SelectBuilder

    fun where(cond: Condition): SelectWhereCommandResult {
        checkCorrectSelectColumns(selectBuilder)
        checkColumnsInCondition(selectBuilder, cond)
        selectBuilder.setWhere(cond)
        return SelectWhereCommandResult(selectBuilder)
    }
}

interface SelectOrderByStep {
    val selectBuilder: SelectBuilder

    fun orderBy(vararg columns: Column): SelectOrderByCommandResult {
        checkCorrectSelectColumns(selectBuilder)
        if (!setOf(selectBuilder.tableToJoin, selectBuilder.table).containsAll(
                columns.map { it.getTable() }.distinct().toList()
            )
        ) {
            throw IllegalArgumentException("In ORDER BY clause only columns from FROM table and JOIN table are allowed")
        }
        selectBuilder.setOrderBy(columns.toList())
        return SelectOrderByCommandResult(selectBuilder)
    }
}

interface SelectResultStep {
    val selectBuilder: SelectBuilder

    fun <T> fetch(db: JdbcTemplate, rowMapper: RowMapper<T>) : List<T> {
        checkCorrectSelectColumns(selectBuilder)
        return selectBuilder.execute(db, rowMapper)
    }

    fun getSQL(): String {
        checkCorrectSelectColumns(selectBuilder)
        return selectBuilder.getSQL()
    }
}

fun select(vararg columns: Column): SelectFromStep {
    return SelectCommandResult(SelectBuilder(columns.toList()))
}
