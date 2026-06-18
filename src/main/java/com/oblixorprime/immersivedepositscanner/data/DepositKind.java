package com.oblixorprime.immersivedepositscanner.data;

import com.mojang.serialization.Codec;
import java.util.Locale;

public enum DepositKind {
    MINERAL("mineral"),
    FLUID_RESERVOIR("fluid_reservoir");

    public static final Codec<DepositKind> CODEC = Codec.STRING.xmap(DepositKind::fromSerializedName, DepositKind::serializedName);

    private final String serializedName;

    DepositKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static DepositKind fromSerializedName(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        for (DepositKind kind : values()) {
            if (kind.serializedName.equals(normalized) || kind.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return kind;
            }
        }
        return MINERAL;
    }
}

