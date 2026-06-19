package com.oblixorprime.immersivedepositscanner.qa;

import com.oblixorprime.immersivedepositscanner.client.ClientDepositCache;
import com.oblixorprime.immersivedepositscanner.data.DepositKind;
import com.oblixorprime.immersivedepositscanner.data.DepositSource;
import com.oblixorprime.immersivedepositscanner.data.ImmersiveDepositSavedData;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import com.oblixorprime.immersivedepositscanner.data.TrackedDepositKey;
import com.oblixorprime.immersivedepositscanner.network.payload.DepositRemovePayload;
import com.oblixorprime.immersivedepositscanner.network.payload.DepositUpsertPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncBatchPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncEndPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncStartPayload;
import com.oblixorprime.immersivedepositscanner.tracking.DepositTrackingService;
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

public final class DataModelQa {
    private DataModelQa() {
    }

    public static void main(String[] args) {
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

        assertEquals(1, data.clearForPlayer(secondPlayer), "clearing second player's data must remove their visibility");
        assertTrue(data.canSee(ieKey, player), "clearing one player must not remove another player's visibility");
        assertTrue(!data.canSee(ieKey, secondPlayer), "clearing one player must remove their personal visibility");
        assertEquals(2, data.getAll().size(), "clearing one visible owner must not delete shared deposits that others still know");

        TrackedDeposit roundTrip = TrackedDeposit.fromTag(ip.toTag());
        assertTrue(roundTrip.equals(ip), "deposit NBT round-trip should preserve data");

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

        UUID removeSyncId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        ClientDepositCache.beginSync(removeSyncId, 0);
        ClientDepositCache.remove(ieKey);
        ClientDepositCache.finishSync(removeSyncId);
        assertEquals(0, ClientDepositCache.getAll().size(), "in-flight removes must not be restored by full sync finish");
    }

    private static TrackedDeposit deposit(TrackedDepositKey key, DepositKind kind, String name, UUID player, long time) {
        return new TrackedDeposit(
                key,
                kind,
                name,
                Optional.of(key.depositId()),
                Optional.of(new BlockPos(key.chunkX() * 16 + 8, 64, key.chunkZ() * 16 + 8)),
                player,
                time,
                time,
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

    private static void assertThrows(Runnable action, String message) {
        try {
            action.run();
        } catch (RuntimeException expected) {
            return;
        }
        throw new AssertionError(message);
    }
}
