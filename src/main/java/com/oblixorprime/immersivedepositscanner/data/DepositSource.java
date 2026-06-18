package com.oblixorprime.immersivedepositscanner.data;

import com.mojang.serialization.Codec;
import java.util.Locale;

public enum DepositSource {
    IMMERSIVE_ENGINEERING("immersive_engineering"),
    IMMERSIVE_PETROLEUM("immersive_petroleum");

    public static final Codec<DepositSource> CODEC = Codec.STRING.xmap(DepositSource::fromSerializedName, DepositSource::serializedName);

    private final String serializedName;

    DepositSource(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static DepositSource fromSerializedName(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        for (DepositSource source : values()) {
            if (source.serializedName.equals(normalized) || source.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return source;
            }
        }
        return IMMERSIVE_ENGINEERING;
    }
}

