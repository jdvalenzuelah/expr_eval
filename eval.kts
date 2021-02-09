#!/usr/bin/env kscript

import java.util.ArrayDeque
import kotlin.math.pow

sealed class ExpressionElement {
    data class Number(val value: Double): ExpressionElement() {

        override fun toString(): String = this.value.toString()

        companion object {
            fun of(value: String) = when(value) {
                "." -> Number(.0)
                else -> value.toDoubleOrNull()?.let { Number(it) }
            }
        }

    }

    sealed class Operator(val id: Char, val precedence: Int): ExpressionElement() {
        object Plus: Operator('+', 1)
        object Minus: Operator('-', 1)
        object Times: Operator('*', 2)
        object Div: Operator('/', 2)
        object Pow: Operator('^', 3)

        override fun toString(): String = this.id.toString()

        companion object {
            fun of(value: Char): ExpressionElement? = when(value) {
                Plus.id -> Plus
                Minus.id -> Minus
                Times.id -> Times
                Div.id -> Div
                Pow.id -> Pow
                else -> null
            }
        }

    }

    sealed class Grouping(val id: Char): ExpressionElement() {
        object OpenParenthesis: Grouping('(')
        object ClosingParenthesis: Grouping(')')

        override fun toString(): String = this.id.toString()

        companion object {
            fun of(value: Char): ExpressionElement? = when(value) {
                OpenParenthesis.id -> OpenParenthesis
                ClosingParenthesis.id -> ClosingParenthesis
                else -> null
            }
        }

    }

    companion object {
        fun of(value: Char) = of(value.toString())
        fun of(value: String): ExpressionElement? {
            return when {
                value.isEmpty() -> null
                value.length > 1 ->  Number.of(value)
                else -> {
                    val num = Number.of(value)
                    if(num != null)
                        return num
                    val op = Operator.of(value[0])
                    if(op != null)
                        return op
                    return Grouping.of(value[0]) ?: error("Unrecognized input $value")
                }
            }
        }
    }

    fun precedence(): Int = if(this is Operator) this.precedence else Int.MIN_VALUE

}

typealias Expression = List<ExpressionElement>

fun String.toExpression(): Expression {
    var expression = this.replace(" ", "")

    val res = mutableListOf<ExpressionElement>()
    while(expression.isNotEmpty()) {
        val number = expression.takeWhile { ExpressionElement.of(it) is ExpressionElement.Number }
        if(number.isNotEmpty()) {
            res.add(ExpressionElement.of(number)!!)
            expression = expression.removePrefix(number)
        } else {
            val element = ExpressionElement.of(expression[0]) ?: error("Unrecognized character ${expression[0]}")
            res.add(element)
            expression = expression.substring(1)
        }
    }

    return res
}

fun Expression.infixToPostfix(): Expression {
    val stack = ArrayDeque<ExpressionElement>()
    val postfix = mutableListOf<ExpressionElement>()

    forEach { el ->
        when(el) {
            is ExpressionElement.Number -> postfix.add(el)
            is ExpressionElement.Grouping.OpenParenthesis -> stack.push(el)
            is ExpressionElement.Grouping.ClosingParenthesis -> {
                while(stack.isNotEmpty() && stack.peek() != ExpressionElement.Grouping.OpenParenthesis) {
                    postfix.add(stack.pop())
                }
                stack.pop()
            }
            else -> {
                while(stack.isNotEmpty() && el.precedence() <= stack.peek().precedence()) {
                    postfix.add(stack.pop())
                }
                stack.push(el)
            }
        }
    }

    while(stack.isNotEmpty()) {
        postfix.add(stack.pop())
    }

    return postfix
}

fun Expression.evalPostfix(): Double {
    val stack = ArrayDeque<ExpressionElement.Number>()

    forEach { el ->
        if (el is ExpressionElement.Number) {
            stack.push(el)
        } else {
            val (v2, v1) = stack.pop().value to stack.pop().value
            when (el) {
                ExpressionElement.Operator.Pow ->  stack.push(ExpressionElement.Number(v1.pow(v2)))
                ExpressionElement.Operator.Times -> stack.push(ExpressionElement.Number(v1 * v2))
                ExpressionElement.Operator.Div -> stack.push(ExpressionElement.Number(v1 / v2))
                ExpressionElement.Operator.Plus -> stack.push(ExpressionElement.Number(v1 + v2))
                ExpressionElement.Operator.Minus ->  stack.push(ExpressionElement.Number(v1 - v2))
                else -> error("Invalid postfix expression $this")
            }
        }
    }
    return stack.peek().value
}

fun eval(expression: String): Double = expression
    .toExpression()
    .infixToPostfix()
    .evalPostfix()

fun evalAndPrint(expr: String) = try {
    println("$expr = ${eval(expr)}")
} catch (e: Exception) {
    println("Error with expression $expr -> ${e.message}")
}

fun main() {
    listOf(
        "1/3",
        "(2+3)*(4+ 5.0)",
        "3^2",
        "(2*2)^(2*2)",
        "30/(3*2)",
        "5.25-4.50",
        "(2+3)*(4+ 5.0"
    ).forEach(::evalAndPrint)

    println("\nEnter an expression: ")
    val expr = readLine()

    if(expr.isNullOrEmpty())
        println("Invalid expression.")
    else
        evalAndPrint(expr)

}

main()
