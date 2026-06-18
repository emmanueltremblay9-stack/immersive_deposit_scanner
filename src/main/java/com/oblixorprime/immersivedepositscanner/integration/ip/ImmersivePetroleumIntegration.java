package com.oblixorprime.immersivedepositscanner.integration.ip;

import flaxbeard.immersivepetroleum.common.IPContent;
import net.minecraft.world.item.ItemStack;

public final class ImmersivePetroleumIntegration {
    private ImmersivePetroleumIntegration() {
    }

    public static boolean isSurveyResult(ItemStack stack) {
        return !stack.isEmpty() && stack.is(IPContent.Items.SURVEYRESULT.get());
    }
}

