package com.oblixorprime.immersivedepositscanner.network.payload;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DepositUpsertPayload(TrackedDeposit deposit) implements CustomPacketPayload {
    public static final Type<DepositUpsertPayload> TYPE = new Type<>(ImmersiveDepositScanner.id("deposit_upsert"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DepositUpsertPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public DepositUpsertPayload decode(RegistryFriendlyByteBuf buffer) {
            return new DepositUpsertPayload(TrackedDeposit.STREAM_CODEC.decode(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, DepositUpsertPayload payload) {
            TrackedDeposit.STREAM_CODEC.encode(buffer, payload.deposit);
        }
    };

    public DepositUpsertPayload {
        Objects.requireNonNull(deposit, "deposit");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
