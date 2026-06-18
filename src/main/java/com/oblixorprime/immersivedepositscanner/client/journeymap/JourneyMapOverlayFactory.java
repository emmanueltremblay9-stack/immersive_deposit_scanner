package com.oblixorprime.immersivedepositscanner.client.journeymap;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.config.ClientConfig;
import com.oblixorprime.immersivedepositscanner.data.DepositSource;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import net.minecraft.core.BlockPos;

public final class JourneyMapOverlayFactory {
    private JourneyMapOverlayFactory() {
    }

    public static boolean shouldShow(TrackedDeposit deposit) {
        return switch (deposit.key().source()) {
            case IMMERSIVE_ENGINEERING -> ClientConfig.SHOW_IMMERSIVE_ENGINEERING.get();
            case IMMERSIVE_PETROLEUM -> ClientConfig.SHOW_IMMERSIVE_PETROLEUM.get();
        };
    }

    public static PolygonOverlay createOverlay(TrackedDeposit deposit) {
        int minX = deposit.key().chunkX() * 16;
        int minZ = deposit.key().chunkZ() * 16;
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        int y = ClientConfig.WAYPOINT_Y_FALLBACK.get();

        MapPolygon polygon = new MapPolygon(
                new BlockPos(minX, y, minZ),
                new BlockPos(maxX, y, minZ),
                new BlockPos(maxX, y, maxZ),
                new BlockPos(minX, y, maxZ)
        );
        ShapeProperties shape = new ShapeProperties()
                .setStrokeColor(JourneyMapMarkerFactory.color(deposit))
                .setFillColor(fillColor(deposit))
                .setStrokeOpacity(ClientConfig.OVERLAY_STROKE_OPACITY.get().floatValue())
                .setFillOpacity(ClientConfig.OVERLAY_FILL_OPACITY.get().floatValue())
                .setStrokeWidth(deposit.key().source() == DepositSource.IMMERSIVE_PETROLEUM ? 2.0F : 1.0F);

        PolygonOverlay overlay = new PolygonOverlay(ImmersiveDepositScanner.MOD_ID, deposit.key().dimension(), shape, polygon);
        overlay.setOverlayGroupName(JourneyMapMarkerFactory.groupName(deposit));
        overlay.setTitle(JourneyMapMarkerFactory.label(deposit));
        overlay.setLabel(JourneyMapMarkerFactory.label(deposit));
        overlay.setDisplayOrder(50);
        return overlay;
    }

    private static int fillColor(TrackedDeposit deposit) {
        int color = JourneyMapMarkerFactory.color(deposit);
        return deposit.key().source() == DepositSource.IMMERSIVE_PETROLEUM
                ? (color ^ 0x003366)
                : color;
    }
}

