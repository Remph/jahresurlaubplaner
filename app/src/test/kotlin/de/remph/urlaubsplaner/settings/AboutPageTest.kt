package de.remph.urlaubsplaner.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.remph.urlaubsplaner.core.schoolholidays.SchoolHolidayData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The about page carries what the licences of the data and the libraries ask us to show. */
@RunWith(AndroidJUnit4::class)
class AboutPageTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show() {
        compose.setContent { AboutPage(PaddingValues(), onBack = {}) }
    }

    @Test
    @Config(qualifiers = "de")
    fun `German page shows the version, the privacy sentence and the ODbL credit`() {
        show()

        compose.onNodeWithText("Über die App").assertIsDisplayed()
        compose.onNodeWithText("Version ", substring = true).assertIsDisplayed()
        compose.onNodeWithText("keinen Internetzugriff", substring = true).assertIsDisplayed()
        compose.onNodeWithText(SchoolHolidayData.CREDIT).performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun `English page shows the same facts`() {
        show()

        compose.onNodeWithText("Version ", substring = true).assertIsDisplayed()
        compose.onNodeWithText("no internet access", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Libraries").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun `every licence text is bundled and opens`() {
        show()

        compose.onNodeWithText("GPL-3.0 (this app)").performScrollTo().performClick()
        compose.onNodeWithText("GNU GENERAL PUBLIC LICENSE", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()

        compose.onNodeWithText("Apache License 2.0").performScrollTo().performClick()
        compose.onNodeWithText("TERMS AND CONDITIONS FOR USE", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()

        compose.onNodeWithText("BSD-3-Clause (Protocol Buffers)").performScrollTo().performClick()
        compose.onNodeWithText("Copyright 2008 Google Inc.", substring = true).assertIsDisplayed()
    }
}
