package com.oblixorprime.immersivedepositscanner.integration.ie;

import blusunrize.immersiveengineering.api.excavator.ExcavatorHandler;
import blusunrize.immersiveengineering.common.items.CoresampleItem;
import blusunrize.immersiveengineering.common.register.IEDataComponents;
import com.oblixorprime.immersivedepositscanner.data.DepositKind;
import com.oblixorprime.immersivedepositscanner.data.DepositSource;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import com.oblixorprime.immersivedepositscanner.data.TrackedDepositKey;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class IECoreSampleReader {
    public List<IEMineralDiscovery> read(ServerLevel level, ServerPlayer player, ItemStack stack) {
        if (!ImmersiveEngineeringIntegration.isCoreSample(stack)) {
            return List.of();
        }

        CoresampleItem.ItemData data = stack.get(IEDataComponents.CORESAMPLE);
        if (data == null || data.veins().isEmpty() || data.position().equals(CoresampleItem.SamplePosition.NONE)) {
            return List.of();
        }

        CoresampleItem.SamplePosition samplePosition = data.position();
        int chunkX = Math.floorDiv(samplePosition.x(), 16);
        int chunkZ = Math.floorDiv(samplePosition.z(), 16);
        int y = player.blockPosition().getY();
        long now = Instant.now().toEpochMilli();
        BlockPos position = new BlockPos(samplePosition.x(), y, samplePosition.z());

        return data.veins().stream()
                .map(vein -> toDiscovery(samplePosition, chunkX, chunkZ, position, player, now, vein))
                .toList();
    }

    private static IEMineralDiscovery toDiscovery(
            CoresampleItem.SamplePosition samplePosition,
            int chunkX,
            int chunkZ,
            BlockPos position,
            ServerPlayer player,
            long now,
            CoresampleItem.VeinSample vein
    ) {
        ResourceLocation mineral = vein.mineral();
        int maximumYield = ExcavatorHandler.mineralVeinYield;
        OptionalLong currentAmount = currentYield(vein.depletion(), maximumYield);
        OptionalLong maximumAmount = maximumYield(maximumYield);
        OptionalDouble percentageRemaining = remainingPercentage(vein.depletion(), maximumYield);
        double saturation = clamp01(vein.saturation());
        TrackedDepositKey key = new TrackedDepositKey(
                samplePosition.dimension(),
                chunkX,
                chunkZ,
                DepositSource.IMMERSIVE_ENGINEERING,
                mineral
        );
        TrackedDeposit deposit = new TrackedDeposit(
                key,
                DepositKind.MINERAL,
                humanName(mineral),
                Optional.of(mineral),
                Optional.of(position),
                player.getUUID(),
                now,
                now,
                currentAmount,
                maximumAmount,
                percentageRemaining,
                isDepleted(vein.depletion(), maximumYield, saturation)
        );
        return new IEMineralDiscovery(deposit);
    }

    static OptionalLong currentYield(int depletion, int maximumYield) {
        if (depletion < 0 || maximumYield <= 0) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Math.max(0L, (long) maximumYield - depletion));
    }

    static OptionalLong maximumYield(int maximumYield) {
        return maximumYield > 0 ? OptionalLong.of(maximumYield) : OptionalLong.empty();
    }

    static OptionalDouble remainingPercentage(int depletion, int maximumYield) {
        OptionalLong currentYield = currentYield(depletion, maximumYield);
        if (currentYield.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(currentYield.getAsLong() / (double) maximumYield);
    }

    static boolean isDepleted(int depletion, int maximumYield, double saturation) {
        OptionalLong currentYield = currentYield(depletion, maximumYield);
        return currentYield.isPresent() ? currentYield.getAsLong() <= 0L : clamp01(saturation) <= 0.0D;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
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
