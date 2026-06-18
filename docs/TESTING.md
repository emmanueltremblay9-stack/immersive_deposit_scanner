# Testing

Primary build gate:

```powershell
.\gradlew.bat clean build
```

Additional lightweight QA:

```powershell
.\gradlew.bat qaDataModel
```

Server runtime smoke gate:

```powershell
.\gradlew.bat runGameTestServer
```

Runtime validation should cover:

- client with JourneyMap, Immersive Engineering, and Immersive Petroleum installed
- dedicated server startup with all required dependencies
- expected NeoForge dependency failure when any required dependency is absent
- held-sample scan
- full sync after login and `/mst resync`
- JourneyMap marker cleanup when disconnecting or changing servers
