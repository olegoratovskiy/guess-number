package com.example.demo.dsl

interface Condition

interface Comparison: Condition {
    val column: Column
    val value: Any?
}

interface BinaryCondition: Condition {
    val left: Condition
    val right: Condition
}

class Eq(override val column: Column, override val value: Any?) : Comparison {

    override fun toString(): String {
        return when (value) {
            null -> "$column is null"
            is String -> "$column = '$value'"
            else -> "$column = $value"
        }
    }
}



class And(override val left: Condition, override val right: Condition) : BinaryCondition {

    override fun toString(): String {
        return "($left and $right)"
    }

}

class Or(override val left: Condition, override val right: Condition) : BinaryCondition {

    override fun toString(): String {
        return "($left or $right)"
    }

}

fun Column.eq(value: Any?): Condition {
    if (value !is Column &&
        (!sqlKotlinTypesMapping.containsKey(this.getType()) ||
                value != null && value::class != sqlKotlinTypesMapping[this.getType()])) {
        throw IllegalArgumentException("Incorrect types to compare")
    }
    if (value is Column && this.getType() != value.getType()) {
        throw IllegalArgumentException("Compared columns should have equal types")
    }
    return Eq(this, value)
}

fun Condition.and(cond: Condition): Condition {
    return And(this, cond)
}

fun Condition.or(cond: Condition): Condition {
    return Or(this, cond)
}
