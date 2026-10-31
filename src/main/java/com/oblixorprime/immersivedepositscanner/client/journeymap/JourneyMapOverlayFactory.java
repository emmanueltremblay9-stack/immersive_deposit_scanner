package com.oblixorprime.immersivedepositscanner.client.journeymap;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.config.ClientConfig;
import com.oblixorprime.immersivedepositscanner.data.DepositSource;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class JourneyMapOverlayFactory {
    private JourneyMapOverlayFactory() {
    }

    public static boolean shouldShow(TrackedDeposit deposit) {
        return switch (deposit.key().source()) {
            case IMMERSIVE_ENGINEERING -> getBooleanSetting(
                    ClientConfig.SHOW_IMMERSIVE_ENGINEERING,
                    "showImmersiveEngineering",
                    true
            );
            case IMMERSIVE_PETROLEUM -> getBooleanSetting(
                    ClientConfig.SHOW_IMMERSIVE_PETROLEUM,
                    "showImmersivePetroleum",
                    true
            );
        };
    }

    public static PolygonOverlay createOverlay(TrackedDeposit deposit) {
        int minX = deposit.key().chunkX() * 16;
        int minZ = deposit.key().chunkZ() * 16;
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        int y = getIntSetting(ClientConfig.WAYPOINT_Y_FALLBACK, "waypointYFallback", 64);

        MapPolygon polygon = new MapPolygon(
                new BlockPos(minX, y, minZ),
                new BlockPos(maxX, y, minZ),
                new BlockPos(maxX, y, maxZ),
                new BlockPos(minX, y, maxZ)
        );
        ShapeProperties shape = new ShapeProperties()
                .setStrokeColor(JourneyMapMarkerFactory.color(deposit))
                .setFillColor(fillColor(deposit))
                .setStrokeOpacity(getDoubleSetting(
                        ClientConfig.OVERLAY_STROKE_OPACITY,
                        "overlayStrokeOpacity",
                        0.85D
                ).floatValue())
                .setFillOpacity(getDoubleSetting(
                        ClientConfig.OVERLAY_FILL_OPACITY,
                        "overlayFillOpacity",
                        0.20D
                ).floatValue())
                .setStrokeWidth(deposit.key().source() == DepositSource.IMMERSIVE_PETROLEUM ? 2.0F : 1.0F);

        PolygonOverlay overlay = new PolygonOverlay(ImmersiveDepositScanner.MOD_ID, deposit.key().dimension(), shape, polygon);
        overlay.setOverlayGroupName(JourneyMapMarkerFactory.groupName(deposit));
        String label = JourneyMapMarkerFactory.label(deposit);
        overlay.setTitle(label);
        overlay.setLabel(overlayMapLabel(label));
        overlay.setDisplayOrder(50);
        return overlay;
    }

    static String overlayMapLabel(
            TrackedDeposit deposit,
            boolean showOverlayLabels,
            boolean showResourceSubject,
            boolean showRemainingPercentage,
            boolean showAmounts
    ) {
        return showOverlayLabels
                ? JourneyMapMarkerFactory.label(deposit, showResourceSubject, showRemainingPercentage, showAmounts)
                : "";
    }

    private static int fillColor(TrackedDeposit deposit) {
        int color = JourneyMapMarkerFactory.color(deposit);
        return deposit.key().source() == DepositSource.IMMERSIVE_PETROLEUM
                ? (color ^ 0x003366)
                : color;
    }

    private static String overlayMapLabel(String label) {
        return getBooleanSetting(
                ClientConfig.SHOW_JOURNEYMAP_OVERLAY_LABELS,
                "showJourneyMapOverlayLabels",
                false
        ) ? label : "";
    }

    private static boolean getBooleanSetting(ModConfigSpec.BooleanValue setting, String settingName, boolean fallback) {
        try {
            return setting.get();
        } catch (IllegalStateException exception) {
            ImmersiveDepositScanner.LOGGER.debug(
                    "Unable to read client config {}; using fallback {}",
                    settingName,
                    fallback,
                    exception
            );
            return fallback;
        }
    }

    private static int getIntSetting(ModConfigSpec.IntValue setting, String settingName, int fallback) {
        try {
            return setting.get();
        } catch (IllegalStateException exception) {
            ImmersiveDepositScanner.LOGGER.debug(
                    "Unable to read client config {}; using fallback {}",
                    settingName,
                    fallback,
                    exception
            );
            return fallback;
        }
    }

    private static Double getDoubleSetting(ModConfigSpec.DoubleValue setting, String settingName, double fallback) {
        try {
            return setting.get();
        } catch (IllegalStateException exception) {
            ImmersiveDepositScanner.LOGGER.debug(
                    "Unable to read client config {}; using fallback {}",
                    settingName,
                    fallback,
                    exception
            );
            return fallback;
        }
    }
}
