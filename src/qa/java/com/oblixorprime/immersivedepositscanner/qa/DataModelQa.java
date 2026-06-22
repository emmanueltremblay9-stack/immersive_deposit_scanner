package com.oblixorprime.immersivedepositscanner.qa;

import com.oblixorprime.immersivedepositscanner.client.ClientDepositCache;
import com.oblixorprime.immersivedepositscanner.client.journeymap.JourneyMapDepositManager;
import com.oblixorprime.immersivedepositscanner.data.DepositKind;
import com.oblixorprime.immersivedepositscanner.data.DepositSource;
import com.oblixorprime.immersivedepositscanner.data.ImmersiveDepositSavedData;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import com.oblixorprime.immersivedepositscanner.data.TrackedDepositKey;
import com.oblixorprime.immersivedepositscanner.integration.ie.IECoreSampleReaderQa;
import com.oblixorprime.immersivedepositscanner.network.payload.DepositRemovePayload;
import com.oblixorprime.immersivedepositscanner.network.payload.DepositUpsertPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncBatchPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncEndPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncStartPayload;
import com.oblixorprime.immersivedepositscanner.tracking.DepositTrackingService;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import journeymap.api.v2.client.IClientAPI;

public final class DataModelQa {
    private DataModelQa() {
    }

    public static void main(String[] args) {
        assertCommonNetworkHandlerDoesNotDirectlyLinkClientHandlers();
        assertJourneyMapCleanupToleratesApiFailures();
        IECoreSampleReaderQa.run();

        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondPlayer = UUID.fromString("00000000-0000-0000-0000-000000000002");
        TrackedDepositKey ieKey = new TrackedDepositKey(
                Level.OVERWORLD,
                12,
                -4,
                DepositSource.IMMERSIVE_ENGINEERING,
                ResourceLocation.fromNamespaceAndPath("immersiveengineering", "sample_mineral")
        );
        TrackedDepositKey ipKey = new TrackedDepositKey(
                Level.OVERWORLD,
                12,
                -4,
                DepositSource.IMMERSIVE_PETROLEUM,
                ResourceLocation.fromNamespaceAndPath("immersivepetroleum", "sample_reservoir")
        );

        assertTrue(ieKey.stableId().equals(new TrackedDepositKey(
                Level.OVERWORLD,
                12,
                -4,
                DepositSource.IMMERSIVE_ENGINEERING,
                ResourceLocation.fromNamespaceAndPath("immersiveengineering", "sample_mineral")
        ).stableId()), "stable key id should be deterministic");

        ImmersiveDepositSavedData data = new ImmersiveDepositSavedData();
        TrackedDeposit ie = deposit(ieKey, DepositKind.MINERAL, "Sample Mineral", player, 1000L);
        TrackedDeposit ip = deposit(ipKey, DepositKind.FLUID_RESERVOIR, "Sample Reservoir", player, 1000L);
        data.addOrUpdate(ie);
        data.addOrUpdate(ip);
        assertEquals(2, data.getForChunk(Level.OVERWORLD, new net.minecraft.world.level.ChunkPos(12, -4)).size(), "IE and IP entries must coexist in one chunk");

        data.addOrUpdate(deposit(ieKey, DepositKind.MINERAL, "Updated Mineral", player, 2000L));
        assertEquals(2, data.getAll().size(), "same deposit key must update instead of duplicate");
        assertEquals(1000L, data.get(ieKey).orElseThrow().discoveredAt(), "rediscovery must preserve first discoveredAt");
        assertEquals("Updated Mineral", data.get(ieKey).orElseThrow().displayName(), "rediscovery must update technical data");

        TrackedDeposit secondDiscovery = data.addOrUpdate(deposit(ieKey, DepositKind.MINERAL, "Second Player Mineral", secondPlayer, 3000L), secondPlayer);
        assertEquals(player, secondDiscovery.discoveredBy(), "shared deposit keys must preserve the first discoverer");
        assertEquals(1000L, secondDiscovery.discoveredAt(), "shared deposit keys must preserve the first discovery time");
        assertEquals(1, data.visibleTo(secondPlayer, false).size(), "second discoverer must see rediscovered personal deposit");

        CompoundTag saved = data.save(new CompoundTag(), null);
        ImmersiveDepositSavedData loaded = ImmersiveDepositSavedData.load(saved, null);
        assertTrue(loaded.canSee(ieKey, player), "knownBy must preserve first player's personal visibility");
        assertTrue(loaded.canSee(ieKey, secondPlayer), "knownBy must preserve later discoverer's personal visibility");

        CompoundTag duplicateSaved = new CompoundTag();
        ListTag duplicateEntries = new ListTag();
        TrackedDeposit firstSavedDeposit = deposit(ieKey, DepositKind.MINERAL, "First Saved Mineral", player, 1000L);
        TrackedDeposit secondSavedDeposit = deposit(ieKey, DepositKind.MINERAL, "Second Saved Mineral", secondPlayer, 3000L);
        duplicateEntries.add(withKnownBy(firstSavedDeposit.toTag(), player));
        duplicateEntries.add(withKnownBy(secondSavedDeposit.toTag(), secondPlayer));
        duplicateSaved.put("deposits", duplicateEntries);
        ImmersiveDepositSavedData duplicateLoaded = ImmersiveDepositSavedData.load(duplicateSaved, null);
        assertEquals(1, duplicateLoaded.getAll().size(), "duplicate saved keys must be merged on load");
        assertEquals(player, duplicateLoaded.get(ieKey).orElseThrow().discoveredBy(), "duplicate load must preserve first discoverer");
        assertTrue(duplicateLoaded.canSee(ieKey, player), "duplicate load must keep first known player");
        assertTrue(duplicateLoaded.canSee(ieKey, secondPlayer), "duplicate load must merge later known player");

        CompoundTag corruptSaved = new CompoundTag();
        ListTag corruptEntries = new ListTag();
        CompoundTag missingChunkEntry = ip.toTag();
        missingChunkEntry.getCompound("key").remove("chunkZ");
        corruptEntries.add(withKnownBy(missingChunkEntry, player));
        CompoundTag unknownSourceEntry = ip.toTag();
        unknownSourceEntry.getCompound("key").putString("source", "unknown_source");
        corruptEntries.add(withKnownBy(unknownSourceEntry, player));
        CompoundTag unknownKindEntry = ip.toTag();
        unknownKindEntry.putString("kind", "unknown_kind");
        corruptEntries.add(withKnownBy(unknownKindEntry, player));
        corruptSaved.put("deposits", corruptEntries);
        ImmersiveDepositSavedData corruptLoaded = ImmersiveDepositSavedData.load(corruptSaved, null);
        assertEquals(0, corruptLoaded.getAll().size(), "corrupt saved entries must be skipped instead of loading as bogus deposits");

        assertEquals(1, data.clearForPlayer(secondPlayer), "clearing second player's data must remove their visibility");
        assertTrue(data.canSee(ieKey, player), "clearing one player must not remove another player's visibility");
        assertTrue(!data.canSee(ieKey, secondPlayer), "clearing one player must remove their personal visibility");
        assertEquals(2, data.getAll().size(), "clearing one visible owner must not delete shared deposits that others still know");

        TrackedDeposit roundTrip = TrackedDeposit.fromTag(ip.toTag());
        assertTrue(roundTrip.equals(ip), "deposit NBT round-trip should preserve data");
        CompoundTag partialSampleTag = ip.toTag();
        partialSampleTag.remove("sampleY");
        assertTrue(
                TrackedDeposit.fromTag(partialSampleTag).samplePosition().isEmpty(),
                "partial NBT sample positions must not create bogus coordinates"
        );
        CompoundTag wrongTypedAmounts = ip.toTag();
        wrongTypedAmounts.putString("currentAmount", "10");
        wrongTypedAmounts.putString("maximumAmount", "100");
        wrongTypedAmounts.putString("percentageRemaining", "0.10");
        TrackedDeposit wrongTypedAmountRoundTrip = TrackedDeposit.fromTag(wrongTypedAmounts);
        assertTrue(wrongTypedAmountRoundTrip.currentAmount().isEmpty(), "wrong-type current amounts must not load as zero");
        assertTrue(wrongTypedAmountRoundTrip.maximumAmount().isEmpty(), "wrong-type maximum amounts must not load as zero");
        assertTrue(wrongTypedAmountRoundTrip.percentageRemaining().isEmpty(), "wrong-type percentages must not load as zero");
        TrackedDeposit invalidNumbers = depositWithNumbers(ieKey, DepositKind.MINERAL, "Invalid Numbers", player, 1000L,
                OptionalLong.of(-1L), OptionalLong.of(-100L), OptionalDouble.of(Double.NaN));
        assertTrue(invalidNumbers.currentAmount().isEmpty(), "negative current amounts must be discarded");
        assertTrue(invalidNumbers.maximumAmount().isEmpty(), "negative maximum amounts must be discarded");
        assertTrue(invalidNumbers.percentageRemaining().isEmpty(), "non-finite percentages must be discarded");
        assertTrue(
                depositWithNumbers(ieKey, DepositKind.MINERAL, "Negative Percentage", player,
                        1000L, OptionalLong.of(1L), OptionalLong.of(100L), OptionalDouble.of(-0.1D)).percentageRemaining().isEmpty(),
                "negative percentages must be discarded"
        );
        assertTrue(
                depositWithNumbers(ieKey, DepositKind.MINERAL, "Oversized Percentage", player,
                        1000L, OptionalLong.of(1L), OptionalLong.of(100L), OptionalDouble.of(1.1D)).percentageRemaining().isEmpty(),
                "oversized percentages must be discarded"
        );
        TrackedDeposit outOfOrderTimes = depositWithTimes(ieKey, DepositKind.MINERAL, "Out Of Order Times", player, 5000L, 4000L);
        assertEquals(5000L, outOfOrderTimes.updatedAt(), "updatedAt must not be earlier than discoveredAt");

        assertThrows(() -> new FullSyncStartPayload(UUID.randomUUID(), -1), "negative expected sync count must be rejected");
        assertThrows(
                () -> new FullSyncStartPayload(UUID.randomUUID(), FullSyncStartPayload.MAX_EXPECTED_DEPOSITS + 1),
                "oversized expected sync count must be rejected"
        );
        List<TrackedDeposit> oversizedBatch = new ArrayList<>(Collections.nCopies(FullSyncBatchPayload.MAX_BATCH_SIZE + 1, ip));
        assertThrows(
                () -> new FullSyncBatchPayload(UUID.randomUUID(), oversizedBatch),
                "oversized sync batches must be rejected before encoding"
        );
        List<TrackedDeposit> oversizedSync = new ArrayList<>(Collections.nCopies(FullSyncStartPayload.MAX_EXPECTED_DEPOSITS + 1, ip));
        assertEquals(
                FullSyncStartPayload.MAX_EXPECTED_DEPOSITS,
                DepositTrackingService.limitForFullSync(oversizedSync).size(),
                "full sync selection must be capped before creating the start payload"
        );
        assertEquals(2, DepositTrackingService.limitForFullSync(List.of(ie, ip)).size(), "normal full sync selection must not be truncated");
        assertThrows(() -> new DepositUpsertPayload(null), "null upsert payload deposits must be rejected at construction");
        assertThrows(() -> new DepositRemovePayload(null), "null remove payload keys must be rejected at construction");
        assertThrows(() -> new FullSyncEndPayload(null), "null sync end ids must be rejected at construction");

        ClientDepositCache.clear();
        UUID syncId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        TrackedDeposit staleSyncDeposit = deposit(ieKey, DepositKind.MINERAL, "Stale Sync Mineral", player, 4000L);
        TrackedDeposit liveUpdateDeposit = deposit(ieKey, DepositKind.MINERAL, "Live Update Mineral", player, 5000L);
        ClientDepositCache.beginSync(syncId, 1);
        ClientDepositCache.acceptBatch(syncId, List.of(staleSyncDeposit));
        ClientDepositCache.upsert(liveUpdateDeposit);
        ClientDepositCache.finishSync(syncId);
        assertEquals("Live Update Mineral", ClientDepositCache.getAll().getFirst().displayName(), "in-flight upserts must survive full sync finish");

        UUID liveNewKeySyncId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        TrackedDeposit syncSnapshotDeposit = deposit(ieKey, DepositKind.MINERAL, "Snapshot Mineral", player, 6000L);
        TrackedDeposit liveNewKeyDeposit = deposit(ipKey, DepositKind.FLUID_RESERVOIR, "Live New Reservoir", player, 7000L);
        ClientDepositCache.clear();
        ClientDepositCache.beginSync(liveNewKeySyncId, 1);
        ClientDepositCache.acceptBatch(liveNewKeySyncId, List.of(syncSnapshotDeposit));
        ClientDepositCache.upsert(liveNewKeyDeposit);
        ClientDepositCache.finishSync(liveNewKeySyncId);
        assertEquals(2, ClientDepositCache.getAll().size(), "live new-key upserts must not make a complete sync look incomplete");
        assertCacheContains("Snapshot Mineral", "complete sync snapshot must still apply when a live new-key upsert arrives");
        assertCacheContains("Live New Reservoir", "live new-key upsert must survive full sync finish");

        UUID removeSyncId = UUID.fromString("00000000-0000-0000-0000-000000000005");
        ClientDepositCache.beginSync(removeSyncId, 0);
        ClientDepositCache.remove(ieKey);
        ClientDepositCache.finishSync(removeSyncId);
        assertEquals(0, ClientDepositCache.getAll().size(), "in-flight removes must not be restored by full sync finish");

        UUID liveRemoveSyncId = UUID.fromString("00000000-0000-0000-0000-000000000006");
        TrackedDeposit syncKeptDeposit = deposit(ipKey, DepositKind.FLUID_RESERVOIR, "Snapshot Reservoir", player, 8000L);
        ClientDepositCache.clear();
        ClientDepositCache.beginSync(liveRemoveSyncId, 2);
        ClientDepositCache.acceptBatch(liveRemoveSyncId, List.of(syncSnapshotDeposit, syncKeptDeposit));
        ClientDepositCache.remove(ieKey);
        ClientDepositCache.finishSync(liveRemoveSyncId);
        assertEquals(1, ClientDepositCache.getAll().size(), "live removes must not make a complete sync look incomplete");
        assertCacheMissing("Snapshot Mineral", "live remove must win over a full-sync snapshot entry");
        assertCacheContains("Snapshot Reservoir", "unremoved full-sync entries must still apply after a live remove");
    }

    private static void assertJourneyMapCleanupToleratesApiFailures() {
        Field apiField;
        Object originalApi;
        try {
            apiField = JourneyMapDepositManager.class.getDeclaredField("api");
            apiField.setAccessible(true);
            originalApi = apiField.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("JourneyMapDepositManager API field must be reachable for cleanup QA", exception);
        }

        IClientAPI throwingApi = (IClientAPI) Proxy.newProxyInstance(
                IClientAPI.class.getClassLoader(),
                new Class<?>[]{IClientAPI.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("removeAll") || method.getName().equals("removeAllWaypoints")) {
                        throw new IllegalStateException("simulated JourneyMap cleanup failure");
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        try {
            apiField.set(null, throwingApi);
            JourneyMapDepositManager.clearAll();
        } catch (RuntimeException exception) {
            throw new AssertionError("JourneyMap cleanup failures must not escape cache cleanup", exception);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("JourneyMapDepositManager API field must be writable for cleanup QA", exception);
        } finally {
            try {
                apiField.set(null, originalApi);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("JourneyMapDepositManager API field must be restorable after cleanup QA", exception);
            }
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive() || returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Character.TYPE) {
            return '\0';
        }
        return 0;
    }

    private static void assertCommonNetworkHandlerDoesNotDirectlyLinkClientHandlers() {
        String source;
        try {
            source = Files.readString(Path.of(
                    "src/main/java/com/oblixorprime/immersivedepositscanner/network/NetworkHandler.java"
            ));
        } catch (IOException exception) {
            throw new AssertionError("network handler source must be readable for side-boundary QA", exception);
        }

        assertTrue(
                !source.contains("import com.oblixorprime.immersivedepositscanner.client."),
                "common network handler must not import client package classes"
        );
        assertTrue(
                !source.contains("ClientPayloadHandlers.handle("),
                "common network handler must not directly invoke client payload handlers"
        );
    }

    private static TrackedDeposit deposit(TrackedDepositKey key, DepositKind kind, String name, UUID player, long time) {
        return depositWithNumbers(key, kind, name, player, time, OptionalLong.of(10), OptionalLong.of(100), OptionalDouble.of(0.10D));
    }

    private static TrackedDeposit depositWithNumbers(
            TrackedDepositKey key,
            DepositKind kind,
            String name,
            UUID player,
            long time,
            OptionalLong currentAmount,
            OptionalLong maximumAmount,
            OptionalDouble percentageRemaining
    ) {
        return new TrackedDeposit(
                key,
                kind,
                name,
                Optional.of(key.depositId()),
                Optional.of(new BlockPos(key.chunkX() * 16 + 8, 64, key.chunkZ() * 16 + 8)),
                player,
                time,
                time,
                currentAmount,
                maximumAmount,
                percentageRemaining,
                false
        );
    }

    private static TrackedDeposit depositWithTimes(
            TrackedDepositKey key,
            DepositKind kind,
            String name,
            UUID player,
            long discoveredAt,
            long updatedAt
    ) {
        return new TrackedDeposit(
                key,
                kind,
                name,
                Optional.of(key.depositId()),
                Optional.of(new BlockPos(key.chunkX() * 16 + 8, 64, key.chunkZ() * 16 + 8)),
                player,
                discoveredAt,
                updatedAt,
                OptionalLong.of(10),
                OptionalLong.of(100),
                OptionalDouble.of(0.10D),
                false
        );
    }

    private static CompoundTag withKnownBy(CompoundTag tag, UUID player) {
        ListTag knownBy = new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putUUID("player", player);
        knownBy.add(entry);
        tag.put("knownBy", knownBy);
        return tag;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertCacheContains(String displayName, String message) {
        assertTrue(ClientDepositCache.getAll().stream().anyMatch(deposit -> displayName.equals(deposit.displayName())), message);
    }

    private static void assertCacheMissing(String displayName, String message) {
        assertTrue(ClientDepositCache.getAll().stream().noneMatch(deposit -> displayName.equals(deposit.displayName())), message);
    }

    private static void assertThrows(Runnable action, String message) {
        try {
            action.run();
        } catch (RuntimeException expected) {
            return;
        }
        throw new AssertionError(message);
    }
}
