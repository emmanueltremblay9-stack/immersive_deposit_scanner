package com.oblixorprime.immersivedepositscanner.network.payload;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FullSyncStartPayload(UUID syncId, int expectedDeposits) implements CustomPacketPayload {
    public static final int MAX_EXPECTED_DEPOSITS = 100_000;
    public static final Type<FullSyncStartPayload> TYPE = new Type<>(ImmersiveDepositScanner.id("full_sync_start"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FullSyncStartPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FullSyncStartPayload decode(RegistryFriendlyByteBuf buffer) {
            return new FullSyncStartPayload(buffer.readUUID(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FullSyncStartPayload payload) {
            buffer.writeUUID(payload.syncId());
            buffer.writeVarInt(payload.expectedDeposits());
        }
    };

    public FullSyncStartPayload {
        Objects.requireNonNull(syncId, "syncId");
        if (expectedDeposits < 0 || expectedDeposits > MAX_EXPECTED_DEPOSITS) {
            throw new IllegalArgumentException("Invalid expected deposit count: " + expectedDeposits);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
