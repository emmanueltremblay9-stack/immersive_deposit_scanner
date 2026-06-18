package com.oblixorprime.immersivedepositscanner.integration.ip;

import com.oblixorprime.immersivedepositscanner.tracking.DepositTrackingService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class IPIntegrationEvents {
    @SubscribeEvent
    public void onSurveyResultPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player && ImmersivePetroleumIntegration.isSurveyResult(event.getOriginalStack())) {
            DepositTrackingService.scanStack(player, event.getOriginalStack());
        }
    }

    @SubscribeEvent
    public void onSurveyResultUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && ImmersivePetroleumIntegration.isSurveyResult(event.getItemStack())) {
            DepositTrackingService.scanStack(player, event.getItemStack());
        }
    }
}

