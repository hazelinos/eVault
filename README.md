# eFootball AUTH Manager — Android

Prototype untuk mengelola file `online_user_id_data.dat` milik akun sendiri.

## Struktur Library

Contoh:

/storage/emulated/0/eFootball AUTH/
  001 Messi BigTime/
    online_user_id_data.dat
  002 Ronaldo/
    online_user_id_data.dat
  003 Neymar/
    online_user_id_data.dat

## Target

Saat pertama kali dipakai, pilih folder:

`/storage/673C-008B/data/files/SaveData/AUTH/`

Aplikasi akan menulis:

`online_user_id_data.dat`

ke folder tersebut.

## Mode

- SWITCH: memasang akun yang dipilih lalu membuka package `epl.idsphone`.
- AUTO 1→100: mengurutkan folder berdasarkan angka awal nama folder, memasang satu per satu, membuka game, lalu menunggu delay yang diatur.

## Catatan Android

Aplikasi memakai Storage Access Framework (SAF). Pada beberapa versi Android/file provider, folder tertentu di storage eksternal tidak dapat dipilih atau ditulis oleh aplikasi biasa. Jika folder target tidak bisa dipilih/ditulis melalui picker, aplikasi tidak dapat memaksa permission tersebut.

Package target game diset ke:

`epl.idsphone`

Jika package clone berbeda, ubah konstanta `PACKAGE` di `MainActivity.java`.

## Build

Buka folder ini di Android Studio dan lakukan Gradle Sync, lalu Build APK.

Proyek ini sengaja tidak membaca atau mengurai isi file AUTH; file diperlakukan sebagai data biner dan hanya disalin.
