# Immersive Integration

Immersive Engineering core samples are read through the 1.21.1 data component `IEDataComponents.CORESAMPLE`, whose value is `CoresampleItem.ItemData`.

Immersive Petroleum survey data is read through `ISurveyInfo.from(stack)`. When a core sample provides coordinates, the server may also query `ReservoirHandler.getReservoir(level, samplePosition)` to capture current reservoir amount, capacity, fluid, and depletion state exposed by Immersive Petroleum.

All reads happen server-side. Clients cannot submit arbitrary deposit coordinates.

