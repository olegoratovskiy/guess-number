package com.example.demo.dsl

import kotlin.reflect.KClass

interface Table {
    fun getName(): String
}

interface Column {
    fun getName(): String
    fun getTable(): Table
    fun getType(): SQLDataType
}

interface SortableColumn : Column {
    fun asc(): SortedColumn
    fun desc(): SortedColumn
}

interface SortedColumn : Column {
    fun getSortingOrder(): SortingOrder
}

enum class SQLDataType {
    VARCHAR, INTEGER
}

enum class SortingOrder(private val value: String) {
    ASC("asc"), DESC("desc"), DEFAULT("");

    override fun toString(): String {
        return value
    }
}

abstract class AbstractColumn(
    private val name: String,
    private val type: SQLDataType,
    private val table: Table,
) : Column {
    override fun getName(): String = name

    override fun getTable(): Table = table

    override fun getType(): SQLDataType = type

    override fun toString(): String {
        return getName()
    }
}

class SortableColumnImpl(
    private val name: String,
    private val type: SQLDataType,
    private val table: Table,
) : AbstractColumn(name, type, table), SortableColumn {

    override fun asc(): SortedColumn = SortedColumnImpl(name, type, table, SortingOrder.ASC)

    override fun desc(): SortedColumn = SortedColumnImpl(name, type, table, SortingOrder.DESC)
}

class SortedColumnImpl(
    private val name: String,
    private val type: SQLDataType,
    private val table: Table,
    private val sortingOrder: SortingOrder
) : AbstractColumn(name, type, table), SortedColumn {

    override fun getSortingOrder(): SortingOrder = sortingOrder
}

val sqlKotlinTypesMapping: Map<SQLDataType, KClass<out Any>> =
    mapOf(SQLDataType.VARCHAR to String::class, SQLDataType.INTEGER to Int::class)