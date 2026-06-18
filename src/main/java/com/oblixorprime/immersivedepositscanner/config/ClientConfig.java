package com.oblixorprime.immersivedepositscanner.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_WAYPOINTS;
    public static final ModConfigSpec.BooleanValue ENABLE_CHUNK_OVERLAYS;
    public static final ModConfigSpec.BooleanValue SHOW_IMMERSIVE_ENGINEERING;
    public static final ModConfigSpec.BooleanValue SHOW_IMMERSIVE_PETROLEUM;
    public static final ModConfigSpec.BooleanValue SHOW_REMAINING_PERCENTAGE;
    public static final ModConfigSpec.BooleanValue SHOW_AMOUNTS;
    public static final ModConfigSpec.IntValue WAYPOINT_Y_FALLBACK;
    public static final ModConfigSpec.ConfigValue<String> IE_GROUP_NAME;
    public static final ModConfigSpec.ConfigValue<String> IP_GROUP_NAME;
    public static final ModConfigSpec.IntValue IE_DEFAULT_COLOR;
    public static final ModConfigSpec.IntValue IP_DEFAULT_COLOR;
    public static final ModConfigSpec.DoubleValue OVERLAY_FILL_OPACITY;
    public static final ModConfigSpec.DoubleValue OVERLAY_STROKE_OPACITY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("client");
        ENABLE_WAYPOINTS = builder.define("enableWaypoints", true);
        ENABLE_CHUNK_OVERLAYS = builder.define("enableChunkOverlays", true);
        SHOW_IMMERSIVE_ENGINEERING = builder.define("showImmersiveEngineering", true);
        SHOW_IMMERSIVE_PETROLEUM = builder.define("showImmersivePetroleum", true);
        SHOW_REMAINING_PERCENTAGE = builder.define("showRemainingPercentage", true);
        SHOW_AMOUNTS = builder.define("showAmounts", false);
        WAYPOINT_Y_FALLBACK = builder.defineInRange("waypointYFallback", 64, -64, 320);
        IE_GROUP_NAME = builder.define("ieGroupName", "Immersive Deposit Scanner / Immersive Engineering");
        IP_GROUP_NAME = builder.define("ipGroupName", "Immersive Deposit Scanner / Immersive Petroleum");
        IE_DEFAULT_COLOR = builder.comment("Default IE marker color as 0xRRGGBB.")
                .defineInRange("ieDefaultColor", 0x2ED3D3, 0x000000, 0xFFFFFF);
        IP_DEFAULT_COLOR = builder.comment("Default IP marker color as 0xRRGGBB.")
                .defineInRange("ipDefaultColor", 0xF6A83A, 0x000000, 0xFFFFFF);
        OVERLAY_FILL_OPACITY = builder.defineInRange("overlayFillOpacity", 0.20D, 0.0D, 1.0D);
        OVERLAY_STROKE_OPACITY = builder.defineInRange("overlayStrokeOpacity", 0.85D, 0.0D, 1.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}

