package com.oblixorprime.immersivedepositscanner.client;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.client.journeymap.JourneyMapDepositManager;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import com.oblixorprime.immersivedepositscanner.data.TrackedDepositKey;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class ClientDepositCache {
    private static final Map<TrackedDepositKey, TrackedDeposit> DEPOSITS = new LinkedHashMap<>();
    private static final Map<TrackedDepositKey, TrackedDeposit> SYNC_STAGING = new LinkedHashMap<>();
    private static final Map<TrackedDepositKey, TrackedDeposit> SYNC_LIVE_UPSERTS = new LinkedHashMap<>();
    private static final Set<TrackedDepositKey> SYNC_LIVE_REMOVES = new LinkedHashSet<>();
    private static UUID activeSyncId;
    private static int expectedSyncDeposits = -1;

    private ClientDepositCache() {
    }

    public static void beginSync(UUID syncId, int expectedDeposits) {
        activeSyncId = syncId;
        expectedSyncDeposits = expectedDeposits;
        SYNC_STAGING.clear();
        SYNC_LIVE_UPSERTS.clear();
        SYNC_LIVE_REMOVES.clear();
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
            clearSyncState();
            return;
        }
        DEPOSITS.clear();
        DEPOSITS.putAll(SYNC_STAGING);
        SYNC_LIVE_REMOVES.forEach(DEPOSITS::remove);
        DEPOSITS.putAll(SYNC_LIVE_UPSERTS);
        clearSyncState();
        JourneyMapDepositManager.replaceAll(DEPOSITS.values());
    }

    public static void upsert(TrackedDeposit deposit) {
        DEPOSITS.put(deposit.key(), deposit);
        if (activeSyncId != null) {
            SYNC_LIVE_REMOVES.remove(deposit.key());
            SYNC_LIVE_UPSERTS.put(deposit.key(), deposit);
        }
        JourneyMapDepositManager.upsert(deposit);
    }

    public static void remove(TrackedDepositKey key) {
        DEPOSITS.remove(key);
        if (activeSyncId != null) {
            SYNC_LIVE_UPSERTS.remove(key);
            SYNC_LIVE_REMOVES.add(key);
        }
        JourneyMapDepositManager.remove(key);
    }

    public static void clear() {
        DEPOSITS.clear();
        clearSyncState();
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

    private static void clearSyncState() {
        SYNC_STAGING.clear();
        SYNC_LIVE_UPSERTS.clear();
        SYNC_LIVE_REMOVES.clear();
        activeSyncId = null;
        expectedSyncDeposits = -1;
    }
}
