package com.oblixorprime.immersivedepositscanner.network.payload;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FullSyncBatchPayload(UUID syncId, List<TrackedDeposit> deposits) implements CustomPacketPayload {
    public static final int MAX_BATCH_SIZE = 64;
    public static final Type<FullSyncBatchPayload> TYPE = new Type<>(ImmersiveDepositScanner.id("full_sync_batch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FullSyncBatchPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FullSyncBatchPayload decode(RegistryFriendlyByteBuf buffer) {
            UUID syncId = buffer.readUUID();
            int count = buffer.readVarInt();
            if (count < 0 || count > MAX_BATCH_SIZE) {
                throw new IllegalArgumentException("Invalid deposit sync batch size: " + count);
            }
            List<TrackedDeposit> deposits = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                deposits.add(TrackedDeposit.STREAM_CODEC.decode(buffer));
            }
            return new FullSyncBatchPayload(syncId, List.copyOf(deposits));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FullSyncBatchPayload payload) {
            if (payload.deposits.size() > MAX_BATCH_SIZE) {
                throw new IllegalArgumentException("Deposit sync batch exceeds " + MAX_BATCH_SIZE);
            }
            buffer.writeUUID(payload.syncId);
            buffer.writeVarInt(payload.deposits.size());
            payload.deposits.forEach(deposit -> TrackedDeposit.STREAM_CODEC.encode(buffer, deposit));
        }
    };

    public FullSyncBatchPayload {
        Objects.requireNonNull(syncId, "syncId");
        Objects.requireNonNull(deposits, "deposits");
        if (deposits.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Deposit sync batch exceeds " + MAX_BATCH_SIZE);
        }
        deposits = List.copyOf(deposits);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
