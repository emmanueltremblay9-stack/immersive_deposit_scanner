package com.oblixorprime.immersivedepositscanner.network.payload;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.data.TrackedDepositKey;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DepositRemovePayload(TrackedDepositKey key) implements CustomPacketPayload {
    public static final Type<DepositRemovePayload> TYPE = new Type<>(ImmersiveDepositScanner.id("deposit_remove"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DepositRemovePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public DepositRemovePayload decode(RegistryFriendlyByteBuf buffer) {
            return new DepositRemovePayload(TrackedDepositKey.STREAM_CODEC.decode(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, DepositRemovePayload payload) {
            TrackedDepositKey.STREAM_CODEC.encode(buffer, payload.key);
        }
    };

    public DepositRemovePayload {
        Objects.requireNonNull(key, "key");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
