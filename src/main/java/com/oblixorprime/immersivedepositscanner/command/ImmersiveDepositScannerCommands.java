package com.oblixorprime.immersivedepositscanner.command;

import com.mojang.brigadier.CommandDispatcher;
import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.config.ClientConfig;
import com.oblixorprime.immersivedepositscanner.data.DepositSource;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import com.oblixorprime.immersivedepositscanner.tracking.DepositTrackingService;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ImmersiveDepositScannerCommands {
    public static final String PRIMARY_ROOT = "ids";
    public static final String LONG_ROOT = "immersivedepositscanner";
    public static final String LEGACY_ROOT = "mst";

    private ImmersiveDepositScannerCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        registerRoot(event.getDispatcher(), PRIMARY_ROOT);
        registerRoot(event.getDispatcher(), LONG_ROOT);
        registerRoot(event.getDispatcher(), LEGACY_ROOT);
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String root) {
        dispatcher.register(Commands.literal(root)
                .then(Commands.literal("list")
                        .executes(context -> list(context.getSource(), null, false))
                        .then(Commands.literal("ie").executes(context -> list(context.getSource(), DepositSource.IMMERSIVE_ENGINEERING, false)))
                        .then(Commands.literal("ip").executes(context -> list(context.getSource(), DepositSource.IMMERSIVE_PETROLEUM, false)))
                        .then(Commands.literal("currentchunk").executes(context -> list(context.getSource(), null, true))))
                .then(Commands.literal("version").executes(context -> version(context.getSource())))
                .then(Commands.literal("resync").executes(context -> resync(context.getSource())))
                .then(Commands.literal("scanheld").executes(context -> scanHeld(context.getSource())))
                .then(Commands.literal("remove")
                        .then(Commands.literal("currentchunk").executes(context -> removeCurrentChunk(context.getSource(), null))
                                .then(Commands.literal("ie").executes(context -> removeCurrentChunk(context.getSource(), DepositSource.IMMERSIVE_ENGINEERING)))
                                .then(Commands.literal("ip").executes(context -> removeCurrentChunk(context.getSource(), DepositSource.IMMERSIVE_PETROLEUM)))
                                .then(Commands.literal("all").executes(context -> removeCurrentChunk(context.getSource(), null)))))
                .then(Commands.literal("clear")
                        .then(Commands.literal("mine").executes(context -> clearMine(context.getSource())))
                        .then(Commands.literal("all").requires(source -> source.hasPermission(2)).executes(context -> clearAll(context.getSource()))))
                .then(Commands.literal("debug")
                        .then(Commands.literal("heldsample").requires(source -> source.hasPermission(2)).executes(context -> debugHeldSample(context.getSource())))));
    }

    private static int scanHeld(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<TrackedDeposit> deposits = DepositTrackingService.scanHeld(player);
        if (deposits.isEmpty()) {
            source.sendFailure(Component.translatable("commands.immersive_deposit_scanner.scanheld.none"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.immersive_deposit_scanner.scanheld.success", deposits.size()), false);
        return deposits.size();
    }

    private static int version(CommandSourceStack source) {
        formatVersionLines(ImmersiveDepositScanner.loadedVersion())
                .forEach(line -> source.sendSuccess(() -> Component.literal(line), false));
        return 1;
    }

    private static int resync(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        DepositTrackingService.sendFullSync(player);
        source.sendSuccess(() -> Component.translatable("commands.immersive_deposit_scanner.resync"), false);
        return 1;
    }

    private static int list(CommandSourceStack source, DepositSource sourceFilter, boolean currentChunkOnly) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<TrackedDeposit> deposits = DepositTrackingService.visibleDeposits(player).stream()
                .filter(deposit -> sourceFilter == null || deposit.key().source() == sourceFilter)
                .filter(deposit -> !currentChunkOnly || (deposit.key().dimension().equals(player.level().dimension())
                        && deposit.key().chunkX() == player.chunkPosition().x
                        && deposit.key().chunkZ() == player.chunkPosition().z))
                .toList();
        if (deposits.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.immersive_deposit_scanner.list.empty"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.immersive_deposit_scanner.list.header", deposits.size()), false);
        deposits.stream().limit(10).forEach(deposit -> source.sendSuccess(() -> Component.literal(formatDeposit(deposit)), false));
        return deposits.size();
    }

    private static int removeCurrentChunk(CommandSourceStack source, DepositSource sourceFilter) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int removed = DepositTrackingService.removeCurrentChunk(player, sourceFilter);
        source.sendSuccess(() -> Component.translatable("commands.immersive_deposit_scanner.remove.currentchunk", removed), true);
        return removed;
    }

    private static int clearMine(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int removed = DepositTrackingService.clearMine(player);
        source.sendSuccess(() -> Component.translatable("commands.immersive_deposit_scanner.clear.mine"), true);
        return removed;
    }

    private static int clearAll(CommandSourceStack source) {
        DepositTrackingService.clearAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("commands.immersive_deposit_scanner.clear.all"), true);
        return 1;
    }

    private static int debugHeldSample(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.translatable("commands.immersive_deposit_scanner.debug.heldsample", DepositTrackingService.debugHeldSample(player)), false);
        return 1;
    }

    static String formatDeposit(TrackedDeposit deposit) {
        return "%s source=%s chunk=%s %d,%d resourceId=%s depositId=%s".formatted(
                deposit.displayName(),
                deposit.key().source().serializedName(),
                deposit.key().dimension().location(),
                deposit.key().chunkX(),
                deposit.key().chunkZ(),
                deposit.resourceId().map(ResourceLocation::toString).orElse("<none>"),
                deposit.key().depositId()
        );
    }

    static List<String> formatVersionLines(String modVersion) {
        return List.of(
                "modId=" + ImmersiveDepositScanner.MOD_ID,
                "modVersion=" + modVersion,
                "journeyMapLabelCleanup=active",
                journeyMapClientDefaultsLine(),
                "journeyMapClientConfig=client-local; server command shows code defaults, not each client's current config file"
        );
    }

    private static String journeyMapClientDefaultsLine() {
        try {
            return "journeyMapClientDefaults enableJourneyMapMarkers=%s enableJourneyMapChunkOverlays=%s enableWaypoints=%s markerLabels=%s overlayLabels=%s waypointLabels=%s subjectIdsInLabel=%s".formatted(
                    ClientConfig.ENABLE_JOURNEYMAP_MARKERS.getDefault(),
                    ClientConfig.ENABLE_JOURNEYMAP_CHUNK_OVERLAYS.getDefault(),
                    ClientConfig.ENABLE_WAYPOINTS.getDefault(),
                    ClientConfig.SHOW_JOURNEYMAP_MARKER_LABELS.getDefault(),
                    ClientConfig.SHOW_JOURNEYMAP_OVERLAY_LABELS.getDefault(),
                    ClientConfig.SHOW_JOURNEYMAP_WAYPOINT_LABELS.getDefault(),
                    ClientConfig.SHOW_JOURNEYMAP_SUBJECT_IDS_IN_LABEL.getDefault()
            );
        } catch (RuntimeException exception) {
            return "journeyMapClientDefaults=<unavailable server-side>";
        }
    }
}
