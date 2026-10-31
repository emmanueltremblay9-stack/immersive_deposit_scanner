package com.oblixorprime.immersivedepositscanner.client.journeymap;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.client.ClientDepositCache;
import com.oblixorprime.immersivedepositscanner.config.ClientConfig;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import com.oblixorprime.immersivedepositscanner.data.TrackedDepositKey;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Displayable;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointGroup;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class JourneyMapDepositManager {
    private static final String IDS_KEY_CUSTOM_DATA = "ids:key";
    private static final Set<String> IDS_GROUP_NAMES = Set.of(
            "Immersive Deposit Scanner / Immersive Engineering",
            "Immersive Deposit Scanner / Immersive Petroleum"
    );
    private static final Map<TrackedDepositKey, MarkerOverlay> MARKERS = new LinkedHashMap<>();
    private static final Map<TrackedDepositKey, PolygonOverlay> OVERLAYS = new LinkedHashMap<>();
    private static final Map<TrackedDepositKey, Waypoint> WAYPOINTS = new LinkedHashMap<>();
    private static IClientAPI api;

    private JourneyMapDepositManager() {
    }

    public static void initialize(IClientAPI clientApi) {
        api = clientApi;
        ImmersiveDepositScanner.LOGGER.info("JourneyMap API initialized for {}; running IDS legacy display cleanup", ImmersiveDepositScanner.MOD_NAME);
        cleanupLegacyJourneyMapState();
        ClientDepositCache.getAll().forEach(JourneyMapDepositManager::upsert);
    }

    public static void replaceAll(Collection<TrackedDeposit> deposits) {
        if (api == null) {
            return;
        }
        clearAll();
        deposits.forEach(JourneyMapDepositManager::upsert);
    }

    public static void upsert(TrackedDeposit deposit) {
        if (api == null) {
            return;
        }
        remove(deposit.key());
        if (getBooleanSetting(
                ClientConfig.ENABLE_JOURNEYMAP_MARKERS,
                "enableJourneyMapMarkers",
                true
        ) && JourneyMapMarkerFactory.shouldShow(deposit)) {
            if (getBooleanSetting(
                    ClientConfig.ENABLE_WAYPOINTS,
                    "enableWaypoints",
                    false
            )) {
                createAndAddWaypoint(deposit);
            }
            createAndShowMarker(deposit);
        }
        if (getBooleanSetting(
                ClientConfig.ENABLE_JOURNEYMAP_CHUNK_OVERLAYS,
                "enableJourneyMapChunkOverlays",
                true
        )
                && getBooleanSetting(
                ClientConfig.ENABLE_CHUNK_OVERLAYS,
                "enableChunkOverlays",
                true
        )
                && JourneyMapOverlayFactory.shouldShow(deposit)) {
            createAndShowOverlay(deposit);
        }
    }

    public static void remove(TrackedDepositKey key) {
        if (api == null) {
            return;
        }
        MarkerOverlay marker = MARKERS.remove(key);
        if (marker != null) {
            remove(marker);
        }
        PolygonOverlay overlay = OVERLAYS.remove(key);
        if (overlay != null) {
            remove(overlay);
        }
        Waypoint waypoint = WAYPOINTS.remove(key);
        if (waypoint != null) {
            removeWaypoint(waypoint);
        }
    }

    public static void clearAll() {
        if (api == null) {
            MARKERS.clear();
            OVERLAYS.clear();
            WAYPOINTS.clear();
            return;
        }
        MARKERS.values().forEach(JourneyMapDepositManager::remove);
        OVERLAYS.values().forEach(JourneyMapDepositManager::remove);
        WAYPOINTS.values().forEach(JourneyMapDepositManager::removeWaypoint);
        MARKERS.clear();
        OVERLAYS.clear();
        WAYPOINTS.clear();
        removeAll();
        removeAllWaypoints();
    }

    static void cleanupLegacyJourneyMapState() {
        clearAll();
        removeLegacyWaypointsByOwnerOrGroup();
    }

    private static boolean addWaypoint(Waypoint waypoint) {
        try {
            api.addWaypoint(ImmersiveDepositScanner.MOD_ID, waypoint);
            return true;
        } catch (Exception exception) {
            ImmersiveDepositScanner.LOGGER.warn("Unable to add JourneyMap waypoint {}", waypoint.getGuid(), exception);
            return false;
        }
    }

    private static void remove(Displayable displayable) {
        try {
            api.remove(displayable);
        } catch (Exception exception) {
            ImmersiveDepositScanner.LOGGER.warn("Unable to remove JourneyMap display {}", displayable.getGuid(), exception);
        }
    }

    private static void removeWaypoint(Waypoint waypoint) {
        try {
            api.removeWaypoint(ImmersiveDepositScanner.MOD_ID, waypoint);
        } catch (Exception exception) {
            ImmersiveDepositScanner.LOGGER.warn("Unable to remove JourneyMap waypoint {}", waypoint.getGuid(), exception);
        }
    }

    private static void removeAll() {
        try {
            api.removeAll(ImmersiveDepositScanner.MOD_ID);
        } catch (Exception exception) {
            ImmersiveDepositScanner.LOGGER.warn("Unable to remove all JourneyMap displays for {}", ImmersiveDepositScanner.MOD_ID, exception);
        }
    }

    private static void removeAllWaypoints() {
        try {
            api.removeAllWaypoints(ImmersiveDepositScanner.MOD_ID);
        } catch (Exception exception) {
            ImmersiveDepositScanner.LOGGER.warn("Unable to remove all JourneyMap waypoints for {}", ImmersiveDepositScanner.MOD_ID, exception);
        }
    }

    private static void removeLegacyWaypointsByOwnerOrGroup() {
        try {
            java.util.Collection<? extends Waypoint> waypoints = api.getAllWaypoints();
            if (waypoints == null) {
                return;
            }
            for (Waypoint waypoint : new java.util.ArrayList<>(waypoints)) {
                removeLegacyWaypointIfIdsOwned(waypoint);
            }
        } catch (Exception exception) {
            ImmersiveDepositScanner.LOGGER.warn("Unable to enumerate JourneyMap waypoints for IDS legacy cleanup", exception);
        }
    }

    private static void removeLegacyWaypointIfIdsOwned(Waypoint waypoint) {
        try {
            if (isIdsOwnedWaypoint(waypoint)) {
                removeWaypoint(waypoint);
            }
        } catch (Exception exception) {
            ImmersiveDepositScanner.LOGGER.warn("Unable to inspect JourneyMap waypoint for IDS legacy cleanup", exception);
        }
    }

    private static boolean isIdsOwnedWaypoint(Waypoint waypoint) {
        if (waypoint == null) {
            return false;
        }
        if (ImmersiveDepositScanner.MOD_ID.equals(waypoint.getModId())) {
            return true;
        }
        if (hasIdsCustomData(waypoint)) {
            return true;
        }
        return isIdsWaypointGroup(waypoint.getGroupId());
    }

    private static boolean hasIdsCustomData(Waypoint waypoint) {
        try {
            String stableId = waypoint.getCustomData(IDS_KEY_CUSTOM_DATA);
            return stableId != null && !stableId.isBlank();
        } catch (Exception exception) {
            return false;
        }
    }

    private static boolean isIdsWaypointGroup(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return false;
        }
        try {
            WaypointGroup group = api.getWaypointGroup(groupId);
            return group != null
                    && ImmersiveDepositScanner.MOD_ID.equals(group.getModId())
                    && IDS_GROUP_NAMES.contains(group.getName());
        } catch (Exception exception) {
            return false;
        }
    }

    private static void createAndAddWaypoint(TrackedDeposit deposit) {
        try {
            Waypoint waypoint = JourneyMapMarkerFactory.createWaypoint(deposit);
            if (addWaypoint(waypoint)) {
                WAYPOINTS.put(deposit.key(), waypoint);
            }
        } catch (Exception exception) {
            logCreationFailure("waypoint", deposit, exception);
        }
    }

    private static void createAndShowMarker(TrackedDeposit deposit) {
        try {
            MarkerOverlay marker = JourneyMapMarkerFactory.createMarker(deposit);
            trackIfShown(MARKERS, deposit.key(), marker);
        } catch (Exception exception) {
            logCreationFailure("marker", deposit, exception);
        }
    }

    private static void createAndShowOverlay(TrackedDeposit deposit) {
        try {
            PolygonOverlay overlay = JourneyMapOverlayFactory.createOverlay(deposit);
            trackIfShown(OVERLAYS, deposit.key(), overlay);
        } catch (Exception exception) {
            logCreationFailure("chunk overlay", deposit, exception);
        }
    }

    private static <T extends Displayable> void trackIfShown(Map<TrackedDepositKey, T> displays, TrackedDepositKey key, T displayable) {
        if (displayable == null) {
            ImmersiveDepositScanner.LOGGER.warn("JourneyMap display creation returned null for deposit {}", key.stableId());
            return;
        }
        if (show(displayable)) {
            displays.put(key, displayable);
        }
    }

    private static boolean show(Displayable displayable) {
        try {
            api.show(displayable);
            return true;
        } catch (Exception exception) {
            ImmersiveDepositScanner.LOGGER.warn("Unable to show JourneyMap display {}", displayable.getGuid(), exception);
            return false;
        }
    }

    private static void logCreationFailure(String displayType, TrackedDeposit deposit, Exception exception) {
        ImmersiveDepositScanner.LOGGER.warn(
                "Unable to create JourneyMap {} for deposit {} ({})",
                displayType,
                deposit.key().stableId(),
                deposit.displayName(),
                exception
        );
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
}
