package org.wikipedia.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures cold startup time with and without the Baseline Profile, to verify that it helps.
 * Run on a physical device for meaningful numbers:
 *
 *   ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest \
 *       -P android.testInstrumentationRunnerArguments.class=org.wikipedia.baselineprofile.StartupBenchmarks
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Before
    fun setUp() {
        skipOnboarding(targetPackageName)
    }

    @Test
    fun startupWithoutPrecompilation() = benchmark(CompilationMode.None())

    @Test
    fun startupWithBaselineProfile() = benchmark(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun benchmark(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = targetPackageName,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.COLD,
        iterations = 10,
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
        waitForHomeFeed()
    }
}
