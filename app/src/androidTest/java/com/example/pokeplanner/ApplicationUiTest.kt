package com.example.pokeplanner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import com.example.pokeplanner.data.local.AppDatabase
import com.example.pokeplanner.work.SyncController
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class ApplicationUiTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var api: TestApi

    @Inject lateinit var sync: SyncController

    @Inject lateinit var database: AppDatabase
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun setup() {
        hilt.inject()
    }

    @After fun close() {
        scenario?.close()
        sync.close()
        database.close()
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }
    private fun waitForTag(tag: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitForText(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun fullCatalogSearchNavigatesToCorrectCardAndSurvivesRecreation() {
        launch()
        waitForTag("pokemon-1")
        compose.onNodeWithTag("search").performTextInput("mewtwo")
        waitForTag("pokemon-150")
        scenario!!.recreate()
        waitForTag("pokemon-150")
        compose.onNodeWithTag("search").assertTextContains("mewtwo")
        compose.onNodeWithTag("pokemon-150").performClick()
        waitForTag("detail-name")
        compose.onNodeWithTag("detail-name").assertTextContains("Mewtwo")
    }

    @Test
    fun errorRetryActuallyLoadsCatalog() {
        api.offline = true
        launch()
        waitForText("Повторить")
        compose.onNodeWithText("Повторить").assertIsDisplayed()
        api.offline = false
        compose.onNodeWithText("Повторить").performClick()
        waitForTag("pokemon-1")
        compose.onNodeWithTag("pokemon-1").assertIsDisplayed()
    }

    @Test
    fun deletingCollectionRequiresConfirmationAndCancelKeepsIt() {
        launch()
        waitForTag("pokemon-1")
        compose.onNodeWithText("Коллекции").performClick()
        compose.onNodeWithText("Создать коллекцию").performClick()
        compose.onNodeWithTag("collection-name").performTextInput("Моя команда")
        compose.onNodeWithText("Сохранить коллекцию").performClick()
        waitForText("Моя команда")
        compose.onNodeWithText("Моя команда").assertIsDisplayed()
        compose.onNodeWithText("Удалить коллекцию").performClick()
        compose.onNodeWithText("Отмена").performClick()
        compose.onNodeWithText("Моя команда").assertIsDisplayed()
        compose.onNodeWithText("Удалить коллекцию").performClick()
        compose.onNodeWithText("Подтвердить удаление").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Моя команда").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun editingCollectionUsesDedicatedDialogAndUpdatesExistingRecord() {
        launch()
        waitForTag("pokemon-1")
        compose.onNodeWithText("Коллекции").performClick()
        compose.onNodeWithText("Создать коллекцию").performClick()
        compose.onNodeWithTag("collection-name").performTextInput("Команда")
        compose.onNodeWithText("Сохранить коллекцию").performClick()
        waitForText("Изменить")
        compose.onNodeWithText("Изменить").performClick()
        waitForText("Редактирование коллекции")
        compose.onNodeWithText("Редактирование коллекции").assertIsDisplayed()
        compose.onNodeWithTag("collection-name").performTextReplacement("Команда 2")
        scenario!!.recreate()
        waitForTag("collection-name")
        compose.onNodeWithText("Редактирование коллекции").assertIsDisplayed()
        compose.onNodeWithTag("collection-name").assertTextContains("Команда 2")
        compose.onNodeWithText("Сохранить коллекцию").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("collection-name").fetchSemanticsNodes().isEmpty()
        }
        waitForText("Команда 2")
        compose.onNodeWithText("Команда 2").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun unmatchedSearchShowsEmptyStateAndClearingRestoresCatalog() {
        launch()
        waitForTag("pokemon-1")
        compose.onNodeWithTag("search").performTextReplacement("no-such-pokemon")
        waitForText("Ничего не найдено. Измените запрос или фильтр.")
        compose.onNodeWithText("Ничего не найдено. Измените запрос или фильтр.").assertIsDisplayed()
        compose.onNodeWithTag("search").performTextReplacement("")
        waitForTag("pokemon-1")
        compose.onNodeWithTag("pokemon-1").assertIsDisplayed()
    }
}
