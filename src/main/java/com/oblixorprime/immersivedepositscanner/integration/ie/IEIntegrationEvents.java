package com.oblixorprime.immersivedepositscanner.integration.ie;

import com.oblixorprime.immersivedepositscanner.tracking.DepositTrackingService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class IEIntegrationEvents {
    @SubscribeEvent
    public void onCoreSamplePickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player && ImmersiveEngineeringIntegration.isCoreSample(event.getOriginalStack())) {
            DepositTrackingService.scanStack(player, event.getOriginalStack());
        }
    }

    @SubscribeEvent
    public void onCoreSampleUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && ImmersiveEngineeringIntegration.isCoreSample(event.getItemStack())) {
            DepositTrackingService.scanStack(player, event.getItemStack());
        }
    }
}

