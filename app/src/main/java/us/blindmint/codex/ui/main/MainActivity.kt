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
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
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

    // Mutex to serialize file import operations and prevent race conditions
    private val fileImportMutex = Mutex()

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
                            val targetScreen = this.targetState
                            val isReaderScreen = targetScreen is us.blindmint.codex.ui.reader.ReaderScreen ||
                                                 targetScreen is us.blindmint.codex.ui.reader.SpeedReadingScreen

                            when (lastEvent) {
                                StackEvent.Default -> {
                                    if (isReaderScreen) {
                                        // Use fade transition for reader screens to avoid sliding animation during loading
                                        Transitions.FadeTransitionIn
                                            .togetherWith(Transitions.FadeTransitionOut)
                                    } else {
                                        Transitions.SlidingTransitionIn
                                            .togetherWith(Transitions.SlidingTransitionOut)
                                    }
                                }

                                StackEvent.Pop -> {
                                    if (isReaderScreen) {
                                        // Use fade transition for reader screens to avoid sliding animation during loading
                                        Transitions.FadeTransitionIn
                                            .togetherWith(Transitions.FadeTransitionOut)
                                    } else {
                                        Transitions.BackSlidingTransitionIn
                                            .togetherWith(Transitions.BackSlidingTransitionOut)
                                    }
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
                        // Handle file import state changes
                        val navigator = LocalNavigator.current
                        val currentFileImportState by fileImportState.collectAsStateWithLifecycle()
                        val libraryModel = hiltViewModel<LibraryModel>()

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                pendingFileUri = uri
                pendingFileUriCounter++

                // Launch file import in lifecycleScope
                lifecycleScope.launch {
                    processFileImport(uri)
                }
            }
        }
    }

    /**
     * Resolves the display name for a content URI by querying the content resolver.
     * Falls back to the URI's last path segment or a generated name.
     */
    private fun resolveFileName(uri: Uri): String {
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        } catch (_: Exception) { }
        // Fall back to URI last path segment
        return uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: "import_${System.currentTimeMillis()}"
    }

    /**
     * Copies a content:// URI to permanent internal storage so the file remains
     * accessible after the temporary URI permission expires.
     * Returns the local File, or null if the copy fails.
     */
    private fun copyToInternalStorage(uri: Uri, fileName: String): File? {
        val importsDir = File(filesDir, "imports")
        if (!importsDir.exists()) importsDir.mkdirs()

        // Use a unique prefix to avoid collisions for files with the same name
        val targetFile = File(importsDir, "${System.currentTimeMillis()}_$fileName")
        val tempFile = File(importsDir, "${targetFile.name}.tmp")

        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                BufferedOutputStream(FileOutputStream(tempFile)).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            if (!tempFile.renameTo(targetFile)) {
                tempFile.delete()
                return null
            }
            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            null
        }
    }

    private suspend fun processFileImport(uri: Uri) {
        // Use mutex to serialize imports and prevent race conditions
        fileImportMutex.withLock {
            try {
                val cachedFile = CachedFileCompat.fromUri(this@MainActivity, uri)
                // Get absolute path; for content:// URIs that don't resolve to a path, fall back to URI string
                val absolutePath = if (cachedFile.path.isNotBlank()) {
                    cachedFile.path
                } else {
                    uri.toString()
                }

                // Check if book already exists in library by absolute path
                // This is URI-scheme-agnostic and works for both file:// and content:// URIs
                val existingBook = withContext(Dispatchers.IO) {
                    getBookByFilePath.execute(absolutePath)
                }

                if (existingBook != null) {
                    // Book already exists - open it directly
                    fileImportState.value = FileImportState(
                        bookId = existingBook.id,
                        message = getString(R.string.opening_existing_book, existingBook.title)
                    )
                } else {
                    // For content:// URIs without a resolvable path, copy the file to internal
                    // storage while the temporary URI permission is still valid
                    val (importCachedFile, importPath) = if (cachedFile.path.isBlank()) {
                        withContext(Dispatchers.IO) {
                            val fileName = resolveFileName(uri)
                            val localFile = copyToInternalStorage(uri, fileName)
                            if (localFile != null) {
                                val localUri = Uri.fromFile(localFile)
                                val localCachedFile = CachedFileCompat.fromUri(this@MainActivity, localUri)
                                Pair(localCachedFile, localFile.absolutePath)
                            } else {
                                // Copy failed — fall back to original URI (may still work
                                // if the provider grants long-lived permissions)
                                Pair(cachedFile, absolutePath)
                            }
                        }
                    } else {
                        Pair(cachedFile, absolutePath)
                    }

                    // Import the book
                    val result = withContext(Dispatchers.IO) {
                        getBookFromFile.execute(importCachedFile)
                    }
                    when (result) {
                        is NullableBook.NotNull -> {
                            val bookTitle = result.bookWithCover!!.book.title
                            // Always store the resolved path for consistent duplicate detection
                            val bookWithCorrectPath = result.bookWithCover!!.copy(
                                book = result.bookWithCover.book.copy(filePath = importPath)
                            )
                            withContext(Dispatchers.IO) {
                                insertBook.execute(bookWithCorrectPath)
                            }
                            // Get the newly inserted book to get its ID
                            val newBook = withContext(Dispatchers.IO) {
                                getBookByFilePath.execute(importPath)
                            }
                            if (newBook != null) {
                                fileImportState.value = FileImportState(
                                    bookId = newBook.id,
                                    message = getString(R.string.book_added, bookTitle)
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

    override fun onDestroy() {
        cacheDir.deleteRecursively()
        super.onDestroy()
    }
}