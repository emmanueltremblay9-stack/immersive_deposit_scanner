package com.oblixorprime.immersivedepositscanner.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.EnumValue<SharingMode> SHARING_MODE;
    public static final ModConfigSpec.BooleanValue TRACK_IMMERSIVE_ENGINEERING;
    public static final ModConfigSpec.BooleanValue TRACK_IMMERSIVE_PETROLEUM;
    public static final ModConfigSpec.BooleanValue TRACK_DEPLETED_RESERVOIRS;
    public static final ModConfigSpec.BooleanValue ALLOW_PLAYER_REMOVE_OWN_ENTRIES;
    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("server");
        SHARING_MODE = builder.comment("PERSONAL sends each player only their discoveries. SERVER sends all discoveries to everyone.")
                .defineEnum("sharingMode", SharingMode.PERSONAL);
        TRACK_IMMERSIVE_ENGINEERING = builder.define("trackImmersiveEngineering", true);
        TRACK_IMMERSIVE_PETROLEUM = builder.define("trackImmersivePetroleum", true);
        TRACK_DEPLETED_RESERVOIRS = builder.define("trackDepletedReservoirs", true);
        ALLOW_PLAYER_REMOVE_OWN_ENTRIES = builder.define("allowPlayerRemoveOwnEntries", true);
        DEBUG_LOGGING = builder.define("debugLogging", false);
        builder.pop();
        SPEC = builder.build();
    }

    private ServerConfig() {
    }
}

