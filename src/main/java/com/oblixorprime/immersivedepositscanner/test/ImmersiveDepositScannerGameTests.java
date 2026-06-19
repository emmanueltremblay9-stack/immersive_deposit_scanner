package com.oblixorprime.immersivedepositscanner.test;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.command.ImmersiveDepositScannerCommands;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmersiveDepositScanner.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ImmersiveDepositScannerGameTests {
    private ImmersiveDepositScannerGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void serverRuntimeBoots(GameTestHelper helper) {
        helper.assertTrue(
                ImmersiveDepositScanner.MOD_ID.equals("immersive_deposit_scanner"),
                "mod id must match the registered game-test namespace"
        );
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void commandRootsAreRegistered(GameTestHelper helper) {
        var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();
        helper.assertTrue(root.getChild(ImmersiveDepositScannerCommands.PRIMARY_ROOT) != null, "primary /ids command root must be registered");
        helper.assertTrue(root.getChild(ImmersiveDepositScannerCommands.LONG_ROOT) != null, "long command alias must be registered");
        helper.assertTrue(root.getChild(ImmersiveDepositScannerCommands.LEGACY_ROOT) != null, "legacy /mst command alias must remain registered");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void clearAllWorksFromServerCommandSource(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "ids clear all");
        helper.succeed();
    }
}
