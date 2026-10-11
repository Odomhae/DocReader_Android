package com.odom.docreader.feature.settings

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSourceLibrariesTest {
    // Unit tests run with the module directory as the working directory.
    private val licensesDir = File("src/main/assets/licenses")

    @Test
    fun everyListedLicenseTextIsBundled() {
        OPEN_SOURCE_LIBRARIES.forEach { library ->
            val file = File(licensesDir, library.licenseAsset)
            assertTrue("${library.name}: missing ${file.path}", file.isFile && file.length() > 0)
        }
    }

    @Test
    fun everyBundledLicenseTextIsListed() {
        val listed = OPEN_SOURCE_LIBRARIES.map { it.licenseAsset }.toSet()
        val bundled = licensesDir.listFiles().orEmpty().map { it.name }.toSet()

        assertEquals(listed, bundled)
    }

    @Test
    fun libraryNamesAreUniqueBecauseTheyKeyTheList() {
        val names = OPEN_SOURCE_LIBRARIES.map { it.name }

        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun onlyAllowedLicensesAreUsed() {
        val forbidden = OPEN_SOURCE_LIBRARIES.filter { library ->
            Regex("\\bA?GPL\\b").containsMatchIn(library.license)
        }

        assertTrue("GPL-licensed entries: $forbidden", forbidden.isEmpty())
    }
}
