package com.oblixorprime.immersivedepositscanner.client.journeymap;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.config.ClientConfig;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

final class JourneyMapExternalTextureResolver {
    private static final int MAX_LOGGED_ENTRIES = 128;
    private static final Set<String> LOGGED_MALFORMED_MAPPINGS = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_MISSING_TEXTURES = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_VERIFICATION_FAILURES = ConcurrentHashMap.newKeySet();

    private JourneyMapExternalTextureResolver() {
    }

    static Optional<ResourceLocation> resolve(TrackedDeposit deposit) {
        return resolve(
                deposit,
                getConfiguredMappings(),
                JourneyMapExternalTextureResolver::resourceExists
        );
    }

    private static List<? extends String> getConfiguredMappings() {
        try {
            return ClientConfig.EXTERNAL_SUBJECT_TEXTURE_MAPPINGS.get();
        } catch (IllegalStateException exception) {
            ImmersiveDepositScanner.LOGGER.debug(
                    "Unable to read JourneyMap external subject texture mappings; using fallback",
                    exception
            );
            return List.of();
        }
    }

    static Optional<ResourceLocation> resolve(
            TrackedDeposit deposit,
            List<? extends String> mappings,
            ResourceVerifier verifier
    ) {
        if (deposit == null || mappings == null || mappings.isEmpty() || verifier == null) {
            return Optional.empty();
        }

        ResourceLocation subject = JourneyMapSubjectVisuals.subjectId(deposit);
        for (String mapping : mappings) {
            Optional<TextureMapping> parsed = parseMapping(mapping);
            if (parsed.isEmpty() || !parsed.get().subject().equals(subject)) {
                continue;
            }

            ResourceLocation texture = parsed.get().texture();
            try {
                if (verifier.exists(texture)) {
                    return Optional.of(texture);
                }
                logMissingTextureOnce(subject, texture);
            } catch (Exception exception) {
                logVerificationFailureOnce(subject, texture, exception);
            }
        }
        return Optional.empty();
    }

    private static Optional<TextureMapping> parseMapping(String rawMapping) {
        if (rawMapping == null) {
            logMalformedMappingOnce("<null>");
            return Optional.empty();
        }

        String mapping = rawMapping.trim();
        int separator = mapping.indexOf('=');
        if (mapping.isEmpty() || separator <= 0 || separator >= mapping.length() - 1 || mapping.indexOf('=', separator + 1) >= 0) {
            logMalformedMappingOnce(rawMapping);
            return Optional.empty();
        }

        ResourceLocation subject = ResourceLocation.tryParse(mapping.substring(0, separator).trim());
        ResourceLocation texture = ResourceLocation.tryParse(mapping.substring(separator + 1).trim());
        if (subject == null || texture == null) {
            logMalformedMappingOnce(rawMapping);
            return Optional.empty();
        }
        return Optional.of(new TextureMapping(subject, texture));
    }

    private static boolean resourceExists(ResourceLocation texture) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) {
                logVerificationUnavailableOnce(texture, "Minecraft client is not initialized");
                return false;
            }
            ResourceManager resourceManager = minecraft.getResourceManager();
            if (resourceManager == null) {
                logVerificationUnavailableOnce(texture, "Minecraft resource manager is not initialized");
                return false;
            }
            return resourceManager.getResource(texture).isPresent();
        } catch (Exception exception) {
            logVerificationFailureOnce(null, texture, exception);
            return false;
        }
    }

    private static void logMalformedMappingOnce(String mapping) {
        if (remember(LOGGED_MALFORMED_MAPPINGS, mapping)) {
            ImmersiveDepositScanner.LOGGER.warn(
                    "Ignoring malformed JourneyMap external subject texture mapping '{}'; expected subject_resource_id=texture_resource_location",
                    mapping
            );
        }
    }

    private static void logMissingTextureOnce(ResourceLocation subject, ResourceLocation texture) {
        String key = subject + "=" + texture;
        if (remember(LOGGED_MISSING_TEXTURES, key)) {
            ImmersiveDepositScanner.LOGGER.debug(
                    "JourneyMap external subject texture {} for {} is missing; using generic marker fallback",
                    texture,
                    subject
            );
        }
    }

    private static void logVerificationUnavailableOnce(ResourceLocation texture, String reason) {
        String key = "unavailable:" + texture + ":" + reason;
        if (remember(LOGGED_VERIFICATION_FAILURES, key)) {
            ImmersiveDepositScanner.LOGGER.debug(
                    "JourneyMap external subject texture {} cannot be verified because {}; using generic marker fallback",
                    texture,
                    reason
            );
        }
    }

    private static void logVerificationFailureOnce(ResourceLocation subject, ResourceLocation texture, Exception exception) {
        String key = subject + "=" + texture;
        if (remember(LOGGED_VERIFICATION_FAILURES, key)) {
            ImmersiveDepositScanner.LOGGER.warn(
                    "Unable to verify JourneyMap external subject texture {} for {}; using generic marker fallback",
                    texture,
                    subject == null ? "<unknown>" : subject,
                    exception
            );
        }
    }

    private static boolean remember(Set<String> logged, String key) {
        String normalized = key == null ? "<null>" : key;
        if (logged.contains(normalized) || logged.size() >= MAX_LOGGED_ENTRIES) {
            return false;
        }
        return logged.add(normalized);
    }

    @FunctionalInterface
    interface ResourceVerifier {
        boolean exists(ResourceLocation texture);
    }

    private record TextureMapping(ResourceLocation subject, ResourceLocation texture) {
    }
}
