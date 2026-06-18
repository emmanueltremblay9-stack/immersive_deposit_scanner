package com.oblixorprime.immersivedepositscanner.client.journeymap;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.JourneyMapPlugin;

@JourneyMapPlugin(apiVersion = "2.0.0")
public final class ImmersiveDepositScannerJourneyMapPlugin implements IClientPlugin {
    @Override
    public String getModId() {
        return ImmersiveDepositScanner.MOD_ID;
    }

    @Override
    public void initialize(IClientAPI api) {
        JourneyMapDepositManager.initialize(api);
    }
}
