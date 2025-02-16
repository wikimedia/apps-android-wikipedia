package org.wikipedia.homeworks

import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*

// Define color type
enum class Color {
    RED,
    BLUE,
    GREEN,
    YELLOW,
    BLACK,
    WHITE
}

// Define shape class
data class Shape(val sideLength: Float, val sides: Int, val color: Color)

// Matcher to check side length within a specified range
class SideLengthMatcher(private val min: Float, private val max: Float) : TypeSafeMatcher<Shape>() {
    override fun describeTo(description: Description) {
        description.appendText("side length in the range from $min to $max")
    }
    override fun matchesSafely(item: Shape): Boolean {
        return item.sideLength in min..max
    }
}
fun hasSideLengthInRange(min: Float, max: Float) = SideLengthMatcher(min, max)

// Matcher to check for angles of the shape
class AnglesMatcher : TypeSafeMatcher<Shape>() {
    override fun describeTo(description: Description) {
        description.appendText("should have valid angles")
    }

    override fun matchesSafely(item: Shape): Boolean {
        return item.sides >= 3 // A valid polygon must have at least 3 sides
    }
}
fun hasAngles() = AnglesMatcher()

// Matcher to check for even number of sides
class EvenSidesMatcher : TypeSafeMatcher<Shape>() {
    override fun describeTo(description: Description) {
        description.appendText("should have an even number of sides")
    }

    override fun matchesSafely(item: Shape): Boolean {
        return item.sides % 2 == 0
    }
}
fun hasEvenSides() = EvenSidesMatcher()

// Matcher to check color
class ColorMatcher(private val expectedColor: Color) : TypeSafeMatcher<Shape>() {
    override fun describeTo(description: Description) {
        description.appendText("shape should be $expectedColor color")
    }
    override fun matchesSafely(item: Shape): Boolean {
        return item.color == expectedColor
    }
}
fun hasColor(expectedColor: Color) = ColorMatcher(expectedColor)

// Matcher to check for non-negative side length
class NegativeSideLengthMatcher : TypeSafeMatcher<Shape>() {
    override fun describeTo(description: Description) {
        description.appendText("side length should not be negative")
    }
    override fun matchesSafely(item: Shape): Boolean {
        return item.sideLength >= 0
    }
}
fun hasValidSideLength() = NegativeSideLengthMatcher()

// Matcher to check for non-negative number of sides
class NegativeSidesMatcher : TypeSafeMatcher<Shape>() {
    override fun describeTo(description: Description) {
        description.appendText("number of sides should not be negative")
    }
    override fun matchesSafely(item: Shape): Boolean {
        return item.sides >= 0
    }
}
fun hasValidSides() = NegativeSidesMatcher()

val shapes = listOf(
    Shape(10f, 3, Color.RED), Shape(5f, 4, Color.BLUE), Shape(7f, 2, Color.GREEN),
    Shape(0.5f, 1, Color.YELLOW), Shape(-3f, 5, Color.BLACK), Shape(8f, -2, Color.WHITE),
    Shape(12f, 6, Color.RED), Shape(15f, 8, Color.BLUE), Shape(20f, 4, Color.GREEN),
    Shape(9f, 5, Color.YELLOW), Shape(2f, 3, Color.BLACK), Shape(11f, 7, Color.WHITE),
    Shape(6f, 10, Color.RED), Shape(3f, 2, Color.BLUE), Shape(4f, 1, Color.GREEN),
    Shape(25f, 12, Color.YELLOW), Shape(30f, 14, Color.BLACK), Shape(35f, 16, Color.WHITE),
    Shape(40f, 18, Color.RED), Shape(50f, 20, Color.BLUE)
)

// Filtering using allOf
val filteredShapes = shapes.filter { shape ->
    allOf(
        hasSideLengthInRange(1f, 20f),
        hasAngles(),
        hasEvenSides(),
        hasColor(Color.BLUE),
        hasValidSideLength(),
        hasValidSides()
    ).matches(shape)
}

// Assertion examples
fun testMatchers() {
    val shape = Shape(10f, 4, Color.RED)
    assertThat(shape, hasSideLengthInRange(1f, 20f))
    assertThat(shape, hasAngles())
    assertThat(shape, hasEvenSides())
    assertThat(shape, hasColor(Color.RED))
    assertThat(shape, hasValidSideLength())
    assertThat(shape, hasValidSides())
}

fun main() {
    testMatchers()
    println("Shapes that passed the filter: $filteredShapes")
}