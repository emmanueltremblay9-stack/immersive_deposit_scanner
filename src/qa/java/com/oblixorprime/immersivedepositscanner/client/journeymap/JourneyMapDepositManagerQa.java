package com.oblixorprime.immersivedepositscanner.client.journeymap;

import com.oblixorprime.immersivedepositscanner.config.ClientConfig;
import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.data.DepositKind;
import com.oblixorprime.immersivedepositscanner.data.DepositSource;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import com.oblixorprime.immersivedepositscanner.data.TrackedDepositKey;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Displayable;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointGroup;

public final class JourneyMapDepositManagerQa {
    private JourneyMapDepositManagerQa() {
    }

    public static void run() {
        assertCleanupToleratesApiFailures();
        assertLegacyCleanupRemovesIdsWaypointsOnly();
        assertShowFailureDoesNotTrackDisplays();
        assertSubjectVisualsUseResourceIdentity();
        assertJourneyMapLabelDefaults();
    }

    private static void assertJourneyMapLabelDefaults() {
        assertEquals(Boolean.TRUE, ClientConfig.SHOW_JOURNEYMAP_MARKER_LABELS.getDefault(), "marker labels should default to visible");
        assertEquals(Boolean.FALSE, ClientConfig.SHOW_JOURNEYMAP_OVERLAY_LABELS.getDefault(), "overlay labels should default to hidden");
        assertEquals(Boolean.FALSE, ClientConfig.SHOW_JOURNEYMAP_WAYPOINT_LABELS.getDefault(), "waypoint labels should default to short labels");
        assertEquals(Boolean.FALSE, ClientConfig.SHOW_JOURNEYMAP_SUBJECT_IDS_IN_LABEL.getDefault(), "technical subject ids should be hidden from labels by default");
        assertEquals(Boolean.FALSE, ClientConfig.ENABLE_WAYPOINTS.getDefault(), "JourneyMap waypoints should be disabled by default");
        assertEquals(Boolean.TRUE, ClientConfig.FALLBACK_TO_GENERIC_ICONS.getDefault(), "journeymap icons should default to generic fallback");
    }

    private static void assertLegacyCleanupRemovesIdsWaypointsOnly() {
        Field apiField;
        Object originalApi;
        try {
            apiField = JourneyMapDepositManager.class.getDeclaredField("api");
            apiField.setAccessible(true);
            originalApi = apiField.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("JourneyMapDepositManager API field must be reachable for legacy cleanup QA", exception);
        }

        Waypoint brokenWaypoint = throwingWaypoint("broken");
        Waypoint ownedWaypoint = waypoint("owned", ImmersiveDepositScanner.MOD_ID, "ids-owned-group", null);
        Waypoint groupedWaypoint = waypoint("grouped", "other_mod", "ids-group", null);
        Waypoint customDataWaypoint = waypoint("custom", "other_mod", "custom-group", "ids_123");
        Waypoint userWaypoint = waypoint("user", "other_mod", "user-group", null);
        List<Waypoint> removedWaypoints = new ArrayList<>();
        AtomicInteger removeAllCalls = new AtomicInteger();
        AtomicInteger removeAllWaypointsCalls = new AtomicInteger();

        IClientAPI cleanupApi = (IClientAPI) Proxy.newProxyInstance(
                IClientAPI.class.getClassLoader(),
                new Class<?>[]{IClientAPI.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "removeAll" -> {
                        removeAllCalls.incrementAndGet();
                        yield null;
                    }
                    case "removeAllWaypoints" -> {
                        removeAllWaypointsCalls.incrementAndGet();
                        yield null;
                    }
                    case "getAllWaypoints" -> List.of(brokenWaypoint, ownedWaypoint, groupedWaypoint, customDataWaypoint, userWaypoint);
                    case "getWaypointGroup" -> waypointGroup((String) args[0]);
                    case "removeWaypoint" -> {
                        removedWaypoints.add((Waypoint) args[1]);
                        yield null;
                    }
                    default -> defaultValue(method.getReturnType());
                }
        );

        try {
            apiField.set(null, cleanupApi);
            JourneyMapDepositManager.cleanupLegacyJourneyMapState();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("JourneyMapDepositManager API field must be writable for legacy cleanup QA", exception);
        } finally {
            try {
                apiField.set(null, originalApi);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("JourneyMapDepositManager API field must be restorable after legacy cleanup QA", exception);
            }
        }

        assertTrue(removeAllCalls.get() > 0, "legacy cleanup must call JourneyMap removeAll for IDS displays");
        assertTrue(removeAllWaypointsCalls.get() > 0, "legacy cleanup must call JourneyMap removeAllWaypoints for IDS waypoints");
        assertTrue(!containsIdentity(removedWaypoints, brokenWaypoint), "legacy cleanup must not remove a waypoint it could not inspect");
        assertTrue(containsIdentity(removedWaypoints, ownedWaypoint), "legacy cleanup must remove IDS-owned waypoints");
        assertTrue(containsIdentity(removedWaypoints, groupedWaypoint), "legacy cleanup must remove waypoints in IDS-owned IDS groups");
        assertTrue(containsIdentity(removedWaypoints, customDataWaypoint), "legacy cleanup must remove waypoints tagged with IDS custom data");
        assertTrue(!containsIdentity(removedWaypoints, userWaypoint), "legacy cleanup must not remove unrelated user waypoints");
    }

    private static void assertCleanupToleratesApiFailures() {
        Field apiField;
        Object originalApi;
        try {
            apiField = JourneyMapDepositManager.class.getDeclaredField("api");
            apiField.setAccessible(true);
            originalApi = apiField.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("JourneyMapDepositManager API field must be reachable for cleanup QA", exception);
        }

        IClientAPI throwingApi = (IClientAPI) Proxy.newProxyInstance(
                IClientAPI.class.getClassLoader(),
                new Class<?>[]{IClientAPI.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("removeAll") || method.getName().equals("removeAllWaypoints")) {
                        throw new IllegalStateException("simulated JourneyMap cleanup failure");
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        try {
            apiField.set(null, throwingApi);
            JourneyMapDepositManager.clearAll();
        } catch (RuntimeException exception) {
            throw new AssertionError("JourneyMap cleanup failures must not escape cache cleanup", exception);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("JourneyMapDepositManager API field must be writable for cleanup QA", exception);
        } finally {
            try {
                apiField.set(null, originalApi);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("JourneyMapDepositManager API field must be restorable after cleanup QA", exception);
            }
        }
    }

    private static void assertShowFailureDoesNotTrackDisplays() {
        Field apiField;
        Method trackIfShown;
        Object originalApi;
        try {
            apiField = JourneyMapDepositManager.class.getDeclaredField("api");
            trackIfShown = JourneyMapDepositManager.class.getDeclaredMethod(
                    "trackIfShown",
                    Map.class,
                    TrackedDepositKey.class,
                    Displayable.class
            );
            apiField.setAccessible(true);
            trackIfShown.setAccessible(true);
            originalApi = apiField.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("JourneyMapDepositManager display tracker must be reachable for display QA", exception);
        }

        IClientAPI throwingApi = (IClientAPI) Proxy.newProxyInstance(
                IClientAPI.class.getClassLoader(),
                new Class<?>[]{IClientAPI.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("show")) {
                        throw new IllegalStateException("simulated JourneyMap display failure");
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        TrackedDepositKey key = new TrackedDepositKey(
                Level.OVERWORLD,
                2,
                -3,
                DepositSource.IMMERSIVE_ENGINEERING,
                ResourceLocation.fromNamespaceAndPath("immersiveengineering", "show_failure_mineral")
        );
        Displayable display = new MarkerOverlay(
                "immersive_deposit_scanner",
                new BlockPos(40, 64, -40),
                new MapImage(ResourceLocation.fromNamespaceAndPath("immersive_deposit_scanner", "textures/journeymap/ie_mineral.png"), 16, 16)
        );
        Map<TrackedDepositKey, Displayable> displays = new LinkedHashMap<>();

        try {
            apiField.set(null, throwingApi);
            trackIfShown.invoke(null, displays, key, display);
            assertTrue(displays.isEmpty(), "JourneyMap displays must not be tracked when show fails");
            trackIfShown.invoke(null, displays, key, null);
            assertTrue(displays.isEmpty(), "null JourneyMap displays must not be tracked");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("JourneyMapDepositManager display tracker must be callable for display QA", exception);
        } finally {
            try {
                apiField.set(null, originalApi);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("JourneyMapDepositManager API field must be restorable after display QA", exception);
            }
        }
    }

    private static void assertSubjectVisualsUseResourceIdentity() {
        TrackedDepositKey ieKey = new TrackedDepositKey(
                Level.OVERWORLD,
                2,
                -3,
                DepositSource.IMMERSIVE_ENGINEERING,
                ResourceLocation.fromNamespaceAndPath("immersiveengineering", "technical_mineral")
        );
        TrackedDeposit ieDeposit = deposit(
                ieKey,
                DepositKind.MINERAL,
                "Cinnabar",
                Optional.of(ResourceLocation.fromNamespaceAndPath("minecraft", "coal")),
                false
        );
        TrackedDepositKey ipKey = new TrackedDepositKey(
                Level.OVERWORLD,
                2,
                -3,
                DepositSource.IMMERSIVE_PETROLEUM,
                ResourceLocation.fromNamespaceAndPath("immersivepetroleum", "technical_reservoir")
        );
        TrackedDeposit ipDeposit = deposit(
                ipKey,
                DepositKind.FLUID_RESERVOIR,
                "Sample Reservoir",
                Optional.of(ResourceLocation.fromNamespaceAndPath("minecraft", "water")),
                false
        );
        TrackedDeposit fallbackSubjectDeposit = deposit(
                ieKey,
                DepositKind.MINERAL,
                "Fallback Mineral",
                Optional.empty(),
                false
        );
        TrackedDeposit unknownSubjectDeposit = deposit(
                ieKey,
                DepositKind.MINERAL,
                "Unknown Mineral",
                Optional.of(ResourceLocation.fromNamespaceAndPath("unknown_mod", "unknown_resource")),
                false
        );
        TrackedDeposit depletedDeposit = deposit(
                ieKey,
                DepositKind.MINERAL,
                "Empty Mineral",
                Optional.of(ResourceLocation.fromNamespaceAndPath("minecraft", "coal")),
                true
        );
        ResourceLocation coalTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/coal.png");
        List<String> validMappings = List.of("minecraft:coal=minecraft:textures/item/coal.png");

        assertEquals(
                ResourceLocation.fromNamespaceAndPath("minecraft", "coal"),
                JourneyMapSubjectVisuals.subjectId(ieDeposit),
                "resourceId must be preferred as the JourneyMap subject even when it differs from depositId"
        );
        assertTrue(
                !ieDeposit.key().depositId().equals(ieDeposit.resourceId().orElseThrow()),
                "resourceId must be allowed to differ from the stable depositId"
        );
        assertEquals(
                ieKey.depositId(),
                JourneyMapSubjectVisuals.subjectId(fallbackSubjectDeposit),
                "depositId must be the JourneyMap subject fallback"
        );
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("immersive_deposit_scanner", "textures/journeymap/ie_mineral.png"),
                JourneyMapMarkerFactory.icon(ieDeposit),
                "IE subject markers must keep the generic texture fallback"
        );
        assertNotNull(JourneyMapMarkerFactory.icon(ieDeposit), "IE marker factory icons must never be null");
        assertEquals(
                Optional.of(coalTexture),
                JourneyMapExternalTextureResolver.resolve(ieDeposit, validMappings, texture -> texture.equals(coalTexture)),
                "valid external texture mappings must resolve to the configured texture"
        );
        assertEquals(
                Optional.empty(),
                JourneyMapExternalTextureResolver.resolve(ieDeposit, List.of("not-a-mapping", "minecraft:coal="), texture -> true),
                "malformed external texture mappings must be ignored"
        );
        assertEquals(
                Optional.empty(),
                JourneyMapExternalTextureResolver.resolve(ieDeposit, validMappings, texture -> false),
                "missing external textures must not resolve"
        );
        assertEquals(
                coalTexture,
                JourneyMapSubjectVisuals.icon(
                        ieDeposit,
                        true,
                        true,
                        deposit -> JourneyMapExternalTextureResolver.resolve(deposit, validMappings, texture -> true)
                ),
                "valid external texture mappings can select a marker texture when explicitly enabled"
        );
        assertEquals(
                coalTexture,
                JourneyMapSubjectVisuals.icon(
                        ieDeposit,
                        true,
                        false,
                        deposit -> JourneyMapExternalTextureResolver.resolve(deposit, validMappings, texture -> true)
                ),
                "fallback-to-generic flag should not suppress explicit external texture mappings"
        );
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("immersive_deposit_scanner", "textures/journeymap/ie_mineral.png"),
                JourneyMapSubjectVisuals.icon(
                        ieDeposit,
                        true,
                        true,
                        deposit -> JourneyMapExternalTextureResolver.resolve(deposit, validMappings, texture -> false)
                ),
                "missing external textures must fall back to the generic IE texture"
        );
        assertTrue(
                JourneyMapSubjectVisuals.icon(
                        ieDeposit,
                        true,
                        false,
                        deposit -> JourneyMapExternalTextureResolver.resolve(deposit, validMappings, texture -> false)
                ) == null,
                "fallback-to-generic false should not return a generic IDS icon when no external icon is available"
        );
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("immersive_deposit_scanner", "textures/journeymap/ip_reservoir.png"),
                JourneyMapMarkerFactory.icon(ipDeposit),
                "IP subject markers must keep the generic texture fallback"
        );
        assertNotNull(JourneyMapMarkerFactory.icon(ipDeposit), "IP marker factory icons must never be null");
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("immersive_deposit_scanner", "textures/journeymap/ie_mineral.png"),
                JourneyMapMarkerFactory.icon(unknownSubjectDeposit),
                "unknown resource subjects must use the safe generic IE texture fallback"
        );
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("immersive_deposit_scanner", "textures/journeymap/ie_mineral.png"),
                JourneyMapSubjectVisuals.icon(
                        unknownSubjectDeposit,
                        true,
                        true,
                        deposit -> JourneyMapExternalTextureResolver.resolve(deposit, validMappings, texture -> true)
                ),
                "unknown subjects must fall back when no matching external texture mapping exists"
        );
        assertNotNull(JourneyMapMarkerFactory.icon(unknownSubjectDeposit), "unknown subject marker factory icons must never be null");
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("immersive_deposit_scanner", "textures/journeymap/depleted.png"),
                JourneyMapMarkerFactory.icon(depletedDeposit),
                "depleted subject markers must use the depleted texture"
        );
        assertNotNull(JourneyMapMarkerFactory.icon(depletedDeposit), "depleted marker factory icons must never be null");
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("immersive_deposit_scanner", "textures/journeymap/depleted.png"),
                JourneyMapSubjectVisuals.icon(depletedDeposit, true, true, deposit -> Optional.of(coalTexture)),
                "depleted deposits must override external texture mappings"
        );
        String label = JourneyMapMarkerFactory.label(ieDeposit, false, true, false);
        assertEquals("[IE] Cinnabar [2, -3] 10%", label, "default JourneyMap labels must be concise");
        assertTrue(!label.contains("subject="), "default JourneyMap labels must not expose technical subject ids");
        String debugLabel = JourneyMapMarkerFactory.label(ieDeposit, true, true, false);
        assertTrue(debugLabel.contains("subject=minecraft:coal"), "debug subject labels must expose only the scanned resource id");
        assertTrue(debugLabel.contains("10%"), "subject labels must keep the configured remaining percentage");
        assertEquals(
                "",
                JourneyMapMarkerFactory.markerMapLabel(ieDeposit, false, false, true, false),
                "marker labels must be suppressible without returning null"
        );
        assertEquals(
                label,
                JourneyMapMarkerFactory.markerMapLabel(ieDeposit, true, false, true, false),
                "marker labels should use the concise marker formatter"
        );
        assertEquals(
                "",
                JourneyMapOverlayFactory.overlayMapLabel(ieDeposit, false, false, true, false),
                "chunk overlay labels must be disabled by default"
        );
        assertEquals(
                label,
                JourneyMapOverlayFactory.overlayMapLabel(ieDeposit, true, false, true, false),
                "opt-in chunk overlay labels should still use the concise marker formatter"
        );
        assertEquals("[IE]", JourneyMapMarkerFactory.waypointLabel(ieDeposit, false), "waypoint labels should be very short by default");
        assertEquals("[IE] Cinnabar", JourneyMapMarkerFactory.waypointLabel(ieDeposit, true), "opt-in waypoint labels should stay short");
        assertTrue(
                JourneyMapSubjectVisuals.color(ieDeposit, true) != JourneyMapSubjectVisuals.color(ipDeposit, true),
                "different resource subjects in the same chunk should receive different display colors"
        );
        assertTrue(
                JourneyMapSubjectVisuals.color(unknownSubjectDeposit, true) != JourneyMapSubjectVisuals.color(fallbackSubjectDeposit, true),
                "unknown resource ids must still produce deterministic subject colors"
        );
        MapImage image = JourneyMapSubjectVisuals.markerImage(ieDeposit, false);
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("immersive_deposit_scanner", "textures/journeymap/ie_mineral.png"),
                image.getImageLocation(),
                "marker images must use the safe generic IE texture"
        );
        assertEquals(
                null,
                image.getImage(),
                "marker images must not use NativeImage by default"
        );
        MapImage nativeRequestedImage = JourneyMapSubjectVisuals.markerImage(unknownSubjectDeposit, true);
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("immersive_deposit_scanner", "textures/journeymap/ie_mineral.png"),
                nativeRequestedImage.getImageLocation(),
                "NativeImage subject icons must remain disabled until a safe implementation exists"
        );
        assertEquals(
                null,
                nativeRequestedImage.getImage(),
                "NativeImage subject icons must not be created by the fallback path"
        );
        assertEquals(
                0x2ED3D3,
                JourneyMapSubjectVisuals.color(ieDeposit, false, 0x2ED3D3, 0xF6A83A),
                "disabling subject icons must fall back to the generic source color"
        );
    }

    private static TrackedDeposit deposit(
            TrackedDepositKey key,
            DepositKind kind,
            String displayName,
            Optional<ResourceLocation> resourceId,
            boolean depleted
    ) {
        return new TrackedDeposit(
                key,
                kind,
                displayName,
                resourceId,
                Optional.of(new BlockPos(key.chunkX() * 16 + 8, 64, key.chunkZ() * 16 + 8)),
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                1000L,
                1000L,
                OptionalLong.of(10L),
                OptionalLong.of(100L),
                OptionalDouble.of(0.10D),
                depleted
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive() || returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Character.TYPE) {
            return '\0';
        }
        return 0;
    }

    private static Waypoint waypoint(String id, String modId, String groupId, String idsStableKey) {
        return (Waypoint) Proxy.newProxyInstance(
                Waypoint.class.getClassLoader(),
                new Class<?>[]{Waypoint.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getGuid", "getId", "getName" -> id;
                    case "getModId" -> modId;
                    case "getGroupId" -> groupId;
                    case "getCustomData" -> args != null && args.length == 1 && "ids:key".equals(args[0]) ? idsStableKey : null;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Waypoint throwingWaypoint(String id) {
        return (Waypoint) Proxy.newProxyInstance(
                Waypoint.class.getClassLoader(),
                new Class<?>[]{Waypoint.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getGuid", "getId", "getName" -> id;
                    case "getModId" -> throw new IllegalStateException("simulated waypoint inspection failure");
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static boolean containsIdentity(List<Waypoint> waypoints, Waypoint expected) {
        return waypoints.stream().anyMatch(waypoint -> waypoint == expected);
    }

    private static WaypointGroup waypointGroup(String groupId) {
        if ("ids-group".equals(groupId)) {
            return waypointGroup(ImmersiveDepositScanner.MOD_ID, "Immersive Deposit Scanner / Immersive Engineering");
        }
        return waypointGroup("other_mod", "User Group");
    }

    private static WaypointGroup waypointGroup(String modId, String name) {
        return (WaypointGroup) Proxy.newProxyInstance(
                WaypointGroup.class.getClassLoader(),
                new Class<?>[]{WaypointGroup.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getGuid", "getTag" -> name;
                    case "getModId" -> modId;
                    case "getName" -> name;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
