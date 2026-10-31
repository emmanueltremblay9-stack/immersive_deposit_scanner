package com.oblixorprime.immersivedepositscanner.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;
import java.util.Optional;

public enum DepositSource {
    IMMERSIVE_ENGINEERING("immersive_engineering"),
    IMMERSIVE_PETROLEUM("immersive_petroleum");

    public static final Codec<DepositSource> CODEC = Codec.STRING.comapFlatMap(DepositSource::read, DepositSource::serializedName);

    private final String serializedName;

    DepositSource(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static DepositSource fromSerializedName(String value) {
        return bySerializedName(value).orElseThrow(() -> new IllegalArgumentException("Unknown deposit source: " + normalize(value)));
    }

    public static Optional<DepositSource> bySerializedName(String value) {
        String normalized = normalize(value);
        for (DepositSource source : values()) {
            if (source.serializedName.equals(normalized) || source.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(source);
            }
        }
        return Optional.empty();
    }

    private static DataResult<DepositSource> read(String value) {
        return bySerializedName(value)
                .map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "Unknown deposit source: " + normalize(value)));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
