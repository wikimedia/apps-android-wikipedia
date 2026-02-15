package org.wikipedia.lesson04.homeworks

import kotlin.collections.mutableListOf
import kotlin.random.Random

class Inventory() {
    private val items = mutableListOf<String>()

    operator fun plus(item: String) {
        items.add(item)
    }

    operator fun get(index: Int): String {
        return items[index]
    }

    operator fun contains(str: String): Boolean {
        return str in items
    }
}


class Toggle(private val enabled: Boolean) {
    operator fun not(): Toggle {
        return Toggle(!enabled)
    }
}

class Price(private val amount: Int) {

    operator fun times(num: Int): Int {
        return num * amount
    }
}

class Step(val number: Int) {

    operator fun rangeTo(s: Step): IntRange {
        return number..s.number
    }
}

operator fun IntRange.contains(s: Step): Boolean {
    return s.number in this
}

class Log() {

    private val entries = mutableListOf<String>()

    operator fun plus(entry: String): Log {
        entries.add(entry)
        return this
    }
}

class Person(private val name: String) {

    private val phrases = mutableListOf<String>()

    fun print() {
        println(phrases.joinToString(" "))
    }

    private fun selectPhrase(first: String, second: String): String {
        val random = Random.nextInt(0, 2)
        return if (random == 0) first else second
    }

    infix fun says(str: String): Person {
        phrases.add(str)
        return this
    }

    infix fun and(str: String): Person {
        check(phrases.isNotEmpty()) { "Сначала используй says" }
        phrases.add(str)
        return this
    }

    infix fun or(str: String): Person {
        check(phrases.isNotEmpty()) { "Сначала используй says" }
        phrases[phrases.lastIndex] = selectPhrase(phrases[phrases.lastIndex], str)
        return this
    }
}

fun main() {
    val andrew = Person("Andrew")
    andrew says "Hello" and "brothers." or "sisters." and "I believe" and "you" and "can do it" or "can't"
    andrew.print()
}