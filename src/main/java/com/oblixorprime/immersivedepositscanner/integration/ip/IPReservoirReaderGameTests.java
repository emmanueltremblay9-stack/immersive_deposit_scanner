package com.oblixorprime.immersivedepositscanner.integration.ip;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import flaxbeard.immersivepetroleum.common.util.survey.ReservoirInfo;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmersiveDepositScanner.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IPReservoirReaderGameTests {
    private IPReservoirReaderGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void faultyReservoirInfoIsIgnored(GameTestHelper helper) {
        ReservoirInfo faulty = new ReservoirInfo(0, 0, (byte) 0, 0L, FluidStack.EMPTY, 0);
        helper.assertTrue(!IPReservoirReader.isUsableReservoirInfo(faulty), "faulty IP survey results must not create deposits");

        ReservoirInfo valid = new ReservoirInfo(0, 0, (byte) 100, 1000L, new FluidStack(Fluids.WATER, 1), 1);
        helper.assertTrue(IPReservoirReader.isUsableReservoirInfo(valid), "valid IP survey results must remain readable");

        helper.assertTrue(!IPReservoirReader.isUsableReservoirInfo(null), "null IP reservoir info must be ignored");
        helper.succeed();
    }
}
