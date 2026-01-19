/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("UnusedVariable", "unused")

package us.blindmint.codex.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.database.CursorWindow
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.immutableListOf
import us.blindmint.codex.R
import us.blindmint.codex.domain.file.CachedFileCompat
import us.blindmint.codex.domain.library.book.NullableBook
import us.blindmint.codex.domain.navigator.NavigatorItem
import us.blindmint.codex.domain.navigator.StackEvent
import us.blindmint.codex.domain.ui.isDark
import us.blindmint.codex.domain.ui.isPureDark
import us.blindmint.codex.domain.use_case.book.GetBookByFilePath
import us.blindmint.codex.domain.use_case.book.InsertBook
import us.blindmint.codex.domain.use_case.file_system.GetBookFromFile
import us.blindmint.codex.ui.reader.ReaderScreen
import us.blindmint.codex.presentation.core.components.navigation_bar.NavigationBar
import us.blindmint.codex.presentation.core.components.navigation_rail.NavigationRail
import us.blindmint.codex.presentation.main.MainActivityKeyboardManager
import us.blindmint.codex.presentation.navigator.LocalNavigator
import us.blindmint.codex.presentation.navigator.Navigator
import us.blindmint.codex.presentation.navigator.NavigatorTabs
import us.blindmint.codex.ui.browse.BrowseModel
import us.blindmint.codex.ui.browse.BrowseScreen
import us.blindmint.codex.ui.history.HistoryModel
import us.blindmint.codex.ui.history.HistoryScreen
import us.blindmint.codex.ui.library.LibraryModel
import us.blindmint.codex.ui.library.LibraryScreen
import us.blindmint.codex.ui.settings.SettingsModel
import us.blindmint.codex.ui.settings.SettingsScreen
import us.blindmint.codex.ui.start.StartScreen
import us.blindmint.codex.ui.theme.CodexTheme
import us.blindmint.codex.ui.theme.Transitions
import java.lang.reflect.Field
import javax.inject.Inject


@SuppressLint("DiscouragedPrivateApi")
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    // Creating an instance of Models
    private val mainModel: MainModel by viewModels()
    private val settingsModel: SettingsModel by viewModels()

    // Injected use cases for handling file intents
    @Inject lateinit var getBookFromFile: GetBookFromFile
    @Inject lateinit var insertBook: InsertBook
    @Inject lateinit var getBookByFilePath: GetBookByFilePath

    // Pending file URI from external intent
    private var pendingFileUri: Uri? = null
    private var pendingFileUriCounter = 0

    // State for file import results
    private val fileImportState = MutableStateFlow<FileImportState?>(null)

    data class FileImportState(val bookId: Int, val message: String?)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                !mainModel.isReady.value
            }
        }

        // Default super
        super.onCreate(savedInstanceState)

        // Handle file open intent from external file managers
        handleIncomingIntent(intent)

        // Bigger Cursor size for Room
        try {
            val field: Field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
            field.isAccessible = true
            field.set(null, 100 * 1024 * 1024)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initializing the MainModel
        mainModel.init(settingsModel.isReady)

        // Edge to edge
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // Initializing Screen Models
            val libraryModel = hiltViewModel<LibraryModel>()
            val historyModel = hiltViewModel<HistoryModel>()
            val browseModel = hiltViewModel<BrowseModel>()

            val state = mainModel.state.collectAsStateWithLifecycle()
            val isLoaded = mainModel.isReady.collectAsStateWithLifecycle()

            val tabs = immutableListOf(
                NavigatorItem(
                    screen = LibraryScreen,
                    title = R.string.library_screen,
                    tooltip = R.string.library_content_desc,
                    selectedIcon = R.drawable.library_filled,
                    unselectedIcon = R.drawable.library_outlined
                ),
                NavigatorItem(
                    screen = HistoryScreen,
                    title = R.string.history_screen,
                    tooltip = R.string.history_content_desc,
                    selectedIcon = R.drawable.history_filled,
                    unselectedIcon = R.drawable.history_outlined
                ),
                NavigatorItem(
                    screen = BrowseScreen,
                    title = R.string.browse_screen,
                    tooltip = R.string.browse_content_desc,
                    selectedIcon = R.drawable.discover_filled,
                    unselectedIcon = R.drawable.discover_outlined
                ),
                NavigatorItem(
                    screen = SettingsScreen,
                    title = R.string.settings_screen,
                    tooltip = R.string.settings_screen,
                    selectedIcon = R.drawable.settings_filled,
                    unselectedIcon = R.drawable.settings_outlined
                )
            )

            MainActivityKeyboardManager()

            if (isLoaded.value) {
                // Perform initial color preset selection based on system theme
                val isDarkTheme = isSystemInDarkTheme()
                LaunchedEffect(Unit) {
                    settingsModel.performInitialColorPresetSelection(isDarkTheme)
                }



                // Handle file import state changes
                val currentFileImportState by fileImportState.collectAsStateWithLifecycle()
                LaunchedEffect(currentFileImportState) {
                    currentFileImportState?.let { state ->
                        fileImportState.value = null // Consume the state

                        state.message?.let {
                            Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                        }

                        // Navigate to the reader
                        HistoryScreen.insertHistoryChannel.trySend(state.bookId)
                        navigator.push(ReaderScreen(state.bookId))
                    }
                }

                            if (existingBook != null) {
                                // Book already exists - open it directly
                                fileImportBookId = existingBook.id
                                fileImportMessage = getString(R.string.opening_existing_book, existingBook.title)
                            } else {
                                // Book doesn't exist - import it
                                val result = withContext(Dispatchers.IO) {
                                    getBookFromFile.execute(cachedFile)
                                }
                                 when (result) {
                                     is NullableBook.NotNull -> {
                                         val bookTitle = result.bookWithCover!!.book.title
                                        // Update the filePath for content URIs
                                        val storagePath = uriString ?: absolutePath
                                        val bookWithCorrectPath = result.bookWithCover!!.copy(
                                            book = result.bookWithCover.book.copy(filePath = storagePath)
                                        )
                                        withContext(Dispatchers.IO) {
                                            insertBook.execute(bookWithCorrectPath)
                                        }
                                        // Get the newly inserted book to get its ID
                                        val newBook = withContext(Dispatchers.IO) {
                                            getBookByFilePath.execute(storagePath)
                                        }
                                        if (newBook != null) {
                                            fileImportBookId = newBook.id
                                            fileImportMessage = getString(R.string.book_added, bookTitle)
                                            // Trigger library refresh
                                            libraryModel.onEvent(
                                                us.blindmint.codex.ui.library.LibraryEvent.OnRefreshList(
                                                    loading = false,
                                                    hideSearch = true
                                                )
                                            )
                                        }
                                    }
                                    is NullableBook.Null -> {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                result.message?.asString(this@MainActivity)
                                                    ?: getString(R.string.error_something_went_wrong),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.error_something_went_wrong),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

                CodexTheme(
                    theme = state.value.theme,
                    isDark = state.value.darkTheme.isDark(),
                    isPureDark = state.value.pureDark.isPureDark(this),
                    themeContrast = state.value.themeContrast
                ) {
                    Navigator(
                        initialScreen = if (state.value.showStartScreen) StartScreen
                        else LibraryScreen,
                        transitionSpec = { lastEvent ->
                            when (lastEvent) {
                                StackEvent.Default -> {
                                    Transitions.SlidingTransitionIn
                                        .togetherWith(Transitions.SlidingTransitionOut)
                                }

                                StackEvent.Pop -> {
                                    Transitions.BackSlidingTransitionIn
                                        .togetherWith(Transitions.BackSlidingTransitionOut)
                                }
                            }
                        },
                        contentKey = {
                            when (it) {
                                LibraryScreen, HistoryScreen, BrowseScreen, SettingsScreen -> "tabs"
                                else -> it
                            }
                        },
                        backHandlerEnabled = { it != StartScreen }
                    ) { screen ->
                        // Handle file import navigation
                        val navigator = LocalNavigator.current
                        LaunchedEffect(fileImportBookId) {
                            val bookId = fileImportBookId ?: return@LaunchedEffect
                            fileImportBookId = null

                            fileImportMessage?.let {
                                Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                                fileImportMessage = null
                            }

                            // Navigate to the reader with the imported/existing book
                            HistoryScreen.insertHistoryChannel.trySend(bookId)
                            navigator.push(ReaderScreen(bookId))
                        }

                        when (screen) {
                            LibraryScreen, HistoryScreen, BrowseScreen, SettingsScreen -> {
                                NavigatorTabs(
                                    currentTab = screen,
                                    transitionSpec = {
                                        Transitions.FadeTransitionIn
                                            .togetherWith(Transitions.FadeTransitionOut)
                                    },
                                    navigationBar = { NavigationBar(tabs = tabs) },
                                    navigationRail = { NavigationRail(tabs = tabs) }
                                ) { tab ->
                                    tab.Content()
                                }
                            }

                            else -> {
                                android.util.Log.d("NAV_DEBUG", "Rendering screen: ${screen::class.simpleName}")
                                screen.Content()
                            }
                        }
                    }
                }
            }
        }
                            }

                            else -> {
                                android.util.Log.d("NAV_DEBUG", "Rendering screen: ${screen::class.simpleName}")
                                screen.Content()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                pendingFileUri = uri
                pendingFileUriCounter++

                // Launch file import in viewModelScope
                viewModelScope.launch {
                    processFileImport(uri)
                }
            }
        }
    }

    private suspend fun processFileImport(uri: Uri) {
        try {
            val cachedFile = CachedFileCompat.fromUri(this@MainActivity, uri)
            val absolutePath = cachedFile.path
            val uriString = if (uri.scheme == "content") uri.toString() else null

            // Check if book already exists in library
            val existingBook = withContext(Dispatchers.IO) {
                // Try to find by URI string first (for content URIs), then by absolute path
                uriString?.let { getBookByFilePath.execute(it) } ?: getBookByFilePath.execute(absolutePath)
            }

            if (existingBook != null) {
                // Book already exists - open it directly
                fileImportState.value = FileImportState(
                    bookId = existingBook.id,
                    message = getString(R.string.opening_existing_book, existingBook.title)
                )
            } else {
                // Import the book
                val result = withContext(Dispatchers.IO) {
                    getBookFromFile.execute(cachedFile)
                }
                when (result) {
                    is NullableBook.NotNull -> {
                        val bookTitle = result.bookWithCover!!.book.title
                        // Update the filePath for content URIs
                        val storagePath = uriString ?: absolutePath
                        val bookWithCorrectPath = result.bookWithCover!!.copy(
                            book = result.bookWithCover.book.copy(filePath = storagePath)
                        )
                        withContext(Dispatchers.IO) {
                            insertBook.execute(bookWithCorrectPath)
                        }
                        // Get the newly inserted book to get its ID
                        val newBook = withContext(Dispatchers.IO) {
                            getBookByFilePath.execute(storagePath)
                        }
                        if (newBook != null) {
                            fileImportState.value = FileImportState(
                                bookId = newBook.id,
                                message = getString(R.string.book_added, bookTitle)
                            )
                            // Trigger library refresh
                            withContext(Dispatchers.Main) {
                                libraryModel.onEvent(
                                    us.blindmint.codex.ui.library.LibraryEvent.OnRefreshList(
                                        loading = false,
                                        hideSearch = true
                                    )
                                )
                            }
                        }
                    }
                    is NullableBook.Null -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                result.message?.asString(this@MainActivity)
                                    ?: getString(R.string.error_something_went_wrong),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.error_something_went_wrong),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        cacheDir.deleteRecursively()
        super.onDestroy()
    }
}