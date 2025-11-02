# Troubleshooting and Setup Guide

This project contains three Android apps:
- GroceryCustomer (`GroceryCustomer`)
- GroceryAdmin (`GroceryAdmin`)
- GroceryDelivery (`GroceryDelivery`)

Auxiliary folder:
- grocery-delivery-api (serverless API hosted on Vercel)

All three Android apps are standard Gradle Android application projects with their own `gradlew.bat`, `settings.gradle.kts`, and `app/` module.

---

## Prerequisites (Windows)
- Android SDK + AVDs (typically at `%LOCALAPPDATA%\Android\Sdk`)
- ADB available at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`
- Microsoft OpenJDK 17 (kapt requires JDK 17):
  - Install: `winget install -e --id Microsoft.OpenJDK.17`

Optional but recommended:
- Up-to-date Android SDK Build-Tools and cmdline-tools via Android Studio SDK Manager.

---

## Emulator and ADB
- List devices:
  - PowerShell: `& "$Env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices`
- List AVDs:
  - `& "$Env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -list-avds`
- Start an AVD (example):
  - `& "$Env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Pixel_9a`

Note: Always ensure an emulator is running before `installDebug`.

---

## Clean state installs
We always start from a clean state. Preferred is Gradle’s uninstall+install tasks:
- `uninstallDebug` then `installDebug` (per app).

If needed, ADB package uninstall (may sometimes report internal errors, but Gradle install still succeeds):
- `& "$Env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" uninstall com.grocery.customer`
- `& "$Env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" uninstall com.grocery.admin`
- `& "$Env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" uninstall com.grocery.delivery`

---

## Building with JDK 17 (fixes kapt/JDK 21 issues)
Symptoms when using JDK 21:
- `IllegalAccessError: ... jdk.compiler does not export com.sun.tools.javac.main ...`
- Build fails at `:app:kaptGenerateStubsDebugKotlin`.

Fix:
1) Install Microsoft OpenJDK 17 (see Prerequisites).
2) In the shell before running Gradle:
   - PowerShell:
     - `$env:JAVA_HOME = 'C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot'`
     - `$env:Path = "$env:JAVA_HOME\\bin;$env:Path"`
3) From each app directory, run:
   - `.\u200bgradlew.bat clean installDebug --no-daemon --stacktrace`

Alternative (without modifying PATH):
- Run the wrapper via Java 17 directly:
  - `& 'C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot\\bin\\java.exe' -Xmx64m -Xms64m -classpath '.\\gradle\\wrapper\\gradle-wrapper.jar' org.gradle.wrapper.GradleWrapperMain clean installDebug --no-daemon --stacktrace`

Project configuration already includes:
- `kotlin { jvmToolchain(17) }` in each app’s `app/build.gradle.kts`.
- Toolchain auto-detect/auto-download flags in `gradle.properties`.
- If Gradle still cannot find a toolchain, ensure JDK 17 is installed and/or set `JAVA_HOME` as above.

---

## Per-app build/install commands
Run from each app’s directory.

GroceryCustomer:
- `.\u200bgradlew.bat clean uninstallDebug installDebug --no-daemon`

GroceryAdmin:
- First run may crash the daemon on some machines. If so, delete caches and retry:
  - `Remove-Item -Recurse -Force .\.gradle, .\.kotlin` (PowerShell)
  - Then: `.\u200bgradlew.bat clean installDebug --no-daemon`

GroceryDelivery:
- `.\u200bgradlew.bat clean uninstallDebug installDebug --no-daemon`

---

## Known warnings and their status
- SDK processing warning: “only understands SDK XML versions up to 3 but XML version 4 was encountered.”
  - Cause: mismatch between command-line tools and Android Studio versions.
  - Status: benign; builds still succeed. To remove, update cmdline-tools/Build-Tools from SDK Manager.

---

## Configuration (Supabase / API)
BuildConfig is pre-wired in each app (`app/build.gradle.kts`):
- `SUPABASE_URL` points to the project URL.
- `SUPABASE_ANON_KEY` is set for client access.
- `API_BASE_URL` points to the Vercel deployment `.../api/`.

Do not hardcode service-role keys in client apps. For server-side (Vercel), use environment variables:
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`

---

## Test accounts
- Customer: `abcd@gmail.com` / `Password123`
- Admin: `admin@grocery.com` / `AdminPass123`
- Delivery: `driver@grocery.com` / `DriverPass123`

Use normal authentication flows; do not bypass with mock data.

---

## Quick checklist for new developers
1) Install Microsoft OpenJDK 17; set `JAVA_HOME` (or call Gradle via Java 17 directly).
2) Ensure Android SDK is installed and an AVD is running.
3) For each app, run `clean uninstallDebug installDebug`.
4) If kapt errors appear, verify Java 17 is in use; re-run.
5) If Gradle daemon crashes, delete `.gradle` and `.kotlin` in the app folder and retry.
6) Log in with the provided test accounts and verify end-to-end flows.
