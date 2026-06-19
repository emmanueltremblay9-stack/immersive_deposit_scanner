package com.oblixorprime.immersivedepositscanner.tracking;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.config.ServerConfig;
import com.oblixorprime.immersivedepositscanner.config.SharingMode;
import com.oblixorprime.immersivedepositscanner.data.DepositSource;
import com.oblixorprime.immersivedepositscanner.data.ImmersiveDepositSavedData;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import com.oblixorprime.immersivedepositscanner.data.TrackedDepositKey;
import com.oblixorprime.immersivedepositscanner.integration.ie.IECoreSampleReader;
import com.oblixorprime.immersivedepositscanner.integration.ie.IEMineralDiscovery;
import com.oblixorprime.immersivedepositscanner.integration.ip.IPReservoirDiscovery;
import com.oblixorprime.immersivedepositscanner.integration.ip.IPReservoirReader;
import com.oblixorprime.immersivedepositscanner.network.NetworkHandler;
import com.oblixorprime.immersivedepositscanner.network.payload.DepositClearPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.DepositUpsertPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncBatchPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncEndPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncStartPayload;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

public final class DepositTrackingService {
    private static final IECoreSampleReader IE_READER = new IECoreSampleReader();
    private static final IPReservoirReader IP_READER = new IPReservoirReader();

    private DepositTrackingService() {
    }

    public static List<TrackedDeposit> scanHeld(ServerPlayer player) {
        List<TrackedDeposit> main = scanStack(player, player.getItemInHand(InteractionHand.MAIN_HAND));
        if (!main.isEmpty()) {
            return main;
        }
        return scanStack(player, player.getItemInHand(InteractionHand.OFF_HAND));
    }

    public static List<TrackedDeposit> scanStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }

        List<TrackedDeposit> discoveries = new ArrayList<>();
        if (ServerConfig.TRACK_IMMERSIVE_ENGINEERING.get()) {
            discoveries.addAll(IE_READER.read(player.serverLevel(), player, stack).stream()
                    .map(IEMineralDiscovery::deposit)
                    .toList());
        }
        if (ServerConfig.TRACK_IMMERSIVE_PETROLEUM.get()) {
            discoveries.addAll(IP_READER.read(player.serverLevel(), player, stack).stream()
                    .map(IPReservoirDiscovery::deposit)
                    .filter(deposit -> ServerConfig.TRACK_DEPLETED_RESERVOIRS.get() || !deposit.depleted())
                    .toList());
        }

        if (discoveries.isEmpty()) {
            return List.of();
        }
        return recordDiscoveries(player, discoveries);
    }

    public static List<TrackedDeposit> recordDiscoveries(ServerPlayer player, Collection<TrackedDeposit> discoveries) {
        ImmersiveDepositSavedData data = ImmersiveDepositSavedData.get(player.server);
        List<TrackedDeposit> recorded = new ArrayList<>();
        for (TrackedDeposit discovery : discoveries) {
            TrackedDeposit effective = data.addOrUpdate(discovery, player.getUUID());
            recorded.add(effective);
            sendUpsertToVisiblePlayers(data, player, effective);
        }
        return recorded;
    }

    public static void sendFullSync(ServerPlayer player) {
        ImmersiveDepositSavedData data = ImmersiveDepositSavedData.get(player.server);
        List<TrackedDeposit> allVisible = visibleDeposits(data, player);
        List<TrackedDeposit> visible = limitForFullSync(allVisible);
        if (visible.size() < allVisible.size()) {
            ImmersiveDepositScanner.LOGGER.warn(
                    "Full deposit sync for {} reached the {} entry protocol limit; entries beyond the cap are not sent",
                    player.getGameProfile().getName(),
                    FullSyncStartPayload.MAX_EXPECTED_DEPOSITS
            );
        }
        UUID syncId = UUID.randomUUID();
        NetworkHandler.sendToPlayer(player, new FullSyncStartPayload(syncId, visible.size()));
        for (int start = 0; start < visible.size(); start += FullSyncBatchPayload.MAX_BATCH_SIZE) {
            int end = Math.min(start + FullSyncBatchPayload.MAX_BATCH_SIZE, visible.size());
            NetworkHandler.sendToPlayer(player, new FullSyncBatchPayload(syncId, visible.subList(start, end)));
        }
        NetworkHandler.sendToPlayer(player, new FullSyncEndPayload(syncId));
    }

    public static int clearMine(ServerPlayer player) {
        int removed = ImmersiveDepositSavedData.get(player.server).clearForPlayer(player.getUUID());
        resyncAll(player);
        return removed;
    }

    public static void clearAll(ServerPlayer player) {
        clearAll(player.server);
    }

    public static void clearAll(MinecraftServer server) {
        ImmersiveDepositSavedData.get(server).clearAll();
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            NetworkHandler.sendToPlayer(target, new DepositClearPayload());
        }
    }

    public static int removeCurrentChunk(ServerPlayer player, DepositSource sourceFilter) {
        ImmersiveDepositSavedData data = ImmersiveDepositSavedData.get(player.server);
        ChunkPos chunk = player.chunkPosition();
        List<TrackedDepositKey> keys = data.getForChunk(player.level().dimension(), chunk).stream()
                .filter(deposit -> sourceFilter == null || deposit.key().source() == sourceFilter)
                .filter(deposit -> canPlayerRemove(data, player, deposit))
                .map(TrackedDeposit::key)
                .toList();
        int removed = removeKeysForMode(data, player, keys);
        if (removed > 0) {
            resyncAll(player);
        }
        return removed;
    }

    public static List<TrackedDeposit> visibleDeposits(ServerPlayer player) {
        return visibleDeposits(ImmersiveDepositSavedData.get(player.server), player);
    }

    public static List<TrackedDeposit> limitForFullSync(List<TrackedDeposit> visible) {
        Objects.requireNonNull(visible, "visible");
        if (visible.size() <= FullSyncStartPayload.MAX_EXPECTED_DEPOSITS) {
            return visible;
        }
        return List.copyOf(visible.subList(0, FullSyncStartPayload.MAX_EXPECTED_DEPOSITS));
    }

    public static Component debugHeldSample(ServerPlayer player) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.isEmpty()) {
            stack = player.getItemInHand(InteractionHand.OFF_HAND);
        }
        return Component.literal(stack.isEmpty() ? "empty" : stack.getItem().toString() + " x" + stack.getCount());
    }

    private static List<TrackedDeposit> visibleDeposits(ImmersiveDepositSavedData data, ServerPlayer player) {
        return data.visibleTo(player.getUUID(), ServerConfig.SHARING_MODE.get() == SharingMode.SERVER);
    }

    private static boolean canPlayerRemove(ImmersiveDepositSavedData data, ServerPlayer player, TrackedDeposit deposit) {
        return player.hasPermissions(2)
                || (ServerConfig.ALLOW_PLAYER_REMOVE_OWN_ENTRIES.get() && data.canSee(deposit.key(), player.getUUID()));
    }

    private static int removeKeysForMode(ImmersiveDepositSavedData data, ServerPlayer player, List<TrackedDepositKey> keys) {
        if (ServerConfig.SHARING_MODE.get() == SharingMode.SERVER || player.hasPermissions(2)) {
            return data.removeAll(keys);
        }
        int removed = 0;
        for (TrackedDepositKey key : keys) {
            if (data.removeForPlayer(key, player.getUUID())) {
                removed++;
            }
        }
        return removed;
    }

    private static void sendUpsertToVisiblePlayers(ImmersiveDepositSavedData data, ServerPlayer sourcePlayer, TrackedDeposit deposit) {
        for (ServerPlayer target : sourcePlayer.server.getPlayerList().getPlayers()) {
            if (ServerConfig.SHARING_MODE.get() == SharingMode.SERVER || data.canSee(deposit.key(), target.getUUID())) {
                NetworkHandler.sendToPlayer(target, new DepositUpsertPayload(deposit));
            }
        }
    }

    private static void resyncAll(ServerPlayer sourcePlayer) {
        for (ServerPlayer target : sourcePlayer.server.getPlayerList().getPlayers()) {
            sendFullSync(target);
        }
    }
}
