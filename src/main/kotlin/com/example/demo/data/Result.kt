package com.example.demo.data

import com.example.demo.dsl.SQLDataType
import com.example.demo.dsl.SortableColumnImpl
import com.example.demo.dsl.Table

object RESULT: Table {
    override fun getName(): String = "Result"

    val ID = SortableColumnImpl("id", SQLDataType.VARCHAR, this)
    val SCORE = SortableColumnImpl("score", SQLDataType.INTEGER, this)
}