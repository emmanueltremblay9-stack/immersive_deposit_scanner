package com.oblixorprime.immersivedepositscanner.network;

import com.oblixorprime.immersivedepositscanner.ImmersiveDepositScanner;
import com.oblixorprime.immersivedepositscanner.network.payload.DepositClearPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.DepositRemovePayload;
import com.oblixorprime.immersivedepositscanner.network.payload.DepositUpsertPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncBatchPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncEndPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncStartPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.RequestResyncPayload;
import com.oblixorprime.immersivedepositscanner.tracking.DepositTrackingService;
import java.lang.reflect.InvocationTargetException;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler {
    private static final String CLIENT_PAYLOAD_HANDLERS_CLASS =
            "com.oblixorprime.immersivedepositscanner.client.ClientPayloadHandlers";
    private static final String PROTOCOL_VERSION = "1";

    private NetworkHandler() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ImmersiveDepositScanner.MOD_ID).versioned(PROTOCOL_VERSION);
        registrar.playToClient(FullSyncStartPayload.TYPE, FullSyncStartPayload.STREAM_CODEC, NetworkHandler::handleFullSyncStart);
        registrar.playToClient(FullSyncBatchPayload.TYPE, FullSyncBatchPayload.STREAM_CODEC, NetworkHandler::handleFullSyncBatch);
        registrar.playToClient(FullSyncEndPayload.TYPE, FullSyncEndPayload.STREAM_CODEC, NetworkHandler::handleFullSyncEnd);
        registrar.playToClient(DepositUpsertPayload.TYPE, DepositUpsertPayload.STREAM_CODEC, NetworkHandler::handleDepositUpsert);
        registrar.playToClient(DepositRemovePayload.TYPE, DepositRemovePayload.STREAM_CODEC, NetworkHandler::handleDepositRemove);
        registrar.playToClient(DepositClearPayload.TYPE, DepositClearPayload.STREAM_CODEC, NetworkHandler::handleDepositClear);
        registrar.playToServer(RequestResyncPayload.TYPE, RequestResyncPayload.STREAM_CODEC, NetworkHandler::handleRequestResync);
    }

    public static void sendToPlayer(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    private static void handleFullSyncStart(FullSyncStartPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> dispatchClientPayload(payload));
    }

    private static void handleFullSyncBatch(FullSyncBatchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> dispatchClientPayload(payload));
    }

    private static void handleFullSyncEnd(FullSyncEndPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> dispatchClientPayload(payload));
    }

    private static void handleDepositUpsert(DepositUpsertPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> dispatchClientPayload(payload));
    }

    private static void handleDepositRemove(DepositRemovePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> dispatchClientPayload(payload));
    }

    private static void handleDepositClear(DepositClearPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> dispatchClientPayload(payload));
    }

    private static void handleRequestResync(RequestResyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                DepositTrackingService.sendFullSync(serverPlayer);
            }
        });
    }

    private static void dispatchClientPayload(Object payload) {
        try {
            Class<?> handlers = Class.forName(CLIENT_PAYLOAD_HANDLERS_CLASS);
            handlers.getMethod("handle", payload.getClass()).invoke(null, payload);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocationTargetException
                    && invocationTargetException.getCause() != null
                    ? invocationTargetException.getCause()
                    : exception;
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Unable to dispatch IDS client payload " + payload.getClass().getName(), cause);
        }
    }
}
