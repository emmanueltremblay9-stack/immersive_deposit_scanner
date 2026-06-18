package com.oblixorprime.immersivedepositscanner.test;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
}
