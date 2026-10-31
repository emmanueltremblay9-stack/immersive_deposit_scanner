package com.oblixorprime.immersivedepositscanner.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;
import java.util.Optional;

public enum DepositKind {
    MINERAL("mineral"),
    FLUID_RESERVOIR("fluid_reservoir");

    public static final Codec<DepositKind> CODEC = Codec.STRING.comapFlatMap(DepositKind::read, DepositKind::serializedName);

    private final String serializedName;

    DepositKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static DepositKind fromSerializedName(String value) {
        return bySerializedName(value).orElseThrow(() -> new IllegalArgumentException("Unknown deposit kind: " + normalize(value)));
    }

    public static Optional<DepositKind> bySerializedName(String value) {
        String normalized = normalize(value);
        for (DepositKind kind : values()) {
            if (kind.serializedName.equals(normalized) || kind.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }

    private static DataResult<DepositKind> read(String value) {
        return bySerializedName(value)
                .map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "Unknown deposit kind: " + normalize(value)));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
