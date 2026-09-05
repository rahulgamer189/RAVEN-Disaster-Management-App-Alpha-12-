# Project Successfully Renamed to 'Raven'

The Survival Mesh application has been fully rebranded to **Raven**. This involved a complete package rename from `com.example.myapplication` to `com.raven.application` and updating all internal references.

## Changes Overview

### 1. Brand Identity
- **App Name**: Renamed to "Raven" in `strings.xml`.
- **UI Labels**: All dashboard and chat headers now display "RAVEN" instead of "SURVIVAL_MESH".
- **Map Identification**: The OpenStreetMap User-Agent has been updated to `Raven/1.1`.

### 2. Technical Refactoring
- **Package Name**: Globally updated to `com.raven.application`.
- **Directory Structure**: Physically moved source files from `com/example/myapplication` to `com/raven/application` for `main`, `test`, and `androidTest` source sets.
- **Theme Renaming**: `MyApplicationTheme` was renamed to `RavenTheme` to stay consistent with the new naming convention.
- **Gradle Config**: `namespace` and `applicationId` in `app/build.gradle.kts` now point to `com.raven.application`.

### 3. Connection & Mesh
- **Bluetooth Service**: The RFCOMM service record name was changed to "Raven".
- **Mesh Logic**: All internal message relay logic and camp broadcasting remain fully functional under the new package structure.

## Verification Results
- **Gradle Sync**: Successful.
- **Build Status**: `assembleDebug` completed without errors.
- **Package Integrity**: No remaining references to `com.example.myapplication` found in active source code.
