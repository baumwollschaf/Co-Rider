# Co-Rider – Play Store release

This checklist contains no passwords, private keys, or machine-specific signing data.

## Permanent identifiers

- App name: `Co-Rider`
- Application ID/package name: `com.ralphdietrich.corider`
- Current release: version name `1.0.1`, version code `3`
- Minimum Android version: API 24
- Target Android version: API 37
- Distribution format: signed Android App Bundle (`.aab`)

The package name must not be changed after the app has been created in Play Console.
Every subsequent upload must use a higher version code.

The first accepted production-draft bundle used version code 1. Version code 2 added
the required in-app privacy-policy link. Version code 3 is the `1.0.1` maintenance
release with clearer GPX import errors and more robust cancellation handling.

## Release notes for 1.0.1

Improved GPX import feedback and minor stability fixes. Added quick access to the
125ccm Motorräder Berlin community.

## 1. Create the app in Play Console

From the Play Console app list, select **Create app** and use:

- App name: `Co-Rider`
- Default language: English (`en-US`) unless another language is deliberately chosen for the store listing
- App or game: App
- Free or paid: Free

Accept the required declarations and create the app. When the 2026 Android developer
verification flow asks for a package name, use `com.ralphdietrich.corider`.

If Play Console reports that the package name has already been used or asks for proof
of key ownership, stop and resolve that prompt before changing the package name or
creating a production key.

## 2. Create the upload key once

In Android Studio:

1. Select **Build > Generate Signed App Bundle or APK**.
2. Select **Android App Bundle**.
3. Select the `app` module and choose **Create new** below the keystore field.
4. Store the key outside the repository, for example:
   `C:\Users\<name>\AndroidKeys\co-rider-upload.jks`
5. Recommended alias: `co-rider-upload`.
6. Use a strong password and the same password for the keystore and key.
7. Set validity to at least 25 years; 30 years is a suitable choice.
8. Enter the certificate owner information. This information is part of the certificate,
   but is not displayed inside the app.

Keep an encrypted offline backup of the `.jks` file and its password. Do not place the
key in this repository, cloud source control, chat, screenshots, or issue trackers.

For a new Play app, use Play App Signing with a Google-managed app signing key. The
local key above then serves only as the upload key and can be reset through Play Console
if it is ever lost or compromised.

## 3. Connect the local key to Gradle

Copy `keystore.properties.example` to `keystore.properties` in the repository root and
replace all placeholders. Use the absolute path of the `.jks` file. This local file is
ignored by Git.

When `keystore.properties` exists, Gradle automatically signs the `release` build. When
it does not exist, normal development builds continue to work and the release output is
unsigned.

## 4. Build and verify the release bundle

Run:

```powershell
$env:GRADLE_USER_HOME='.gradle'
.\gradlew.bat bundleRelease
```

Expected output:

`app\build\outputs\bundle\release\app-release.aab`

Before upload, verify that the bundle exists and is signed. Never stage the generated
`.aab`; build artifacts are ignored by Git.

## 5. Upload to an internal test

In Play Console, finish the required app setup and then create an internal testing
release. Upload the signed `.aab`, accept Play App Signing for the new app, add testers,
and install the Play-delivered build from the tester link.

Test at minimum:

- GPX import through the Android document picker
- restoration of the active route after an app restart
- route details and elevation profile in portrait and landscape
- `Open in Google Maps`
- at least one short and one complex real route
- behavior on a physical phone and with Honda RoadSync

## 6. Complete the Play listing and policy forms

Required work includes the main store listing, screenshots, feature graphic, app icon,
support contact, privacy policy URL, Data safety form, content rating, target audience,
ads declaration, app access declaration, countries/regions, and pricing.

Privacy policy URL: <https://baumwollschaf.github.io/co-rider-legal/>

Co-Rider is free, contains no ads, has no account system, and has no backend. GPX data
is stored locally. The exact Data safety answers and privacy policy must still describe
the deliberate transfer of route coordinates to the separately installed Google Maps
app when the user selects `Open in Google Maps`.

## 7. Production release

Only promote the tested bundle after all Play Console checks are complete. If Play
Console applies the testing requirement for newer personal developer accounts, complete
the required closed test before applying for production access.
