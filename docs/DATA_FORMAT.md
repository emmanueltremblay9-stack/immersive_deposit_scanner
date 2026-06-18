# Data Format

Saved data file: `immersive_deposit_scanner.dat`.

The file stores:

- `formatVersion`
- `deposits[]`
- dimension id
- chunk coordinates
- source
- kind
- stable deposit id
- display name
- optional resource id
- optional sample position
- owner UUID
- discovery and update timestamps
- optional current amount
- optional maximum amount
- optional remaining percentage
- depleted flag
- `knownBy[]` player UUIDs for personal visibility

Format version `1` is the initial version. Format version `2` adds `knownBy[]`; older entries without this field fall back to the original owner UUID. Unknown future versions are loaded conservatively by reading fields that still match the current schema.
