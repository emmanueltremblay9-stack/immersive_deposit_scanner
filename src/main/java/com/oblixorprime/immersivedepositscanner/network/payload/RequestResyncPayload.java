package com.oblixorprime.immersivedepositscanner.network.payload;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestResyncPayload() implements CustomPacketPayload {
    public static final Type<RequestResyncPayload> TYPE = new Type<>(ImmersiveDepositScanner.id("request_resync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestResyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RequestResyncPayload decode(RegistryFriendlyByteBuf buffer) {
            return new RequestResyncPayload();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RequestResyncPayload payload) {
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

