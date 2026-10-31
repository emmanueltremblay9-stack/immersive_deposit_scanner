package com.oblixorprime.immersivedepositscanner.client.journeymap;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.config.ClientConfig;
import com.oblixorprime.immersivedepositscanner.data.DepositSource;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import java.util.Optional;
import java.util.function.Function;
import journeymap.api.v2.client.model.MapImage;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

final class JourneyMapSubjectVisuals {
    private static final int DEPLETED_COLOR = 0x777777;
    private static final ResourceLocation DEPLETED_ICON = ImmersiveDepositScanner.id("textures/journeymap/depleted.png");
    private static final ResourceLocation IE_MINERAL_ICON = ImmersiveDepositScanner.id("textures/journeymap/ie_mineral.png");
    private static final ResourceLocation IP_RESERVOIR_ICON = ImmersiveDepositScanner.id("textures/journeymap/ip_reservoir.png");

    private JourneyMapSubjectVisuals() {
    }

    static ResourceLocation subjectId(TrackedDeposit deposit) {
        return deposit.resourceId().orElse(deposit.key().depositId());
    }

    static String subjectLabel(TrackedDeposit deposit) {
        return subjectId(deposit).toString();
    }

    static MapImage markerImage(TrackedDeposit deposit) {
        ResourceLocation icon = icon(deposit);
        if (icon == null) {
            return null;
        }
        return markerImage(
                deposit,
                getBooleanSetting(
                        ClientConfig.USE_NATIVE_IMAGE_SUBJECT_ICONS,
                        "useNativeImageSubjectIcons",
                        false
                    )
                );
    }

    static MapImage markerImage(TrackedDeposit deposit, boolean useNativeImageSubjectIcons) {
        // JourneyMap 2.0.0 has no ItemStack marker icon API. NativeImage icons are kept disabled
        // until they can be proven stable, so marker images always use packaged IDS textures.
        return new MapImage(icon(deposit), 16, 16).centerAnchors();
    }

    static ResourceLocation icon(TrackedDeposit deposit) {
        return icon(
                deposit,
                getBooleanSetting(
                        ClientConfig.FALLBACK_TO_GENERIC_ICONS,
                        "fallbackToGenericIcons",
                        true
                )
        );
    }

    static ResourceLocation icon(TrackedDeposit deposit, boolean useGenericIconFallback) {
        return icon(
                deposit,
                useExternalSubjectTextureReferences(),
                useGenericIconFallback,
                JourneyMapExternalTextureResolver::resolve
        );
    }

    static ResourceLocation icon(
            TrackedDeposit deposit,
            boolean useExternalSubjectTextureReferences,
            boolean useGenericIconFallback,
            Function<TrackedDeposit, Optional<ResourceLocation>> externalTextureResolver
    ) {
        if (deposit.depleted()) {
            return DEPLETED_ICON;
        }
        if (useExternalSubjectTextureReferences && externalTextureResolver != null) {
            Optional<ResourceLocation> externalIcon = externalTextureResolver.apply(deposit);
            if (externalIcon.isPresent()) {
                return externalIcon.get();
            }
        }
        if (!useGenericIconFallback) {
            return null;
        }
        return genericIcon(deposit.key().source());
    }

    static int color(TrackedDeposit deposit) {
        return color(deposit, useResourceSubjectVisuals());
    }

    static int color(TrackedDeposit deposit, boolean useResourceSubjectIcons) {
        if (useResourceSubjectIcons) {
            return color(deposit, true, 0, 0);
        }
        return color(
                deposit,
                false,
                getIntSetting(
                        ClientConfig.IE_DEFAULT_COLOR,
                        "ieDefaultColor",
                        0x2ED3D3
                ),
                getIntSetting(
                        ClientConfig.IP_DEFAULT_COLOR,
                        "ipDefaultColor",
                        0xF6A83A
                )
        );
    }

    static int color(TrackedDeposit deposit, boolean useResourceSubjectIcons, int ieDefaultColor, int ipDefaultColor) {
        if (deposit.depleted()) {
            return DEPLETED_COLOR;
        }
        if (!useResourceSubjectIcons) {
            return sourceColor(deposit.key().source(), ieDefaultColor, ipDefaultColor);
        }
        return subjectColor(deposit);
    }

    static int sourceColor(DepositSource source) {
        return sourceColor(
                source,
                getIntSetting(
                        ClientConfig.IE_DEFAULT_COLOR,
                        "ieDefaultColor",
                        0x2ED3D3
                ),
                getIntSetting(
                        ClientConfig.IP_DEFAULT_COLOR,
                        "ipDefaultColor",
                        0xF6A83A
                )
        );
    }

    static int sourceColor(DepositSource source, int ieDefaultColor, int ipDefaultColor) {
        return source == DepositSource.IMMERSIVE_ENGINEERING
                ? ieDefaultColor
                : ipDefaultColor;
    }

    static int subjectColor(TrackedDeposit deposit) {
        String value = deposit.key().source().serializedName() + "|" + subjectId(deposit);
        int mixed = smear(value.hashCode());
        int red = 96 + (mixed & 0x7F);
        int green = 96 + ((mixed >>> 8) & 0x7F);
        int blue = 96 + ((mixed >>> 16) & 0x7F);
        return (red << 16) | (green << 8) | blue;
    }

    private static ResourceLocation genericIcon(DepositSource source) {
        return source == DepositSource.IMMERSIVE_ENGINEERING
                ? IE_MINERAL_ICON
                : IP_RESERVOIR_ICON;
    }

    private static boolean useResourceSubjectVisuals() {
        return getBooleanSetting(
                ClientConfig.USE_RESOURCE_SUBJECT_VISUALS,
                "useResourceSubjectVisuals",
                true
        ) && getBooleanSetting(
                ClientConfig.USE_RESOURCE_SUBJECT_ICONS,
                "useResourceSubjectIcons",
                true
        );
    }

    private static boolean useExternalSubjectTextureReferences() {
        try {
            return ClientConfig.USE_EXTERNAL_SUBJECT_TEXTURE_REFERENCES.get();
        } catch (IllegalStateException exception) {
            ImmersiveDepositScanner.LOGGER.debug(
                    "Unable to read client config {}; using fallback {}",
                    "useExternalSubjectTextureReferences",
                    false,
                    exception
            );
            return false;
        }
    }

    private static boolean getBooleanSetting(ModConfigSpec.BooleanValue setting, String settingName, boolean fallback) {
        try {
            return setting.get();
        } catch (IllegalStateException exception) {
            ImmersiveDepositScanner.LOGGER.debug(
                    "Unable to read client config {}; using fallback {}",
                    settingName,
                    fallback,
                    exception
            );
            return fallback;
        }
    }

    private static int getIntSetting(ModConfigSpec.IntValue setting, String settingName, int fallback) {
        try {
            return setting.get();
        } catch (IllegalStateException exception) {
            ImmersiveDepositScanner.LOGGER.debug(
                    "Unable to read client config {}; using fallback {}",
                    settingName,
                    fallback,
                    exception
            );
            return fallback;
        }
    }

    private static int smear(int value) {
        int mixed = value ^ (value >>> 16);
        mixed *= 0x7FEB352D;
        mixed ^= mixed >>> 15;
        mixed *= 0x846CA68B;
        return mixed ^ (mixed >>> 16);
    }
}
