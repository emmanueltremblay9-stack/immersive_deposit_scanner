package com.oblixorprime.immersivedepositscanner.integration.ip;

import blusunrize.immersiveengineering.common.items.CoresampleItem;
import blusunrize.immersiveengineering.common.register.IEDataComponents;
import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.data.DepositKind;
import com.oblixorprime.immersivedepositscanner.data.DepositSource;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import com.oblixorprime.immersivedepositscanner.data.TrackedDepositKey;
import flaxbeard.immersivepetroleum.api.reservoir.Reservoir;
import flaxbeard.immersivepetroleum.api.reservoir.ReservoirHandler;
import flaxbeard.immersivepetroleum.common.util.survey.ISurveyInfo;
import flaxbeard.immersivepetroleum.common.util.survey.ReservoirInfo;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class IPReservoirReader {
    public List<IPReservoirDiscovery> read(ServerLevel level, ServerPlayer player, ItemStack stack) {
        Map<TrackedDepositKey, IPReservoirDiscovery> discoveries = new LinkedHashMap<>();
        long now = Instant.now().toEpochMilli();

        ISurveyInfo surveyInfo = ISurveyInfo.from(stack);
        if (surveyInfo instanceof ReservoirInfo reservoirInfo && isUsableReservoirInfo(reservoirInfo)) {
            IPReservoirDiscovery discovery = fromReservoirInfo(level, player, now, reservoirInfo);
            discoveries.put(discovery.deposit().key(), discovery);
        }

        CoresampleItem.ItemData coreData = stack.get(IEDataComponents.CORESAMPLE);
        if (coreData != null && !coreData.position().equals(CoresampleItem.SamplePosition.NONE)) {
            CoresampleItem.SamplePosition sample = coreData.position();
            BlockPos samplePos = new BlockPos(sample.x(), player.blockPosition().getY(), sample.z());
            ServerLevel sampleLevel = player.server.getLevel(sample.dimension());
            if (sampleLevel != null) {
                Reservoir reservoir = ReservoirHandler.getReservoir(sampleLevel, samplePos);
                if (reservoir != null) {
                    IPReservoirDiscovery discovery = fromReservoir(sample.dimension(), player, now, samplePos, reservoir);
                    discoveries.put(discovery.deposit().key(), discovery);
                }
            }
        }

        return List.copyOf(discoveries.values());
    }

    static boolean isUsableReservoirInfo(ReservoirInfo info) {
        return info != null && info.fluidStack() != null && !info.fluidStack().isEmpty();
    }

    private static IPReservoirDiscovery fromReservoirInfo(ServerLevel level, ServerPlayer player, long now, ReservoirInfo info) {
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(info.getFluid());
        if (fluidId == null) {
            fluidId = ImmersiveDepositScanner.id("unknown_fluid");
        }
        int chunkX = Math.floorDiv(info.x(), 16);
        int chunkZ = Math.floorDiv(info.z(), 16);
        int percentage = Math.max(0, Byte.toUnsignedInt(info.percentage()));
        BlockPos samplePos = new BlockPos(info.x(), player.blockPosition().getY(), info.z());
        TrackedDeposit deposit = new TrackedDeposit(
                new TrackedDepositKey(level.dimension(), chunkX, chunkZ, DepositSource.IMMERSIVE_PETROLEUM, fluidId),
                DepositKind.FLUID_RESERVOIR,
                humanName(fluidId),
                Optional.of(fluidId),
                Optional.of(samplePos),
                player.getUUID(),
                now,
                now,
                info.amount() >= 0 ? OptionalLong.of(info.amount()) : OptionalLong.empty(),
                OptionalLong.empty(),
                OptionalDouble.of(Math.min(1.0D, percentage / 100.0D)),
                info.amount() <= 0 || percentage <= 0
        );
        return new IPReservoirDiscovery(deposit);
    }

    private static IPReservoirDiscovery fromReservoir(ResourceKey<Level> dimension, ServerPlayer player, long now, BlockPos samplePos, Reservoir reservoir) {
        ResourceLocation typeId = reservoir.getType().id();
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(reservoir.getFluid());
        ResourceLocation resourceId = fluidId == null ? typeId : fluidId;
        long capacity = reservoir.getCapacity();
        OptionalDouble percentage = capacity > 0
                ? OptionalDouble.of(Math.max(0.0D, Math.min(1.0D, reservoir.getAmount() / (double) capacity)))
                : OptionalDouble.empty();
        TrackedDeposit deposit = new TrackedDeposit(
                new TrackedDepositKey(dimension, Math.floorDiv(samplePos.getX(), 16), Math.floorDiv(samplePos.getZ(), 16),
                        DepositSource.IMMERSIVE_PETROLEUM, resourceId),
                DepositKind.FLUID_RESERVOIR,
                humanName(typeId),
                Optional.of(resourceId),
                Optional.of(samplePos),
                player.getUUID(),
                now,
                now,
                OptionalLong.of(reservoir.getAmount()),
                capacity > 0 ? OptionalLong.of(capacity) : OptionalLong.empty(),
                percentage,
                reservoir.isEmpty()
        );
        return new IPReservoirDiscovery(deposit);
    }

    private static String humanName(ResourceLocation id) {
        String path = id.getPath().replace('_', ' ').replace('-', ' ');
        StringBuilder builder = new StringBuilder(path.length());
        boolean upperNext = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (Character.isWhitespace(c)) {
                upperNext = true;
                builder.append(c);
            } else if (upperNext) {
                builder.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
