package com.example.pixelscanner.benchmark;

import android.content.Intent;

import androidx.benchmark.macro.CompilationMode;
import androidx.benchmark.macro.FrameTimingMetric;
import androidx.benchmark.macro.MacrobenchmarkScope;
import androidx.benchmark.macro.StartupMode;
import androidx.benchmark.macro.junit4.MacrobenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@RunWith(AndroidJUnit4.class)
public class SessionIngestionBenchmark {
    @Rule
    public final MacrobenchmarkRule benchmarkRule = new MacrobenchmarkRule();

    @Test
    public void ingestBenchmarkSession() {
        benchmarkRule.measureRepeated(
                BenchmarkConfig.TARGET_PACKAGE_NAME,
                Collections.singletonList(new FrameTimingMetric()),
                CompilationMode.DEFAULT,
                StartupMode.COLD,
                BenchmarkConfig.DEFAULT_ITERATIONS,
                new Function1<MacrobenchmarkScope, Unit>() {
                    @Override
                    public Unit invoke(MacrobenchmarkScope scope) {
                        scope.pressHome();
                        scope.startActivityAndWait(createBenchmarkLaunchIntent());
                        return Unit.INSTANCE;
                    }
                },
                new Function1<MacrobenchmarkScope, Unit>() {
                    @Override
                    public Unit invoke(MacrobenchmarkScope scope) {
                        UiDevice device = UiDevice.getInstance(
                                InstrumentationRegistry.getInstrumentation()
                        );
                        device.wait(Until.hasObject(
                                By.res(BenchmarkConfig.TARGET_PACKAGE_NAME, "review_thumbnail_strip")
                        ), 5_000);
                        device.waitForIdle();
                        return Unit.INSTANCE;
                    }
                }
        );
    }

    private static Intent createBenchmarkLaunchIntent() {
        return new Intent(Intent.ACTION_MAIN)
                .setClassName(
                        BenchmarkConfig.TARGET_PACKAGE_NAME,
                        BenchmarkConfig.TARGET_ACTIVITY_NAME
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(BenchmarkConfig.EXTRA_START_IN_BENCHMARK_INGESTION_MODE, true);
    }
}
