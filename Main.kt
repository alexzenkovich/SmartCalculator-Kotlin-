package calculator

import java.math.BigInteger
import java.util.*
import kotlin.ArithmeticException
import kotlin.math.pow


fun main() {
    val sc = Scanner(System.`in`)
    val variables = mutableMapOf<String, BigInteger>()
    while (true) {
        val input = sc.nextLine()
        if (input.trim() == "" || input.split(" ").isEmpty()) {
            continue
        } else if (input.matches(Regex("[/][a-z]+"))) {
            val resultAction = handleInput(input)
            if (resultAction) return
        } else if (input.contains("=")) {
            val data = input.split("=")
            val variable = data[0].trim()
            if (!variable.matches(Regex("[a-zA-Z]+"))) {
                println("Invalid identifier")
                continue
            }
            val value = data[1].trim()
            if (!input.matches(Regex("\\s*\\w+\\s*[=]\\s*[-+]?\\s*\\w+\\s*"))
                || (!value.matches(Regex("[a-zA-Z]+")) && !value.matches(Regex("[-+]?[0-9]+")))
            ) {
                println("Invalid assignment")
                continue
            } else if (value.matches(Regex("[a-zA-Z]+"))) {
                if (!variables.contains(value)) {
                    println("Unknown variable")
                    continue
                }
                else {
                    val temp = variables[value]
                    if (temp != null) variables[variable] = temp
                    continue
                }
            }
            else if (value.matches(Regex("[-+]?[0-9]+"))) {
                variables[variable] = value.toBigInteger()
                continue
            }
        } else if (input.matches(Regex("\\s*[+-]?\\s*[a-zA-Z0-9]+\\s*"))) {
            val item = input.trim()
            val matchResult = Regex("\\s*[-+]?\\s*\\d+\\s*").find(item)
            if (matchResult != null) {
                println(matchResult.value)
            } else if (!item.matches(Regex("\\s*[a-zA-Z]+\\s*"))) {
                println("Invalid identifier")
                continue
            } else if (!variables.contains(item)) {
                println("Unknown variable")
                continue
            } else if (variables.contains(item)) {
                println(variables[item])
                continue
            }
        } else if (input.matches(Regex("\\s*[(]*\\s*[+-]?\\s*[a-zA-Z0-9]+\\s*[-+*/^]+\\s*[(]*\\s*[a-zA-Z0-9]+\\s*[)]*\\s*([-+*/^]+\\s*[(]*\\s*[a-zA-Z0-9]+\\s*[)]*\\s*)*"))) {
            val handled = handleExpression(input)
            val openedParenthesesSize = handled.filter { item -> item == "(" }.size
            val closedParenthesesSize = handled.filter { item -> item == ")" }.size
            if (handled.isEmpty() || openedParenthesesSize != closedParenthesesSize)
            {
                println("Invalid expression")
                continue
            }
            val postfix = toPostfix(handled)
            try {
                println(resolvePostfix(postfix, variables))
            } catch (e: ArithmeticException) {
                println("division by zero")
                continue
            }
        } else {
            println("Invalid expression")
            continue
        }
    }
}

fun handleExpression(input: String): List<String> {
    val result = mutableListOf<String>()
    var postfix = ""
    val items = input.split(Regex("\\s*")).toMutableList()
    items.removeIf { s -> s == "" }
    var i = 0
    while (i < items.lastIndex + 1) {
        val symbol = items[i]
        if (symbol.matches(Regex("\\d"))) {
            postfix += items[i]
            while (i != items.lastIndex && items[i + 1].matches(Regex("\\d"))) {
                postfix += items[i + 1]
                i++
            }
        } else if (symbol.matches(Regex("[a-zA-Z]"))) {
            postfix += items[i]
            while (i != items.lastIndex && items[i + 1].matches(Regex("[a-zA-Z]"))) {
                postfix += items[i + 1]
                i++
            }
        } else if (symbol.matches(Regex("[*/^()]"))) {
            if (i != items.lastIndex && items[i].matches(Regex("[*/]"))
                && items[i + 1].matches(Regex("[*/]"))) {
                return emptyList()
            }
            postfix += items[i]
        } else if (symbol.matches(Regex("[-+]"))) {
            postfix += items[i]
            while (i != items.lastIndex && items[i + 1].matches(Regex("[-+]"))) {
                postfix += items[i + 1]
                i++
            }
            postfix = checkPlusMinusSign(postfix)
        } else {
            continue
        }
        result.add(postfix)
        postfix = ""
        i++
    }
    return result
}

fun toPostfix(input: List<String>): List<String> {
    var postfix = mutableListOf<String>()
    val operators: Stack<String> = Stack()
    for (i in 0..input.lastIndex step 1) {
        val item = input[i]
        if (item.matches(Regex("[a-zA-Z0-9]+"))) postfix += item
        else if (item == "(") operators.push(item)
        else if (item == ")") {
            while (operators.peek() != "(") postfix += operators.pop()
            operators.pop()
        }
        else {
            while (!operators.empty() && operators.peek() != "(" && prec(item) <= prec(operators.peek())) {
                postfix += operators.pop()
            }
            operators.push(item)
        }
    }
    while (!operators.empty()) postfix += operators.pop()
    return postfix
}

fun resolvePostfix(postfix: List<String>, variables: Map<String, BigInteger>): BigInteger {
    val stack: Stack<BigInteger> = Stack()
    var choice: String
    var result = BigInteger.valueOf(0)
    var x: BigInteger
    var y: BigInteger
    for (i in postfix.indices step 1) {
        val item = postfix[i]
        if (item != "+" && item != "-" && item != "*" && item != "/" && item != "^") {
            stack.push(handleItem(item, variables))
            continue
        } else {
            choice = item
        }
        if (choice != "+" && choice != "-" && choice != "*" && choice != "/" && choice != "^") {
            continue
        } else if (choice == "+") {
            x = stack.pop()
            y = stack.pop()
            result = sum(x, y)
        } else if (choice == "-") {
            x = stack.pop()
            y = stack.pop()
            result = subtract(y, x)
        } else if (choice == "*") {
            x = stack.pop()
            y = stack.pop()
            result = multiple(x, y)
        } else if (choice == "/") {
            x = stack.pop()
            y = stack.pop()
            result = divide(y, x)
        } else if (choice == "^") {
            x = stack.pop()
            y = stack.pop()
            result = pow(x, y)
        }
        stack.push(result)
    }
    return result
}

fun handleItem(item: String, variables: Map<String, BigInteger>): BigInteger {
    var result = BigInteger.valueOf(0)
    if (item.matches(Regex("[a-zA-Z]+")) && variables.containsKey(item)) {
        if (variables[item] != null) result = variables[item]!!
    }
    else if (item.matches(Regex("[0-9]+"))) result = item.toBigInteger()
    return result
}

fun sum(a: BigInteger, b: BigInteger): BigInteger {
    return a.add(b)
}

fun subtract(a: BigInteger, b: BigInteger): BigInteger {
    return a.subtract(b)
}

fun multiple(a: BigInteger, b: BigInteger): BigInteger {
    return a.multiply(b)
}

fun divide(a: BigInteger, b: BigInteger): BigInteger {
    return a.divide(b)
}

fun pow(a: BigInteger, b: BigInteger): BigInteger {
    return a.toDouble().pow(b.toDouble()).toInt().toBigInteger()
}

fun checkPlusMinusSign(line: String): String {
    var result = ""
    if (line.matches(Regex("[+]{2,}"))) result = "+"
    else if (line.matches(Regex("[-]{2,}"))) {
        val length = line.trim().length
        result = if (length % 2 == 0) "+" else "-"
    } else result = line
    return result
}

fun handleInput(input: String): Boolean {
    if (input == "/exit") {
        print("Bye!")
        return true
    } else if (input == "/help") {
        println("The program calculates the sum of numbers")
    } else println("Unknown command")
    return false
}

fun prec(ch: String): Int {
    if (ch == "+" || ch == "-") return 1
    if (ch == "*" || ch == "/") return 2
    if (ch == "^") return 3
    return 0
}