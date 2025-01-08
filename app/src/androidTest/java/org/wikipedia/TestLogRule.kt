package org.wikipedia

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class TestLogRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } catch (t: Throwable) {
                    val locationErrorLog = t.stackTrace
                        .filter { it.className.contains("org.wikipedia") }
                        .take(3)
                        .joinToString("\n") {
                            "\u2551 at ${it.fileName}:${it.lineNumber} --> ${it.methodName}()"
                        }
                    val errorLog = buildString {
                        appendLine("════ TEST FAILURE ════")
                        append(locationErrorLog)
                        appendLine("Stack Trace: ")
                        append(t.localizedMessage)
                        appendLine("\n══════════════════════")
                    }
                    EspressoLogger.logError(errorLog)
                    throw t
                }
            }
        }
    }
}
