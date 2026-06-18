package com.oblixorprime.immersivedepositscanner.client.journeymap;

public final class JourneyMapLifecycleHandler {
    private JourneyMapLifecycleHandler() {
    }

    public static void clearRuntimeObjects() {
        JourneyMapDepositManager.clearAll();
    }
}

