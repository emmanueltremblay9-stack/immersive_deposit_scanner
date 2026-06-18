package com.oblixorprime.immersivedepositscanner.integration.ie;

import blusunrize.immersiveengineering.common.items.CoresampleItem;
import blusunrize.immersiveengineering.common.register.IEItems;
import net.minecraft.world.item.ItemStack;

public final class ImmersiveEngineeringIntegration {
    private ImmersiveEngineeringIntegration() {
    }

    public static boolean isCoreSample(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof CoresampleItem || stack.is(IEItems.Misc.CORESAMPLE.get()));
    }
}

