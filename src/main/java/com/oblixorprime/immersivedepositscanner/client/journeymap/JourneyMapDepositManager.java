package com.oblixorprime.immersivedepositscanner.client.journeymap;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.client.ClientDepositCache;
import com.oblixorprime.immersivedepositscanner.config.ClientConfig;
import com.oblixorprime.immersivedepositscanner.data.TrackedDeposit;
import com.oblixorprime.immersivedepositscanner.data.TrackedDepositKey;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.common.waypoint.Waypoint;

public final class JourneyMapDepositManager {
    private static final Map<TrackedDepositKey, MarkerOverlay> MARKERS = new LinkedHashMap<>();
    private static final Map<TrackedDepositKey, PolygonOverlay> OVERLAYS = new LinkedHashMap<>();
    private static final Map<TrackedDepositKey, Waypoint> WAYPOINTS = new LinkedHashMap<>();
    private static IClientAPI api;

    private JourneyMapDepositManager() {
    }

    public static void initialize(IClientAPI clientApi) {
        api = clientApi;
        ImmersiveDepositScanner.LOGGER.info("JourneyMap API initialized for {}", ImmersiveDepositScanner.MOD_NAME);
        replaceAll(ClientDepositCache.getAll());
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
        if (JourneyMapMarkerFactory.shouldShow(deposit)) {
            if (ClientConfig.ENABLE_WAYPOINTS.get()) {
                Waypoint waypoint = JourneyMapMarkerFactory.createWaypoint(deposit);
                WAYPOINTS.put(deposit.key(), waypoint);
                api.addWaypoint(ImmersiveDepositScanner.MOD_ID, waypoint);
            }
            MarkerOverlay marker = JourneyMapMarkerFactory.createMarker(deposit);
            MARKERS.put(deposit.key(), marker);
            show(marker);
        }
        if (ClientConfig.ENABLE_CHUNK_OVERLAYS.get() && JourneyMapOverlayFactory.shouldShow(deposit)) {
            PolygonOverlay overlay = JourneyMapOverlayFactory.createOverlay(deposit);
            OVERLAYS.put(deposit.key(), overlay);
            show(overlay);
        }
    }

    public static void remove(TrackedDepositKey key) {
        if (api == null) {
            return;
        }
        MarkerOverlay marker = MARKERS.remove(key);
        if (marker != null) {
            api.remove(marker);
        }
        PolygonOverlay overlay = OVERLAYS.remove(key);
        if (overlay != null) {
            api.remove(overlay);
        }
        Waypoint waypoint = WAYPOINTS.remove(key);
        if (waypoint != null) {
            api.removeWaypoint(ImmersiveDepositScanner.MOD_ID, waypoint);
        }
    }

    public static void clearAll() {
        if (api == null) {
            MARKERS.clear();
            OVERLAYS.clear();
            WAYPOINTS.clear();
            return;
        }
        MARKERS.values().forEach(api::remove);
        OVERLAYS.values().forEach(api::remove);
        WAYPOINTS.values().forEach(waypoint -> api.removeWaypoint(ImmersiveDepositScanner.MOD_ID, waypoint));
        MARKERS.clear();
        OVERLAYS.clear();
        WAYPOINTS.clear();
        api.removeAll(ImmersiveDepositScanner.MOD_ID);
        api.removeAllWaypoints(ImmersiveDepositScanner.MOD_ID);
    }

    private static void show(journeymap.api.v2.client.display.Displayable displayable) {
        try {
            api.show(displayable);
        } catch (Exception exception) {
            ImmersiveDepositScanner.LOGGER.warn("Unable to show JourneyMap display {}", displayable.getGuid(), exception);
        }
    }
}
