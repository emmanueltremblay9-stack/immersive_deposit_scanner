package com.oblixorprime.immersivedepositscanner.client;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ImmersiveDepositScanner.MOD_ID, dist = Dist.CLIENT)
public final class ImmersiveDepositScannerClient {
    public ImmersiveDepositScannerClient(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(new ClientLifecycleEvents());
    }
}

