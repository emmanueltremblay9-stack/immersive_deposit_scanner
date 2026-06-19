package com.oblixorprime.immersivedepositscanner.integration.ie;

import java.util.OptionalDouble;
import java.util.OptionalLong;

public final class IECoreSampleReaderQa {
    private IECoreSampleReaderQa() {
    }

    public static void run() {
        assertOptionalLong(900L, IECoreSampleReader.currentYield(100, 1000), "IE current yield must subtract depletion");
        assertOptionalLong(1000L, IECoreSampleReader.maximumYield(1000), "IE maximum yield must be exposed");
        assertOptionalDouble(0.9D, IECoreSampleReader.remainingPercentage(100, 1000), "IE remaining percentage must use remaining yield");
        assertTrue(!IECoreSampleReader.isDepleted(100, 1000, 0.0D), "partial IE depletion must not be depleted");

        assertOptionalLong(0L, IECoreSampleReader.currentYield(1200, 1000), "IE over-depletion must clamp remaining yield to zero");
        assertOptionalDouble(0.0D, IECoreSampleReader.remainingPercentage(1200, 1000), "IE over-depletion must clamp remaining percentage to zero");
        assertTrue(IECoreSampleReader.isDepleted(1200, 1000, 0.75D), "IE depletion exhaustion must mark depleted even with saturation");

        assertTrue(IECoreSampleReader.currentYield(-1, 1000).isEmpty(), "unknown IE depletion must not report a current yield");
        assertTrue(IECoreSampleReader.maximumYield(0).isEmpty(), "invalid IE maximum yield must not be reported");
        assertTrue(IECoreSampleReader.remainingPercentage(-1, 1000).isEmpty(), "unknown IE depletion must not report remaining percentage");
        assertTrue(IECoreSampleReader.isDepleted(-1, 1000, 0.0D), "unknown IE depletion should fall back to zero saturation");
    }

    private static void assertOptionalLong(long expected, OptionalLong actual, String message) {
        if (actual.isEmpty() || actual.getAsLong() != expected) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertOptionalDouble(double expected, OptionalDouble actual, String message) {
        if (actual.isEmpty() || Math.abs(actual.getAsDouble() - expected) > 0.000001D) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
