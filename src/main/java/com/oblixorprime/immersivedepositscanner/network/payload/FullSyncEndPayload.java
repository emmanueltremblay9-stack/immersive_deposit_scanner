package com.oblixorprime.immersivedepositscanner.network.payload;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FullSyncEndPayload(UUID syncId) implements CustomPacketPayload {
    public static final Type<FullSyncEndPayload> TYPE = new Type<>(ImmersiveDepositScanner.id("full_sync_end"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FullSyncEndPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FullSyncEndPayload decode(RegistryFriendlyByteBuf buffer) {
            return new FullSyncEndPayload(buffer.readUUID());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FullSyncEndPayload payload) {
            buffer.writeUUID(payload.syncId);
        }
    };

    public FullSyncEndPayload {
        Objects.requireNonNull(syncId, "syncId");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
