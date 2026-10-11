package com.odom.docreader.feature.settings

/**
 * One row of the open-source licenses screen.
 *
 * @param licenseAsset file under `assets/licenses/` holding the full text shown when the row is opened
 */
data class OpenSourceLibrary(
    val name: String,
    val license: String,
    val copyright: String,
    val licenseAsset: String,
)

private const val APACHE_2 = "Apache License 2.0"
private const val APACHE_2_ASSET = "Apache-2.0.txt"

/**
 * Everything third-party that ships in the APK. Gradle dependencies are grouped by publisher; the
 * JavaScript, WebAssembly and font files bundled in assets are listed individually with versions.
 * Keep in step with docs/THIRD_PARTY.md.
 */
internal val OPEN_SOURCE_LIBRARIES = listOf(
    OpenSourceLibrary(
        name = "rhwp (@rhwp/core 0.8.7)",
        license = "MIT License",
        copyright = "Copyright (c) 2025-2026 Edward Kim",
        licenseAsset = "rhwp-MIT.txt",
    ),
    OpenSourceLibrary(
        name = "rhwp — bundled Rust crates",
        license = "MIT / Apache-2.0 / BSD and compatible",
        copyright = "See the list for each crate",
        licenseAsset = "rhwp-THIRD_PARTY_LICENSES.md",
    ),
    OpenSourceLibrary(
        name = "docx-preview 0.4.1",
        license = APACHE_2,
        copyright = "Copyright Volodymyr Baydalka",
        licenseAsset = APACHE_2_ASSET,
    ),
    OpenSourceLibrary(
        name = "JSZip 3.10.2",
        license = "MIT License",
        copyright = "Copyright (c) 2009-2016 Stuart Knightley, David Duponchel, Franz Buchinger, António Afonso",
        licenseAsset = "jszip-MIT.txt",
    ),
    OpenSourceLibrary(
        name = "Nanum Myeongjo (나눔명조)",
        license = "SIL Open Font License 1.1",
        copyright = "Copyright (c) 2010, NHN Corporation",
        licenseAsset = "NanumMyeongjo-OFL.txt",
    ),
    OpenSourceLibrary(
        name = "AndroidX (Core, Activity, Compose, Lifecycle, Navigation, Fragment, Room, DataStore, Hilt, WebKit, PDF)",
        license = APACHE_2,
        copyright = "Copyright The Android Open Source Project",
        licenseAsset = APACHE_2_ASSET,
    ),
    OpenSourceLibrary(
        name = "Material Components for Android, Material Icons",
        license = APACHE_2,
        copyright = "Copyright Google LLC",
        licenseAsset = APACHE_2_ASSET,
    ),
    OpenSourceLibrary(
        name = "Dagger, Hilt",
        license = APACHE_2,
        copyright = "Copyright The Dagger Authors",
        licenseAsset = APACHE_2_ASSET,
    ),
    OpenSourceLibrary(
        name = "Kotlin standard library, kotlinx.coroutines, kotlinx.serialization, JetBrains annotations",
        license = APACHE_2,
        copyright = "Copyright JetBrains s.r.o. and Kotlin Programming Language contributors",
        licenseAsset = APACHE_2_ASSET,
    ),
    OpenSourceLibrary(
        name = "Okio",
        license = APACHE_2,
        copyright = "Copyright Square, Inc.",
        licenseAsset = APACHE_2_ASSET,
    ),
    OpenSourceLibrary(
        name = "Guava ListenableFuture, Error Prone annotations, JSR-305, JSpecify, javax.inject, jakarta.inject",
        license = APACHE_2,
        copyright = "Copyright their respective authors",
        licenseAsset = APACHE_2_ASSET,
    ),
)
