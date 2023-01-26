package com.example.demo.data

import com.example.demo.dsl.SQLDataType
import com.example.demo.dsl.SortableColumnImpl
import com.example.demo.dsl.Table

object GUESSED_NUMBER: Table {
    override fun getName(): String = "GuessedNumber"

    val ID = SortableColumnImpl("id", SQLDataType.VARCHAR, this)
    val SECRET_NUMBER = SortableColumnImpl("secret_number", SQLDataType.INTEGER, this)
}