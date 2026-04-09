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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@RunWith(AndroidJUnit4.class)
public class ReviewPageSwitchBenchmark {
    @Rule
    public final MacrobenchmarkRule benchmarkRule = new MacrobenchmarkRule();

    @Test
    public void switchReviewPages() {
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
                        clickAndWait(device, "review_thumbnail_1");
                        clickAndWait(device, "review_thumbnail_0");
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
                .putExtra(BenchmarkConfig.EXTRA_START_IN_BENCHMARK_REVIEW_MODE, true);
    }

    private static void clickAndWait(UiDevice device, String resourceName) {
        device.findObject(By.res(BenchmarkConfig.TARGET_PACKAGE_NAME, resourceName)).click();
        device.waitForIdle();
    }
}
