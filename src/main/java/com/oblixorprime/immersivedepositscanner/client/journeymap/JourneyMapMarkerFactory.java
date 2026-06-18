package com.oblixorprime.immersivedepositscanner.client.journeymap;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.config.ClientConfig;
import com.oblixorprime.immersivedepositscanner.data.DepositSource;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class JourneyMapMarkerFactory {
    private JourneyMapMarkerFactory() {
    }

    public static boolean shouldShow(TrackedDeposit deposit) {
        return switch (deposit.key().source()) {
            case IMMERSIVE_ENGINEERING -> ClientConfig.SHOW_IMMERSIVE_ENGINEERING.get();
            case IMMERSIVE_PETROLEUM -> ClientConfig.SHOW_IMMERSIVE_PETROLEUM.get();
        };
    }

    public static Waypoint createWaypoint(TrackedDeposit deposit) {
        Waypoint waypoint = WaypointFactory.createClientWaypoint(label(deposit), position(deposit), groupName(deposit), deposit.key().dimension(), true);
        waypoint.setPersistent(false);
        waypoint.setColor(color(deposit));
        waypoint.setIconColor(color(deposit));
        waypoint.setIconResourceLoctaion(icon(deposit));
        waypoint.setIconTextureSize(16, 16);
        waypoint.setCustomData("ids:key", deposit.key().stableId());
        return waypoint;
    }

    public static MarkerOverlay createMarker(TrackedDeposit deposit) {
        MapImage image = new MapImage(icon(deposit), 16, 16).centerAnchors();
        MarkerOverlay marker = new MarkerOverlay(ImmersiveDepositScanner.MOD_ID, position(deposit), image);
        marker.setDimension(deposit.key().dimension());
        marker.setOverlayGroupName(groupName(deposit));
        marker.setTitle(label(deposit));
        marker.setLabel(label(deposit));
        marker.setDisplayOrder(100);
        return marker;
    }

    public static String label(TrackedDeposit deposit) {
        String prefix = deposit.key().source() == DepositSource.IMMERSIVE_ENGINEERING ? "[IE] " : "[IP] ";
        StringBuilder label = new StringBuilder(prefix)
                .append(deposit.displayName())
                .append(" [")
                .append(deposit.key().chunkX())
                .append(", ")
                .append(deposit.key().chunkZ())
                .append("]");
        deposit.percentageRemaining().ifPresent(value -> {
            if (ClientConfig.SHOW_REMAINING_PERCENTAGE.get()) {
                label.append(" ").append(Math.round(value * 100.0D)).append("%");
            }
        });
        if (ClientConfig.SHOW_AMOUNTS.get() && deposit.currentAmount().isPresent()) {
            label.append(" ").append(deposit.currentAmount().getAsLong());
            deposit.maximumAmount().ifPresent(max -> label.append("/").append(max));
        }
        if (deposit.depleted()) {
            label.append(" depleted");
        }
        return label.toString();
    }

    public static BlockPos position(TrackedDeposit deposit) {
        return deposit.samplePosition().orElseGet(() -> new BlockPos(
                deposit.key().chunkX() * 16 + 8,
                ClientConfig.WAYPOINT_Y_FALLBACK.get(),
                deposit.key().chunkZ() * 16 + 8
        ));
    }

    public static String groupName(TrackedDeposit deposit) {
        return deposit.key().source() == DepositSource.IMMERSIVE_ENGINEERING
                ? ClientConfig.IE_GROUP_NAME.get()
                : ClientConfig.IP_GROUP_NAME.get();
    }

    public static int color(TrackedDeposit deposit) {
        return deposit.key().source() == DepositSource.IMMERSIVE_ENGINEERING
                ? stableColor(deposit, ClientConfig.IE_DEFAULT_COLOR.get())
                : stableColor(deposit, ClientConfig.IP_DEFAULT_COLOR.get());
    }

    public static ResourceLocation icon(TrackedDeposit deposit) {
        if (deposit.depleted()) {
            return ImmersiveDepositScanner.id("textures/journeymap/depleted.png");
        }
        return deposit.key().source() == DepositSource.IMMERSIVE_ENGINEERING
                ? ImmersiveDepositScanner.id("textures/journeymap/ie_mineral.png")
                : ImmersiveDepositScanner.id("textures/journeymap/ip_reservoir.png");
    }

    private static int stableColor(TrackedDeposit deposit, int fallback) {
        int hash = deposit.key().depositId().hashCode();
        int mixed = hash ^ (hash >>> 16);
        int color = mixed & 0xFFFFFF;
        return color == 0 ? fallback : color;
    }
}

