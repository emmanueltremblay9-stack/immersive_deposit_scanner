package com.oblixorprime.immersivedepositscanner.data;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public final class ImmersiveDepositSavedData extends SavedData {
    public static final String DATA_NAME = "immersive_deposit_scanner";
    public static final int FORMAT_VERSION = 2;

    private final Map<TrackedDepositKey, TrackedDeposit> deposits = new LinkedHashMap<>();
    private final Map<TrackedDepositKey, Set<UUID>> visibleOwners = new LinkedHashMap<>();

    public static SavedData.Factory<ImmersiveDepositSavedData> factory() {
        return new SavedData.Factory<>(ImmersiveDepositSavedData::new, ImmersiveDepositSavedData::load);
    }

    public static ImmersiveDepositSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    public static ImmersiveDepositSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ImmersiveDepositSavedData data = new ImmersiveDepositSavedData();
        ListTag entries = tag.getList("deposits", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            try {
                CompoundTag entry = entries.getCompound(i);
                TrackedDeposit deposit = TrackedDeposit.fromTag(entry);
                TrackedDeposit effective = Optional.ofNullable(data.deposits.get(deposit.key()))
                        .map(deposit::preservingFirstDiscovery)
                        .orElse(deposit);
                data.deposits.put(effective.key(), effective);
                data.visibleOwners.computeIfAbsent(effective.key(), ignored -> new HashSet<>())
                        .addAll(readKnownPlayers(entry, deposit.discoveredBy()));
            } catch (RuntimeException exception) {
                ImmersiveDepositScanner.LOGGER.error("Skipping corrupt Immersive Deposit Scanner saved-data entry {}", i, exception);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("formatVersion", FORMAT_VERSION);
        ListTag entries = new ListTag();
        getAll().forEach(deposit -> {
            CompoundTag entry = deposit.toTag();
            writeKnownPlayers(entry, visibleOwners.getOrDefault(deposit.key(), Set.of(deposit.discoveredBy())));
            entries.add(entry);
        });
        tag.put("deposits", entries);
        return tag;
    }

    public TrackedDeposit addOrUpdate(TrackedDeposit deposit) {
        return addOrUpdate(deposit, deposit.discoveredBy());
    }

    public TrackedDeposit addOrUpdate(TrackedDeposit deposit, UUID visibleOwner) {
        TrackedDeposit effective = Optional.ofNullable(deposits.get(deposit.key()))
                .map(deposit::preservingFirstDiscovery)
                .orElse(deposit);
        deposits.put(effective.key(), effective);
        visibleOwners.computeIfAbsent(effective.key(), ignored -> new HashSet<>()).add(visibleOwner);
        setDirty();
        return effective;
    }

    public boolean remove(TrackedDepositKey key) {
        boolean removed = deposits.remove(key) != null;
        visibleOwners.remove(key);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public boolean removeForPlayer(TrackedDepositKey key, UUID player) {
        Set<UUID> owners = visibleOwners.get(key);
        if (owners == null || !owners.remove(player)) {
            return false;
        }
        if (owners.isEmpty()) {
            deposits.remove(key);
            visibleOwners.remove(key);
        }
        setDirty();
        return true;
    }

    public int removeAll(Collection<TrackedDepositKey> keys) {
        int removed = 0;
        for (TrackedDepositKey key : keys) {
            if (deposits.remove(key) != null) {
                visibleOwners.remove(key);
                removed++;
            }
        }
        if (removed > 0) {
            setDirty();
        }
        return removed;
    }

    public void clearAll() {
        if (!deposits.isEmpty() || !visibleOwners.isEmpty()) {
            deposits.clear();
            visibleOwners.clear();
            setDirty();
        }
    }

    public int clearForPlayer(UUID player) {
        List<TrackedDepositKey> keys = deposits.keySet().stream()
                .filter(key -> visibleOwners.getOrDefault(key, Set.of()).contains(player))
                .toList();
        int removed = 0;
        for (TrackedDepositKey key : keys) {
            if (removeForPlayer(key, player)) {
                removed++;
            }
        }
        return removed;
    }

    public List<TrackedDeposit> getAll() {
        return deposits.values().stream()
                .sorted(Comparator.comparing((TrackedDeposit deposit) -> deposit.key().dimension().location().toString())
                        .thenComparingInt(deposit -> deposit.key().chunkX())
                        .thenComparingInt(deposit -> deposit.key().chunkZ())
                        .thenComparing(deposit -> deposit.key().source().serializedName())
                        .thenComparing(deposit -> deposit.key().depositId().toString()))
                .toList();
    }

    public List<TrackedDeposit> getForPlayer(UUID player) {
        return deposits.values().stream()
                .filter(deposit -> canSee(deposit.key(), player))
                .toList();
    }

    public List<TrackedDeposit> getForDimension(ResourceKey<Level> dimension) {
        return deposits.values().stream()
                .filter(deposit -> deposit.key().dimension().equals(dimension))
                .toList();
    }

    public List<TrackedDeposit> getForChunk(ResourceKey<Level> dimension, ChunkPos chunk) {
        return deposits.values().stream()
                .filter(deposit -> deposit.key().dimension().equals(dimension))
                .filter(deposit -> deposit.key().chunkX() == chunk.x)
                .filter(deposit -> deposit.key().chunkZ() == chunk.z)
                .toList();
    }

    public List<TrackedDeposit> getBySource(DepositSource source) {
        return deposits.values().stream()
                .filter(deposit -> deposit.key().source() == source)
                .toList();
    }

    public Optional<TrackedDeposit> get(TrackedDepositKey key) {
        return Optional.ofNullable(deposits.get(key));
    }

    public boolean isEmpty() {
        return deposits.isEmpty();
    }

    public List<TrackedDeposit> visibleTo(UUID player, boolean shareServerWide) {
        return shareServerWide ? new ArrayList<>(deposits.values()) : getForPlayer(player);
    }

    public boolean canSee(TrackedDepositKey key, UUID player) {
        return visibleOwners.getOrDefault(key, Set.of()).contains(player);
    }

    public boolean hasVisibleOwners(TrackedDepositKey key) {
        return !visibleOwners.getOrDefault(key, Set.of()).isEmpty();
    }

    private static Set<UUID> readKnownPlayers(CompoundTag tag, UUID fallback) {
        Set<UUID> players = new HashSet<>();
        ListTag knownBy = tag.getList("knownBy", Tag.TAG_COMPOUND);
        for (int i = 0; i < knownBy.size(); i++) {
            CompoundTag knownByEntry = knownBy.getCompound(i);
            if (knownByEntry.hasUUID("player")) {
                players.add(knownByEntry.getUUID("player"));
            }
        }
        if (players.isEmpty()) {
            players.add(fallback);
        }
        return players;
    }

    private static void writeKnownPlayers(CompoundTag tag, Set<UUID> players) {
        ListTag knownBy = new ListTag();
        players.stream()
                .sorted(Comparator.comparing(UUID::toString))
                .forEach(player -> {
                    CompoundTag entry = new CompoundTag();
                    entry.putUUID("player", player);
                    knownBy.add(entry);
                });
        tag.put("knownBy", knownBy);
    }
}
