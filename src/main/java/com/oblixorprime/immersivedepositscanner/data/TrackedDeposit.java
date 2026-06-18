package com.oblixorprime.immersivedepositscanner.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record TrackedDeposit(
        TrackedDepositKey key,
        DepositKind kind,
        String displayName,
        Optional<ResourceLocation> resourceId,
        Optional<BlockPos> samplePosition,
        UUID discoveredBy,
        long discoveredAt,
        long updatedAt,
        OptionalLong currentAmount,
        OptionalLong maximumAmount,
        OptionalDouble percentageRemaining,
        boolean depleted
) {
    public static final int MAX_DISPLAY_NAME_LENGTH = 128;

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(value -> {
        try {
            return DataResult.success(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Invalid UUID: " + value);
        }
    }, UUID::toString);

    public static final Codec<TrackedDeposit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TrackedDepositKey.CODEC.fieldOf("key").forGetter(TrackedDeposit::key),
            DepositKind.CODEC.fieldOf("kind").forGetter(TrackedDeposit::kind),
            Codec.STRING.fieldOf("displayName").forGetter(TrackedDeposit::displayName),
            ResourceLocation.CODEC.optionalFieldOf("resourceId").forGetter(TrackedDeposit::resourceId),
            BlockPos.CODEC.optionalFieldOf("samplePosition").forGetter(TrackedDeposit::samplePosition),
            UUID_CODEC.fieldOf("discoveredBy").forGetter(TrackedDeposit::discoveredBy),
            Codec.LONG.fieldOf("discoveredAt").forGetter(TrackedDeposit::discoveredAt),
            Codec.LONG.fieldOf("updatedAt").forGetter(TrackedDeposit::updatedAt),
            Codec.LONG.optionalFieldOf("currentAmount").forGetter(value -> toOptional(value.currentAmount())),
            Codec.LONG.optionalFieldOf("maximumAmount").forGetter(value -> toOptional(value.maximumAmount())),
            Codec.DOUBLE.optionalFieldOf("percentageRemaining").forGetter(value -> toOptional(value.percentageRemaining())),
            Codec.BOOL.fieldOf("depleted").forGetter(TrackedDeposit::depleted)
    ).apply(instance, (key, kind, displayName, resourceId, samplePosition, discoveredBy, discoveredAt, updatedAt,
                      currentAmount, maximumAmount, percentageRemaining, depleted) -> new TrackedDeposit(
            key,
            kind,
            displayName,
            resourceId,
            samplePosition,
            discoveredBy,
            discoveredAt,
            updatedAt,
            optionalLong(currentAmount),
            optionalLong(maximumAmount),
            optionalDouble(percentageRemaining),
            depleted
    )));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrackedDeposit> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TrackedDeposit decode(RegistryFriendlyByteBuf buffer) {
            TrackedDepositKey key = TrackedDepositKey.STREAM_CODEC.decode(buffer);
            DepositKind kind = buffer.readEnum(DepositKind.class);
            String displayName = buffer.readUtf(MAX_DISPLAY_NAME_LENGTH);
            Optional<ResourceLocation> resourceId = readOptionalResourceLocation(buffer);
            Optional<BlockPos> samplePosition = readOptionalBlockPos(buffer);
            UUID discoveredBy = buffer.readUUID();
            long discoveredAt = buffer.readLong();
            long updatedAt = buffer.readLong();
            OptionalLong currentAmount = readOptionalLong(buffer);
            OptionalLong maximumAmount = readOptionalLong(buffer);
            OptionalDouble percentageRemaining = readOptionalDouble(buffer);
            boolean depleted = buffer.readBoolean();
            return new TrackedDeposit(key, kind, displayName, resourceId, samplePosition, discoveredBy, discoveredAt, updatedAt,
                    currentAmount, maximumAmount, percentageRemaining, depleted);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, TrackedDeposit deposit) {
            TrackedDepositKey.STREAM_CODEC.encode(buffer, deposit.key);
            buffer.writeEnum(deposit.kind);
            buffer.writeUtf(deposit.displayName, MAX_DISPLAY_NAME_LENGTH);
            writeOptionalResourceLocation(buffer, deposit.resourceId);
            writeOptionalBlockPos(buffer, deposit.samplePosition);
            buffer.writeUUID(deposit.discoveredBy);
            buffer.writeLong(deposit.discoveredAt);
            buffer.writeLong(deposit.updatedAt);
            writeOptionalLong(buffer, deposit.currentAmount);
            writeOptionalLong(buffer, deposit.maximumAmount);
            writeOptionalDouble(buffer, deposit.percentageRemaining);
            buffer.writeBoolean(deposit.depleted);
        }
    };

    public TrackedDeposit {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(kind, "kind");
        displayName = normalizeDisplayName(displayName);
        resourceId = resourceId == null ? Optional.empty() : resourceId;
        samplePosition = samplePosition == null ? Optional.empty() : samplePosition;
        Objects.requireNonNull(discoveredBy, "discoveredBy");
        currentAmount = currentAmount == null ? OptionalLong.empty() : currentAmount;
        maximumAmount = maximumAmount == null ? OptionalLong.empty() : maximumAmount;
        percentageRemaining = percentageRemaining == null ? OptionalDouble.empty() : percentageRemaining;
    }

    public TrackedDeposit preservingFirstDiscovery(TrackedDeposit existing) {
        return new TrackedDeposit(
                key,
                kind,
                displayName,
                resourceId,
                samplePosition,
                existing.discoveredBy,
                existing.discoveredAt,
                Math.max(updatedAt, existing.updatedAt),
                currentAmount,
                maximumAmount,
                percentageRemaining,
                depleted
        );
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("key", key.toTag());
        tag.putString("kind", kind.serializedName());
        tag.putString("displayName", displayName);
        resourceId.ifPresent(value -> tag.putString("resourceId", value.toString()));
        samplePosition.ifPresent(position -> {
            tag.putInt("sampleX", position.getX());
            tag.putInt("sampleY", position.getY());
            tag.putInt("sampleZ", position.getZ());
        });
        tag.putUUID("discoveredBy", discoveredBy);
        tag.putLong("discoveredAt", discoveredAt);
        tag.putLong("updatedAt", updatedAt);
        currentAmount.ifPresent(value -> tag.putLong("currentAmount", value));
        maximumAmount.ifPresent(value -> tag.putLong("maximumAmount", value));
        percentageRemaining.ifPresent(value -> tag.putDouble("percentageRemaining", value));
        tag.putBoolean("depleted", depleted);
        return tag;
    }

    public static TrackedDeposit fromTag(CompoundTag tag) {
        Optional<ResourceLocation> resourceId = tag.contains("resourceId")
                ? Optional.of(ResourceLocation.parse(tag.getString("resourceId")))
                : Optional.empty();
        Optional<BlockPos> samplePosition = tag.contains("sampleX")
                ? Optional.of(new BlockPos(tag.getInt("sampleX"), tag.getInt("sampleY"), tag.getInt("sampleZ")))
                : Optional.empty();
        OptionalLong currentAmount = tag.contains("currentAmount") ? OptionalLong.of(tag.getLong("currentAmount")) : OptionalLong.empty();
        OptionalLong maximumAmount = tag.contains("maximumAmount") ? OptionalLong.of(tag.getLong("maximumAmount")) : OptionalLong.empty();
        OptionalDouble percentageRemaining = tag.contains("percentageRemaining") ? OptionalDouble.of(tag.getDouble("percentageRemaining")) : OptionalDouble.empty();
        return new TrackedDeposit(
                TrackedDepositKey.fromTag(tag.getCompound("key")),
                DepositKind.fromSerializedName(tag.getString("kind")),
                tag.getString("displayName"),
                resourceId,
                samplePosition,
                tag.getUUID("discoveredBy"),
                tag.getLong("discoveredAt"),
                tag.getLong("updatedAt"),
                currentAmount,
                maximumAmount,
                percentageRemaining,
                tag.getBoolean("depleted")
        );
    }

    public static String normalizeDisplayName(String value) {
        String normalized = value == null || value.isBlank() ? "Unknown deposit" : value.trim();
        return normalized.length() <= MAX_DISPLAY_NAME_LENGTH ? normalized : normalized.substring(0, MAX_DISPLAY_NAME_LENGTH);
    }

    private static Optional<Long> toOptional(OptionalLong value) {
        return value.isPresent() ? Optional.of(value.getAsLong()) : Optional.empty();
    }

    private static Optional<Double> toOptional(OptionalDouble value) {
        return value.isPresent() ? Optional.of(value.getAsDouble()) : Optional.empty();
    }

    private static OptionalLong optionalLong(Optional<Long> value) {
        return value.map(OptionalLong::of).orElseGet(OptionalLong::empty);
    }

    private static OptionalDouble optionalDouble(Optional<Double> value) {
        return value.map(OptionalDouble::of).orElseGet(OptionalDouble::empty);
    }

    private static Optional<ResourceLocation> readOptionalResourceLocation(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(buffer.readResourceLocation()) : Optional.empty();
    }

    private static void writeOptionalResourceLocation(RegistryFriendlyByteBuf buffer, Optional<ResourceLocation> value) {
        buffer.writeBoolean(value.isPresent());
        value.ifPresent(buffer::writeResourceLocation);
    }

    private static Optional<BlockPos> readOptionalBlockPos(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(buffer.readBlockPos()) : Optional.empty();
    }

    private static void writeOptionalBlockPos(RegistryFriendlyByteBuf buffer, Optional<BlockPos> value) {
        buffer.writeBoolean(value.isPresent());
        value.ifPresent(buffer::writeBlockPos);
    }

    private static OptionalLong readOptionalLong(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? OptionalLong.of(buffer.readLong()) : OptionalLong.empty();
    }

    private static void writeOptionalLong(RegistryFriendlyByteBuf buffer, OptionalLong value) {
        buffer.writeBoolean(value.isPresent());
        value.ifPresent(buffer::writeLong);
    }

    private static OptionalDouble readOptionalDouble(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? OptionalDouble.of(buffer.readDouble()) : OptionalDouble.empty();
    }

    private static void writeOptionalDouble(RegistryFriendlyByteBuf buffer, OptionalDouble value) {
        buffer.writeBoolean(value.isPresent());
        value.ifPresent(buffer::writeDouble);
    }
}

