package com.oblixorprime.immersivedepositscanner.integration.ie;

import blusunrize.immersiveengineering.api.excavator.ExcavatorHandler;
import blusunrize.immersiveengineering.api.excavator.MineralMix;
import blusunrize.immersiveengineering.api.crafting.StackWithChance;
import blusunrize.immersiveengineering.common.items.CoresampleItem;
import blusunrize.immersiveengineering.common.register.IEDataComponents;
import com.oblixorprime.immersivedepositscanner.data.DepositKind;
import com.oblixorprime.immersivedepositscanner.data.DepositSource;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import com.oblixorprime.immersivedepositscanner.data.TrackedDepositKey;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
        DominantSubject visualSubject = dominantSampleSubject(level, data.veins())
                .orElseGet(() -> fallbackSubject(data.veins().getFirst().mineral()));

        return data.veins().stream()
                .map(vein -> toDiscovery(samplePosition, chunkX, chunkZ, position, player, now, vein, visualSubject))
                .toList();
    }

    private static IEMineralDiscovery toDiscovery(
            CoresampleItem.SamplePosition samplePosition,
            int chunkX,
            int chunkZ,
            BlockPos position,
            ServerPlayer player,
            long now,
            CoresampleItem.VeinSample vein,
            DominantSubject visualSubject
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
                visualSubject.displayName(),
                Optional.of(visualSubject.resourceId()),
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

    private static Optional<DominantSubject> dominantSampleSubject(ServerLevel level, List<CoresampleItem.VeinSample> veins) {
        return veins.stream()
                .max(Comparator.comparingDouble(IECoreSampleReader::sampleShare))
                .map(CoresampleItem.VeinSample::mineral)
                .map(mineral -> subjectForMineral(level, mineral));
    }

    private static DominantSubject subjectForMineral(ServerLevel level, ResourceLocation mineral) {
        try {
            MineralMix mix = MineralMix.RECIPES.getById(level, mineral);
            if (mix == null) {
                return fallbackSubject(mineral);
            }
            return subjectForMineral(mineral, mix.outputs);
        } catch (RuntimeException exception) {
            return fallbackSubject(mineral);
        }
    }

    static DominantSubject subjectForMineral(ResourceLocation mineral, List<StackWithChance> outputs) {
        ResourceLocation subject = dominantOutputResource(outputs).orElse(mineral);
        return new DominantSubject(
                subject,
                humanName(subject)
        );
    }

    static DominantSubject subjectForResolvedOutputs(ResourceLocation mineral, List<ResolvedOutput> outputs) {
        ResourceLocation subject = dominantResolvedOutputResource(outputs).orElse(mineral);
        return new DominantSubject(
                subject,
                humanName(subject)
        );
    }

    static Optional<ResourceLocation> dominantOutputResource(List<StackWithChance> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return Optional.empty();
        }
        List<ResolvedOutput> resolvedOutputs = outputs.stream()
                .map(IECoreSampleReader::toOutputSubject)
                .flatMap(Optional::stream)
                .toList();
        return dominantResolvedOutputResource(resolvedOutputs);
    }

    static Optional<ResourceLocation> dominantResolvedOutputResource(List<ResolvedOutput> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return Optional.empty();
        }
        return outputs.stream()
                .filter(output -> output != null && output.resourceId() != null && Double.isFinite(output.score()) && output.score() > 0.0D)
                .max(Comparator.comparingDouble(ResolvedOutput::score))
                .map(ResolvedOutput::resourceId);
    }

    private static Optional<ResolvedOutput> toOutputSubject(StackWithChance output) {
        if (output == null || !Double.isFinite(output.chance()) || output.chance() <= 0.0F) {
            return Optional.empty();
        }
        try {
            if (output.stack() == null) {
                return Optional.empty();
            }
            ItemStack stack = output.stack().get();
            if (stack.isEmpty()) {
                return Optional.empty();
            }
            ResourceLocation resourceId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (resourceId == null) {
                return Optional.empty();
            }
            return Optional.of(new ResolvedOutput(resourceId, output.chance() * Math.max(1, stack.getCount())));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static DominantSubject fallbackSubject(ResourceLocation mineral) {
        return new DominantSubject(mineral, humanName(mineral));
    }

    private static double sampleShare(CoresampleItem.VeinSample vein) {
        double share = vein.percentageInTotalSample();
        return Double.isFinite(share) ? share : 0.0D;
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
        String rawPath = id.getPath();
        int slash = rawPath.lastIndexOf('/');
        String path = (slash >= 0 ? rawPath.substring(slash + 1) : rawPath).replace('_', ' ').replace('-', ' ');
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

    record DominantSubject(ResourceLocation resourceId, String displayName) {
    }

    record ResolvedOutput(ResourceLocation resourceId, double score) {
    }
}
