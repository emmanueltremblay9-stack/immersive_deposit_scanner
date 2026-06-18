package com.oblixorprime.immersivedepositscanner.client;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.client.journeymap.JourneyMapDepositManager;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import com.oblixorprime.immersivedepositscanner.data.TrackedDepositKey;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class ClientDepositCache {
    private static final Map<TrackedDepositKey, TrackedDeposit> DEPOSITS = new LinkedHashMap<>();
    private static final Map<TrackedDepositKey, TrackedDeposit> SYNC_STAGING = new LinkedHashMap<>();
    private static UUID activeSyncId;
    private static int expectedSyncDeposits = -1;

    private ClientDepositCache() {
    }

    public static void beginSync(UUID syncId, int expectedDeposits) {
        activeSyncId = syncId;
        expectedSyncDeposits = expectedDeposits;
        SYNC_STAGING.clear();
    }

    public static void acceptBatch(UUID syncId, Collection<TrackedDeposit> deposits) {
        if (!syncId.equals(activeSyncId)) {
            return;
        }
        deposits.forEach(deposit -> SYNC_STAGING.put(deposit.key(), deposit));
    }

    public static void finishSync(UUID syncId) {
        if (!syncId.equals(activeSyncId)) {
            return;
        }
        if (SYNC_STAGING.size() != expectedSyncDeposits) {
            ImmersiveDepositScanner.LOGGER.warn(
                    "Ignoring incomplete deposit sync {}; expected {} entries but received {}",
                    syncId,
                    expectedSyncDeposits,
                    SYNC_STAGING.size()
            );
            SYNC_STAGING.clear();
            activeSyncId = null;
            expectedSyncDeposits = -1;
            return;
        }
        DEPOSITS.clear();
        DEPOSITS.putAll(SYNC_STAGING);
        SYNC_STAGING.clear();
        activeSyncId = null;
        expectedSyncDeposits = -1;
        JourneyMapDepositManager.replaceAll(DEPOSITS.values());
    }

    public static void upsert(TrackedDeposit deposit) {
        DEPOSITS.put(deposit.key(), deposit);
        if (activeSyncId != null) {
            SYNC_STAGING.put(deposit.key(), deposit);
        }
        JourneyMapDepositManager.upsert(deposit);
    }

    public static void remove(TrackedDepositKey key) {
        DEPOSITS.remove(key);
        if (activeSyncId != null) {
            SYNC_STAGING.remove(key);
        }
        JourneyMapDepositManager.remove(key);
    }

    public static void clear() {
        DEPOSITS.clear();
        SYNC_STAGING.clear();
        activeSyncId = null;
        expectedSyncDeposits = -1;
        JourneyMapDepositManager.clearAll();
    }

    public static List<TrackedDeposit> getAll() {
        return List.copyOf(DEPOSITS.values());
    }

    public static List<TrackedDeposit> getForCurrentDimension() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return List.of();
        }
        ResourceKey<Level> dimension = minecraft.level.dimension();
        return DEPOSITS.values().stream()
                .filter(deposit -> deposit.key().dimension().equals(dimension))
                .toList();
    }
}
