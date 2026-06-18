package com.oblixorprime.immersivedepositscanner.integration.ie;

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
        TrackedDepositKey key = new TrackedDepositKey(
                samplePosition.dimension(),
                chunkX,
                chunkZ,
                DepositSource.IMMERSIVE_ENGINEERING,
                mineral
        );
        double saturation = clamp01(vein.saturation());
        TrackedDeposit deposit = new TrackedDeposit(
                key,
                DepositKind.MINERAL,
                humanName(mineral),
                Optional.of(mineral),
                Optional.of(position),
                player.getUUID(),
                now,
                now,
                vein.depletion() >= 0 ? OptionalLong.of(vein.depletion()) : OptionalLong.empty(),
                OptionalLong.empty(),
                OptionalDouble.of(saturation),
                saturation <= 0.0D
        );
        return new IEMineralDiscovery(deposit);
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

