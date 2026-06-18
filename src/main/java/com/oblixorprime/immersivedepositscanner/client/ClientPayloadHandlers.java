package com.oblixorprime.immersivedepositscanner.client;

import com.oblixorprime.immersivedepositscanner.network.payload.DepositClearPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.DepositRemovePayload;
import com.oblixorprime.immersivedepositscanner.network.payload.DepositUpsertPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncBatchPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncEndPayload;
import com.oblixorprime.immersivedepositscanner.network.payload.FullSyncStartPayload;

public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    public static void handle(FullSyncStartPayload payload) {
        ClientDepositCache.beginSync(payload.syncId(), payload.expectedDeposits());
    }

    public static void handle(FullSyncBatchPayload payload) {
        ClientDepositCache.acceptBatch(payload.syncId(), payload.deposits());
    }

    public static void handle(FullSyncEndPayload payload) {
        ClientDepositCache.finishSync(payload.syncId());
    }

    public static void handle(DepositUpsertPayload payload) {
        ClientDepositCache.upsert(payload.deposit());
    }

    public static void handle(DepositRemovePayload payload) {
        ClientDepositCache.remove(payload.key());
    }

    public static void handle(DepositClearPayload payload) {
        ClientDepositCache.clear();
    }
}
