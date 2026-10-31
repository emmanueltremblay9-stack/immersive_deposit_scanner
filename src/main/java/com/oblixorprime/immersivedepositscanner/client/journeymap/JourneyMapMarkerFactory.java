package com.oblixorprime.immersivedepositscanner.client.journeymap;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.config.ClientConfig;
import com.oblixorprime.immersivedepositscanner.data.DepositSource;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class JourneyMapMarkerFactory {
    private JourneyMapMarkerFactory() {
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

    public static Waypoint createWaypoint(TrackedDeposit deposit) {
        Waypoint waypoint = WaypointFactory.createClientWaypoint(waypointLabel(deposit), position(deposit), groupName(deposit), deposit.key().dimension(), true);
        waypoint.setPersistent(false);
        waypoint.setColor(color(deposit));
        waypoint.setIconColor(color(deposit));
        ResourceLocation icon = icon(deposit);
        if (icon != null) {
            waypoint.setIconResourceLoctaion(icon);
        }
        waypoint.setIconTextureSize(16, 16);
        waypoint.setCustomData("ids:key", deposit.key().stableId());
        return waypoint;
    }

    public static MarkerOverlay createMarker(TrackedDeposit deposit) {
        var markerImage = JourneyMapSubjectVisuals.markerImage(deposit);
        if (markerImage == null) {
            return null;
        }
        MarkerOverlay marker = new MarkerOverlay(
                ImmersiveDepositScanner.MOD_ID,
                position(deposit),
                markerImage
        );
        marker.setDimension(deposit.key().dimension());
        marker.setOverlayGroupName(groupName(deposit));
        String label = label(deposit);
        marker.setTitle(label);
        marker.setLabel(markerMapLabel(label));
        marker.setDisplayOrder(100);
        return marker;
    }

    public static String label(TrackedDeposit deposit) {
        return label(
                deposit,
                getBooleanSetting(ClientConfig.USE_RESOURCE_SUBJECT_VISUALS, "useResourceSubjectVisuals", true)
                        && getBooleanSetting(ClientConfig.SHOW_JOURNEYMAP_SUBJECT_IDS_IN_LABEL, "showJourneyMapSubjectIdsInLabel", false),
                getBooleanSetting(ClientConfig.SHOW_REMAINING_PERCENTAGE, "showRemainingPercentage", true),
                getBooleanSetting(ClientConfig.SHOW_AMOUNTS, "showAmounts", false)
        );
    }

    static String label(TrackedDeposit deposit, boolean showResourceSubject, boolean showRemainingPercentage, boolean showAmounts) {
        StringBuilder label = new StringBuilder(sourceTag(deposit))
                .append(" ")
                .append(deposit.displayName())
                .append(" [")
                .append(deposit.key().chunkX())
                .append(", ")
                .append(deposit.key().chunkZ())
                .append("]");
        if (showResourceSubject) {
            label.append(" subject=").append(JourneyMapSubjectVisuals.subjectLabel(deposit));
        }
        deposit.percentageRemaining().ifPresent(value -> {
            if (showRemainingPercentage) {
                label.append(" ").append(Math.round(value * 100.0D)).append("%");
            }
        });
        if (showAmounts && deposit.currentAmount().isPresent()) {
            label.append(" ").append(deposit.currentAmount().getAsLong());
            deposit.maximumAmount().ifPresent(max -> label.append("/").append(max));
        }
        if (deposit.depleted()) {
            label.append(" depleted");
        }
        return label.toString();
    }

    static String markerMapLabel(
            TrackedDeposit deposit,
            boolean showMarkerLabels,
            boolean showResourceSubject,
            boolean showRemainingPercentage,
            boolean showAmounts
    ) {
        return showMarkerLabels ? label(deposit, showResourceSubject, showRemainingPercentage, showAmounts) : "";
    }

    static String waypointLabel(TrackedDeposit deposit, boolean showWaypointLabels) {
        return showWaypointLabels ? sourceTag(deposit) + " " + deposit.displayName() : sourceTag(deposit);
    }

    public static BlockPos position(TrackedDeposit deposit) {
        return deposit.samplePosition().orElseGet(() -> new BlockPos(
                deposit.key().chunkX() * 16 + 8,
                getIntSetting(ClientConfig.WAYPOINT_Y_FALLBACK, "waypointYFallback", 64),
                deposit.key().chunkZ() * 16 + 8
        ));
    }

    public static String groupName(TrackedDeposit deposit) {
        return deposit.key().source() == DepositSource.IMMERSIVE_ENGINEERING
                ? getStringSetting(
                        ClientConfig.IE_GROUP_NAME,
                        "ieGroupName",
                        "Immersive Deposit Scanner / Immersive Engineering"
                )
                : getStringSetting(
                        ClientConfig.IP_GROUP_NAME,
                        "ipGroupName",
                        "Immersive Deposit Scanner / Immersive Petroleum"
                );
    }

    public static int color(TrackedDeposit deposit) {
        return JourneyMapSubjectVisuals.color(deposit);
    }

    public static ResourceLocation icon(TrackedDeposit deposit) {
        return JourneyMapSubjectVisuals.icon(deposit);
    }

    private static String markerMapLabel(String label) {
        return getBooleanSetting(
                ClientConfig.SHOW_JOURNEYMAP_MARKER_LABELS,
                "showJourneyMapMarkerLabels",
                true
        ) ? label : "";
    }

    private static String waypointLabel(TrackedDeposit deposit) {
        return waypointLabel(
                deposit,
                getBooleanSetting(
                        ClientConfig.SHOW_JOURNEYMAP_WAYPOINT_LABELS,
                        "showJourneyMapWaypointLabels",
                        false
                )
        );
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

    private static String getStringSetting(ModConfigSpec.ConfigValue<String> setting, String settingName, String fallback) {
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

    private static String sourceTag(TrackedDeposit deposit) {
        return deposit.key().source() == DepositSource.IMMERSIVE_ENGINEERING ? "[IE]" : "[IP]";
    }
}
