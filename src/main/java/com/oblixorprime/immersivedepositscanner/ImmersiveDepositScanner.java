package com.oblixorprime.immersivedepositscanner;

import com.mojang.logging.LogUtils;
import com.oblixorprime.immersivedepositscanner.command.ImmersiveDepositScannerCommands;
import com.oblixorprime.immersivedepositscanner.config.ClientConfig;
import com.oblixorprime.immersivedepositscanner.config.ServerConfig;
import com.oblixorprime.immersivedepositscanner.integration.ie.IEIntegrationEvents;
import com.oblixorprime.immersivedepositscanner.integration.ip.IPIntegrationEvents;
import com.oblixorprime.immersivedepositscanner.network.NetworkHandler;
import com.oblixorprime.immersivedepositscanner.tracking.TrackingEvents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(ImmersiveDepositScanner.MOD_ID)
public final class ImmersiveDepositScanner {
    public static final String MOD_ID = "immersive_deposit_scanner";
    public static final String MOD_NAME = "Immersive Deposit Scanner";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ImmersiveDepositScanner(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        modBus.addListener(NetworkHandler::registerPayloads);

        NeoForge.EVENT_BUS.register(new IEIntegrationEvents());
        NeoForge.EVENT_BUS.register(new IPIntegrationEvents());
        NeoForge.EVENT_BUS.register(new TrackingEvents());
        NeoForge.EVENT_BUS.addListener(ImmersiveDepositScannerCommands::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
