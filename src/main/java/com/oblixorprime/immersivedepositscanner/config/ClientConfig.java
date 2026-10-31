package com.oblixorprime.immersivedepositscanner.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_JOURNEYMAP_MARKERS;
    public static final ModConfigSpec.BooleanValue ENABLE_JOURNEYMAP_CHUNK_OVERLAYS;
    public static final ModConfigSpec.BooleanValue ENABLE_WAYPOINTS;
    public static final ModConfigSpec.BooleanValue ENABLE_CHUNK_OVERLAYS;
    public static final ModConfigSpec.BooleanValue SHOW_IMMERSIVE_ENGINEERING;
    public static final ModConfigSpec.BooleanValue SHOW_IMMERSIVE_PETROLEUM;
    public static final ModConfigSpec.BooleanValue USE_RESOURCE_SUBJECT_VISUALS;
    public static final ModConfigSpec.BooleanValue USE_RESOURCE_SUBJECT_ICONS;
    public static final ModConfigSpec.BooleanValue USE_NATIVE_IMAGE_SUBJECT_ICONS;
    public static final ModConfigSpec.BooleanValue USE_EXTERNAL_SUBJECT_TEXTURE_REFERENCES;
    public static final ModConfigSpec.BooleanValue FALLBACK_TO_GENERIC_ICONS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTERNAL_SUBJECT_TEXTURE_MAPPINGS;
    public static final ModConfigSpec.BooleanValue SHOW_JOURNEYMAP_MARKER_LABELS;
    public static final ModConfigSpec.BooleanValue SHOW_JOURNEYMAP_OVERLAY_LABELS;
    public static final ModConfigSpec.BooleanValue SHOW_JOURNEYMAP_WAYPOINT_LABELS;
    public static final ModConfigSpec.BooleanValue SHOW_JOURNEYMAP_SUBJECT_IDS_IN_LABEL;
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
        ENABLE_JOURNEYMAP_MARKERS = builder.comment("Emergency switch for IDS JourneyMap marker overlays and waypoints.")
                .define("enableJourneyMapMarkers", true);
        ENABLE_JOURNEYMAP_CHUNK_OVERLAYS = builder.comment("Emergency switch for IDS JourneyMap chunk polygon overlays.")
                .define("enableJourneyMapChunkOverlays", true);
        ENABLE_WAYPOINTS = builder.comment("Create JourneyMap waypoints in addition to marker overlays. Disabled by default to avoid duplicate map labels.")
                .define("enableWaypoints", false);
        ENABLE_CHUNK_OVERLAYS = builder.define("enableChunkOverlays", true);
        SHOW_IMMERSIVE_ENGINEERING = builder.define("showImmersiveEngineering", true);
        SHOW_IMMERSIVE_PETROLEUM = builder.define("showImmersivePetroleum", true);
        USE_RESOURCE_SUBJECT_VISUALS = builder.comment("Use stable subject-derived colors and labels for JourneyMap displays when true.")
                .define("useResourceSubjectVisuals", true);
        USE_RESOURCE_SUBJECT_ICONS = builder.comment("Legacy compatibility gate for resource-subject JourneyMap coloring.")
                .define("useResourceSubjectIcons", true);
        USE_NATIVE_IMAGE_SUBJECT_ICONS = builder.comment("Reserved for a future safe NativeImage icon implementation. Disabled by default.")
                .define("useNativeImageSubjectIcons", false);
        USE_EXTERNAL_SUBJECT_TEXTURE_REFERENCES = builder.comment("Experimental: use configured external ResourceLocation textures for JourneyMap subject marker icons after resource-manager verification.")
                .define("useExternalSubjectTextureReferences", false);
        FALLBACK_TO_GENERIC_ICONS = builder.comment("Use the built-in IE/IP/depleted textures when no subject texture icon is available.")
                .define("fallbackToGenericIcons", true);
        EXTERNAL_SUBJECT_TEXTURE_MAPPINGS = builder.comment(
                        "Experimental subject-to-texture mappings. Format: subject_resource_id=texture_resource_location.",
                        "IDS references these runtime textures directly and does not copy or bundle external assets."
                )
                .defineListAllowEmpty("externalSubjectTextureMappings", List.of(
                        "minecraft:iron=minecraft:textures/item/iron_ingot.png",
                        "minecraft:copper=minecraft:textures/item/copper_ingot.png",
                        "minecraft:gold=minecraft:textures/item/gold_ingot.png",
                        "minecraft:coal=minecraft:textures/item/coal.png",
                        "minecraft:diamond=minecraft:textures/item/diamond.png",
                        "minecraft:emerald=minecraft:textures/item/emerald.png",
                        "minecraft:redstone=minecraft:textures/item/redstone.png",
                        "minecraft:lapis=minecraft:textures/item/lapis_lazuli.png",
                        "minecraft:quartz=minecraft:textures/item/quartz.png"
                ), value -> value instanceof String);
        SHOW_JOURNEYMAP_MARKER_LABELS = builder.comment("Show concise labels on JourneyMap marker overlays.")
                .define("showJourneyMapMarkerLabels", true);
        SHOW_JOURNEYMAP_OVERLAY_LABELS = builder.comment("Show labels on JourneyMap chunk polygon overlays. Disabled by default to avoid duplicated marker text.")
                .define("showJourneyMapOverlayLabels", false);
        SHOW_JOURNEYMAP_WAYPOINT_LABELS = builder.comment("Use deposit names for JourneyMap waypoints when waypoints are enabled. Disabled by default for very short waypoint labels.")
                .define("showJourneyMapWaypointLabels", false);
        SHOW_JOURNEYMAP_SUBJECT_IDS_IN_LABEL = builder.comment("Debug only: append the scanned resource id to JourneyMap marker and overlay labels when true.")
                .define("showJourneyMapSubjectIdsInLabel", false);
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
