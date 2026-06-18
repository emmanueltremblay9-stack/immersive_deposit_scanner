# Network Protocol

Payloads use NeoForge 1.21.1 `CustomPacketPayload`, `RegisterPayloadHandlersEvent`, `PayloadRegistrar`, and `StreamCodec`.

Payloads:

- `FullSyncStartPayload`
- `FullSyncBatchPayload`
- `FullSyncEndPayload`
- `DepositUpsertPayload`
- `DepositRemovePayload`
- `DepositClearPayload`
- `RequestResyncPayload`

Full syncs include a UUID session id and an expected deposit count. Clients ignore batches and end packets that do not match the active sync id, and they reject incomplete syncs instead of replacing the current cache with partial data.
