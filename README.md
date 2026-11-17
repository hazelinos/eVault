# eVault

**eVault** is a lightweight Android utility for managing eFootball AUTH files (`online_user_id_data.dat`) locally.

> Current release: **v1.4.1**

## Overview

eVault keeps your AUTH library organized and makes switching between accounts straightforward. AUTH files are treated as opaque binary data; eVault copies them without parsing or modifying their contents.

## Core capabilities

- **AUTH Library** — organize accounts by category.
- **Switch Account** — copy a selected AUTH into the configured eFootball folder and launch the game.
- **Create Accounts** — create multiple library account folders using the built-in naming flow.
- **Save AUTH** — save the current AUTH from the target folder back into the library.
- **Delete Data** — remove AUTH from the configured target folder.
- **Auto Login** — process all accounts, unverified accounts, or a selected account.
- **Login Verification** — record successful logins for the current UTC date.
- **Activity Log** — keep a local history of eVault actions.
- **Auto-detect clone** — MASUK EFOOTBALL launches the first installed eFootball package that is NOT the original (`epl.idsphone`). Recognizes the "ids" package family (`ucl.idsphone`, `idspatch`, …). No picker, no manual package input.
- **Persistent Settings** — keep the Auto Login delay between app launches.

## Save AUTH flow

**Save AUTH** stores the AUTH currently installed in the configured eFootball target folder. If no AUTH is installed, eVault shows a message and does not start the save flow. Otherwise, eVault asks for a category or a new category, then asks for the account name. If the name already exists in that category, the same naming dialog stays open so the name can be changed without restarting the flow.

## Library structure

~~~text
eFootball AUTH/
└── Category/
    ├── Account 1/
    │   └── online_user_id_data.dat
    ├── Account 2/
    │   └── online_user_id_data.dat
    └── Account 3/
        └── online_user_id_data.dat
~~~

## Requirements

- Android **8.0 (API 26)** or newer.
- eFootball installed on the device.
- Access to the AUTH library and target folders through Android Storage Access Framework.

Default game package:
~~~text
epl.idsphone
~~~

## Storage permissions

eVault uses Android **Storage Access Framework (SAF)**. Some Android versions restrict specific external-storage locations; eVault cannot bypass restrictions imposed by the system picker.

## Build

Open the project in Android Studio and sync Gradle. For a local debug APK:
~~~bash
./gradlew assembleDebug
~~~

The APK is generated under `app/build/outputs/apk/debug/`.

## Project structure

~~~text
eVault/
├── app/
├── signing/
├── .github/workflows/
├── README.md
├── CHANGELOG.md
└── settings.gradle.kts
~~~

## Release

Releases are built automatically from `main` with GitHub Actions. Artifacts use:
~~~text
eVault-vX.Y.Z.apk
~~~

## Privacy

eVault does not inspect or decode `online_user_id_data.dat`. The file is handled as binary data and copied between locations selected by the user.

## Development

The project intentionally keeps the Android implementation lightweight and dependency-minimal. Core AUTH and Auto Login behavior should be changed incrementally and tested before adding new integrations.
