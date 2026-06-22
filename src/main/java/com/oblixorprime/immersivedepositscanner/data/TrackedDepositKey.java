package com.oblixorprime.immersivedepositscanner.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record TrackedDepositKey(
        ResourceKey<Level> dimension,
        int chunkX,
        int chunkZ,
        DepositSource source,
        ResourceLocation depositId
) {
    public static final Codec<ResourceKey<Level>> DIMENSION_CODEC = ResourceLocation.CODEC.xmap(
            location -> ResourceKey.create(Registries.DIMENSION, location),
            ResourceKey::location
    );

    public static final Codec<TrackedDepositKey> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DIMENSION_CODEC.fieldOf("dimension").forGetter(TrackedDepositKey::dimension),
            Codec.INT.fieldOf("chunkX").forGetter(TrackedDepositKey::chunkX),
            Codec.INT.fieldOf("chunkZ").forGetter(TrackedDepositKey::chunkZ),
            DepositSource.CODEC.fieldOf("source").forGetter(TrackedDepositKey::source),
            ResourceLocation.CODEC.fieldOf("depositId").forGetter(TrackedDepositKey::depositId)
    ).apply(instance, TrackedDepositKey::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrackedDepositKey> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TrackedDepositKey decode(RegistryFriendlyByteBuf buffer) {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation());
            int chunkX = buffer.readVarInt();
            int chunkZ = buffer.readVarInt();
            DepositSource source = buffer.readEnum(DepositSource.class);
            ResourceLocation depositId = buffer.readResourceLocation();
            return new TrackedDepositKey(dimension, chunkX, chunkZ, source, depositId);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, TrackedDepositKey key) {
            buffer.writeResourceLocation(key.dimension.location());
            buffer.writeVarInt(key.chunkX);
            buffer.writeVarInt(key.chunkZ);
            buffer.writeEnum(key.source);
            buffer.writeResourceLocation(key.depositId);
        }
    };

    public TrackedDepositKey {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(depositId, "depositId");
    }

    public String stableId() {
        String raw = dimension.location() + "|" + chunkX + "|" + chunkZ + "|" + source.serializedName() + "|" + depositId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "ids_" + HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8))).substring(0, 32);
        } catch (NoSuchAlgorithmException exception) {
            return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension.location().toString());
        tag.putInt("chunkX", chunkX);
        tag.putInt("chunkZ", chunkZ);
        tag.putString("source", source.serializedName());
        tag.putString("depositId", depositId.toString());
        return tag;
    }

    public static TrackedDepositKey fromTag(CompoundTag tag) {
        ResourceLocation dimension = ResourceLocation.parse(requireString(tag, "dimension"));
        ResourceLocation depositId = ResourceLocation.parse(requireString(tag, "depositId"));
        return new TrackedDepositKey(
                ResourceKey.create(Registries.DIMENSION, dimension),
                requireInt(tag, "chunkX"),
                requireInt(tag, "chunkZ"),
                requireSource(tag),
                depositId
        );
    }

    private static int requireInt(CompoundTag tag, String fieldName) {
        if (!tag.contains(fieldName, Tag.TAG_INT)) {
            throw new IllegalArgumentException("Missing or invalid deposit key field: " + fieldName);
        }
        return tag.getInt(fieldName);
    }

    private static String requireString(CompoundTag tag, String fieldName) {
        if (!tag.contains(fieldName, Tag.TAG_STRING)) {
            throw new IllegalArgumentException("Missing or invalid deposit key field: " + fieldName);
        }
        return tag.getString(fieldName);
    }

    private static DepositSource requireSource(CompoundTag tag) {
        String normalized = requireString(tag, "source").toLowerCase(Locale.ROOT);
        for (DepositSource source : DepositSource.values()) {
            if (source.serializedName().equals(normalized) || source.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown deposit source: " + normalized);
    }
}

