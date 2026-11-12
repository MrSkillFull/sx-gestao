package com.seuxico.sxgestao

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationOpensAllScreensAndSurvivesRecreation() {
        val screens = listOf(
            R.string.inicio to R.string.inicio_description,
            R.string.atendimentos to R.string.atendimentos_description,
            R.string.cadastros to R.string.cadastros_description,
            R.string.configuracoes to R.string.configuracoes_description,
            R.string.inicio to R.string.inicio_description,
        )
        screens.forEach { (label, description) ->
            compose.onNode(isSelectable() and hasText(compose.activity.getString(label)))
                .performClick().assertIsSelected()
            compose.onNodeWithText(compose.activity.getString(description)).assertIsDisplayed()
        }
        compose.onNode(isSelectable() and hasText(compose.activity.getString(R.string.cadastros)))
            .performClick().assertIsSelected()
        compose.onNodeWithText(compose.activity.getString(R.string.cadastros_description))
            .assertIsDisplayed()
        // Recreate only after Compose has committed the selected destination and saved state.
        compose.waitForIdle()
        compose.activityRule.scenario.recreate()
        // ActivityScenario waits for the lifecycle; Compose must also settle its new composition.
        compose.waitForIdle()
        compose.onNode(isSelectable() and hasText(compose.activity.getString(R.string.cadastros)))
            .assertIsSelected()
        compose.onNodeWithText(compose.activity.getString(R.string.cadastros_description))
            .assertIsDisplayed()
    }
}
