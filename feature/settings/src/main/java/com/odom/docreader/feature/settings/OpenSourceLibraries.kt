package com.odom.docreader.feature.settings

import androidx.annotation.StringRes

/**
 * One row of the open-source licenses screen.
 *
 * @param licenseAsset file under `assets/licenses/` holding the full text shown when the row is opened
 */
data class OpenSourceLibrary(
    @param:StringRes val name: Int,
    @param:StringRes val license: Int,
    @param:StringRes val copyright: Int,
    val licenseAsset: String,
)

private const val APACHE_2_ASSET = "Apache-2.0.txt"

/**
 * Everything third-party that ships in the APK. Gradle dependencies are grouped by publisher; the
 * JavaScript, WebAssembly and font files bundled in assets are listed individually with versions.
 * Keep in step with docs/THIRD_PARTY.md.
 */
internal val OPEN_SOURCE_LIBRARIES = listOf(
    OpenSourceLibrary(
        name = R.string.settings_library_rhwp,
        license = R.string.settings_license_mit,
        copyright = R.string.settings_copyright_rhwp,
        licenseAsset = "rhwp-MIT.txt",
    ),
    OpenSourceLibrary(
        name = R.string.settings_library_rhwp_crates,
        license = R.string.settings_license_rhwp_crates,
        copyright = R.string.settings_copyright_rhwp_crates,
        licenseAsset = "rhwp-THIRD_PARTY_LICENSES.md",
    ),
    OpenSourceLibrary(
        name = R.string.settings_library_docx_preview,
        license = R.string.settings_license_apache_2,
        copyright = R.string.settings_copyright_docx_preview,
        licenseAsset = APACHE_2_ASSET,
    ),
    OpenSourceLibrary(
        name = R.string.settings_library_jszip,
        license = R.string.settings_license_mit,
        copyright = R.string.settings_copyright_jszip,
        licenseAsset = "jszip-MIT.txt",
    ),
    OpenSourceLibrary(
        name = R.string.settings_library_nanum_myeongjo,
        license = R.string.settings_license_ofl,
        copyright = R.string.settings_copyright_nanum_myeongjo,
        licenseAsset = "NanumMyeongjo-OFL.txt",
    ),
    OpenSourceLibrary(
        name = R.string.settings_library_androidx,
        license = R.string.settings_license_apache_2,
        copyright = R.string.settings_copyright_androidx,
        licenseAsset = APACHE_2_ASSET,
    ),
    OpenSourceLibrary(
        name = R.string.settings_library_material,
        license = R.string.settings_license_apache_2,
        copyright = R.string.settings_copyright_material,
        licenseAsset = APACHE_2_ASSET,
    ),
    OpenSourceLibrary(
        name = R.string.settings_library_dagger,
        license = R.string.settings_license_apache_2,
        copyright = R.string.settings_copyright_dagger,
        licenseAsset = APACHE_2_ASSET,
    ),
    OpenSourceLibrary(
        name = R.string.settings_library_kotlin,
        license = R.string.settings_license_apache_2,
        copyright = R.string.settings_copyright_kotlin,
        licenseAsset = APACHE_2_ASSET,
    ),
    OpenSourceLibrary(
        name = R.string.settings_library_okio,
        license = R.string.settings_license_apache_2,
        copyright = R.string.settings_copyright_okio,
        licenseAsset = APACHE_2_ASSET,
    ),
    OpenSourceLibrary(
        name = R.string.settings_library_java_annotations,
        license = R.string.settings_license_apache_2,
        copyright = R.string.settings_copyright_java_annotations,
        licenseAsset = APACHE_2_ASSET,
    ),
)
