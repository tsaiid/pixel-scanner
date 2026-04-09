package com.example.pixelscanner.benchmark;

import androidx.benchmark.macro.CompilationMode;
import androidx.benchmark.macro.MacrobenchmarkScope;
import androidx.benchmark.macro.StartupMode;
import androidx.benchmark.macro.StartupTimingMetric;
import androidx.benchmark.macro.junit4.MacrobenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@RunWith(AndroidJUnit4.class)
public class ColdStartBenchmark {
    @Rule
    public final MacrobenchmarkRule benchmarkRule = new MacrobenchmarkRule();

    @Test
    public void coldStart() {
        benchmarkRule.measureRepeated(
                BenchmarkConfig.TARGET_PACKAGE_NAME,
                java.util.Collections.singletonList(new StartupTimingMetric()),
                CompilationMode.DEFAULT,
                StartupMode.COLD,
                BenchmarkConfig.DEFAULT_ITERATIONS,
                new Function1<MacrobenchmarkScope, Unit>() {
                    @Override
                    public Unit invoke(MacrobenchmarkScope scope) {
                        scope.pressHome();
                        return Unit.INSTANCE;
                    }
                },
                new Function1<MacrobenchmarkScope, Unit>() {
                    @Override
                    public Unit invoke(MacrobenchmarkScope scope) {
                        scope.startActivityAndWait();
                        return Unit.INSTANCE;
                    }
                }
        );
    }
}
