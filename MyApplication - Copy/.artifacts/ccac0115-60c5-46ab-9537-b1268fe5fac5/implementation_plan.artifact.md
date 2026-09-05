# Project Renaming to 'Raven'

This plan details the steps to rename the project and its package structure from `com.example.myapplication` to `com.raven.application`, and update all app name references to "Raven".

## Proposed Changes

### [1] Project & Build Configuration
Update the root project name and the application's unique identifier.
*   #### [MODIFY] [settings.gradle.kts](file:///D:/Project/MyApplication%20-%20Copy/settings.gradle.kts)
    *   Change `rootProject.name` to `"Raven"`.
*   #### [MODIFY] [app/build.gradle.kts](file:///D:/Project/MyApplication%20-%20Copy/app/build.gradle.kts)
    *   Update `namespace` to `"com.raven.application"`.
    *   Update `applicationId` to `"com.raven.application"`.

### [2] Resource Files
Update user-facing strings and theme names.
*   #### [MODIFY] [strings.xml](file:///D:/Project/MyApplication%20-%20Copy/app/src/main/res/values/strings.xml)
    *   Change `app_name` to `"Raven"`.
*   #### [MODIFY] [AndroidManifest.xml](file:///D:/Project/MyApplication%20-%20Copy/app/src/main/AndroidManifest.xml)
    *   (Optional but recommended) Update theme references if they contain "MyApplication".

### [3] Source Code Package Renaming
Update the package declaration in all files and fix imports.
*   #### [MODIFY] ALL Source Files
    *   Update `package com.example.myapplication...` to `package com.raven.application...`.
    *   Update internal imports to use `com.raven.application`.
*   #### [MODIFY] [Theme.kt](file:///D:/Project/MyApplication%20-%20Copy/app/src/main/java/com/example/myapplication/ui/theme/Theme.kt)
    *   Rename `MyApplicationTheme` to `RavenTheme`.
*   #### [MODIFY] [MainActivity.kt](file:///D:/Project/MyApplication%20-%20Copy/app/src/main/java/com/example/myapplication/MainActivity.kt)
    *   Update OSM User-Agent and UI labels ("survival_mesh", "SURVIVAL_MESH") to "Raven".

### [4] Directory Structure
(Note: File movement will be simulated by updating the content of the files. The physical directory structure should ideally be updated by the user in the IDE for best results, or I will use `run_shell_command` to move them if approved.)

## User Review Required

> [!IMPORTANT]
> Renaming the package name and directory structure is a major change. I will update all code references, but if you have any uncommitted changes, please back them up.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to verify that the project still compiles with the new package name.
- Check `BuildConfig` and `R` file generation.

### Manual Verification
- Deploy the app to a device and verify the new app name ("Raven") appears on the launcher.
- Verify that the OSM map still loads (User-Agent check).
