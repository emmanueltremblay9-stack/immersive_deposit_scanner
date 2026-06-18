package com.oblixorprime.immersivedepositscanner.network.payload;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DepositClearPayload() implements CustomPacketPayload {
    public static final Type<DepositClearPayload> TYPE = new Type<>(ImmersiveDepositScanner.id("deposit_clear"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DepositClearPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public DepositClearPayload decode(RegistryFriendlyByteBuf buffer) {
            return new DepositClearPayload();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, DepositClearPayload payload) {
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

