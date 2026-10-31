package com.oblixorprime.immersivedepositscanner.command;

import com.oblixorprime.immersivedepositscanner.data.DepositKind;
import com.oblixorprime.immersivedepositscanner.data.DepositSource;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import com.oblixorprime.immersivedepositscanner.data.TrackedDepositKey;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class ImmersiveDepositScannerCommandsQa {
    private ImmersiveDepositScannerCommandsQa() {
    }

    public static void run() {
        assertListOutputExposesSubjectMappingIds();
        assertListOutputExposesDepositIdWhenResourceIdIsAbsent();
        assertVersionOutputIdentifiesRuntimeAndClientDefaults();
    }

    private static void assertListOutputExposesSubjectMappingIds() {
        TrackedDeposit deposit = deposit(
                DepositSource.IMMERSIVE_ENGINEERING,
                DepositKind.MINERAL,
                ResourceLocation.fromNamespaceAndPath("immersiveengineering", "technical_mineral"),
                Optional.of(ResourceLocation.fromNamespaceAndPath("minecraft", "coal"))
        );

        String line = ImmersiveDepositScannerCommands.formatDeposit(deposit);
        assertContains(line, "Sample Deposit", "list output must include displayName");
        assertContains(line, "source=immersive_engineering", "list output must include source");
        assertContains(line, "chunk=minecraft:overworld 2,-3", "list output must include dimension and chunk coordinates");
        assertContains(line, "resourceId=minecraft:coal", "list output must expose resourceId for external texture mappings");
        assertContains(line, "depositId=immersiveengineering:technical_mineral", "list output must expose key depositId");
    }

    private static void assertListOutputExposesDepositIdWhenResourceIdIsAbsent() {
        TrackedDeposit deposit = deposit(
                DepositSource.IMMERSIVE_PETROLEUM,
                DepositKind.FLUID_RESERVOIR,
                ResourceLocation.fromNamespaceAndPath("immersivepetroleum", "technical_reservoir"),
                Optional.empty()
        );

        String line = ImmersiveDepositScannerCommands.formatDeposit(deposit);
        assertContains(line, "resourceId=<none>", "list output must show when resourceId is absent");
        assertContains(line, "depositId=immersivepetroleum:technical_reservoir", "depositId must remain visible as the mapping fallback");
    }

    private static void assertVersionOutputIdentifiesRuntimeAndClientDefaults() {
        String output = String.join("\n", ImmersiveDepositScannerCommands.formatVersionLines("1.0.26"));
        assertContains(output, "modId=immersive_deposit_scanner", "version output must include mod id");
        assertContains(output, "modVersion=1.0.26", "version output must include loaded mod version");
        assertContains(output, "journeyMapLabelCleanup=active", "version output must identify JourneyMap label cleanup");
        assertContains(output, "enableWaypoints=false", "version output must include waypoint default");
        assertContains(output, "overlayLabels=false", "version output must include overlay label default");
        assertContains(output, "subjectIdsInLabel=false", "version output must include subject id label default");
        assertContains(output, "client-local", "version output must say JourneyMap config is client-local");
    }

    private static TrackedDeposit deposit(
            DepositSource source,
            DepositKind kind,
            ResourceLocation depositId,
            Optional<ResourceLocation> resourceId
    ) {
        TrackedDepositKey key = new TrackedDepositKey(
                Level.OVERWORLD,
                2,
                -3,
                source,
                depositId
        );
        return new TrackedDeposit(
                key,
                kind,
                "Sample Deposit",
                resourceId,
                Optional.empty(),
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                1000L,
                1000L,
                OptionalLong.empty(),
                OptionalLong.empty(),
                OptionalDouble.empty(),
                false
        );
    }

    private static void assertContains(String value, String expected, String message) {
        if (!value.contains(expected)) {
            throw new AssertionError(message + " expected fragment=" + expected + " actual=" + value);
        }
    }
}
