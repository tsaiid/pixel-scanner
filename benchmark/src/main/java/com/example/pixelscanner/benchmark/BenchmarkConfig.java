package com.example.pixelscanner.benchmark;

public final class BenchmarkConfig {
    public static final String TARGET_PACKAGE_NAME = "com.example.pixelscanner";
    public static final String TARGET_ACTIVITY_NAME = "com.example.pixelscanner.MainActivity";
    public static final String EXTRA_START_IN_BENCHMARK_REVIEW_MODE =
            "com.example.pixelscanner.extra.START_IN_BENCHMARK_REVIEW_MODE";
    public static final String EXTRA_START_IN_BENCHMARK_INGESTION_MODE =
            "com.example.pixelscanner.extra.START_IN_BENCHMARK_INGESTION_MODE";
    public static final int DEFAULT_ITERATIONS = 5;

    private BenchmarkConfig() {
    }
}
