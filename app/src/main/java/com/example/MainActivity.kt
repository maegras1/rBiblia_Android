package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.local.DatabaseHelper
import com.example.data.local.PreferencesManager
import com.example.data.local.room.BibleDatabase
import com.example.data.remote.RBibliaApiService
import com.example.data.repository.BibleRepository
import com.example.service.GitHubUpdateService
import com.example.service.UpdateStatus
import com.example.ui.components.AboutDialog
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.BibleTopAppBar
import com.example.ui.components.BookSelectorDialog
import com.example.ui.components.ChapterComparisonDialog
import com.example.ui.components.ChapterSelectorDialog
import com.example.ui.components.ErrorReportDialog
import com.example.ui.components.NoteEditorDialog
import com.example.ui.components.NotesListDialog
import com.example.ui.components.SearchDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SideMenuDrawer
import com.example.ui.components.TranslationSelectorDialog
import com.example.ui.components.UpdateAvailableDialog
import com.example.ui.components.VerseActionsBottomSheet
import com.example.ui.components.VerseComparisonDialog
import com.example.ui.screens.ReaderScreen
import com.example.ui.util.DiffMode
import com.example.ui.theme.RBibliaTheme
import com.example.ui.viewmodel.BibleViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: BibleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dbHelper = DatabaseHelper(applicationContext)
        val prefs = PreferencesManager(applicationContext)
        val apiService = RBibliaApiService()
        val roomDb = BibleDatabase.getInstance(applicationContext)
        val repository = BibleRepository(dbHelper, prefs, apiService, roomDb)
        val updateService = GitHubUpdateService(applicationContext)

        viewModel = BibleViewModel(repository, updateService)

        // Check for updates in background on launch
        updateService.checkForUpdatesAsync(notifySystemNotification = true)

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            RBibliaTheme(
                themeMode = uiState.themeMode,
                darkVariant = uiState.darkVariant
            ) {
                BibleApp(viewModel = viewModel, updateService = updateService)
            }
        }
    }
}

@Composable
fun BibleApp(viewModel: BibleViewModel, updateService: GitHubUpdateService) {
    val uiState by viewModel.uiState.collectAsState()
    val updateStatus by updateService.status.collectAsState()
    var showUpdatePromptDialog by remember { mutableStateOf(true) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog & Sheet States
    var showBookSelector by remember { mutableStateOf(false) }
    var showChapterSelector by remember { mutableStateOf(false) }
    var showTranslationSelector by remember { mutableStateOf(false) }
    var showParallelTranslationSelector by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showChapterComparison by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showNotesListDialog by remember { mutableStateOf(false) }

    // Toast message handler
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // BackHandler for open dialogs / sheets / drawer
    val anyOverlayOpen = drawerState.isOpen ||
        showBookSelector ||
        showChapterSelector ||
        showTranslationSelector ||
        showParallelTranslationSelector ||
        showSearchDialog ||
        showChapterComparison ||
        showSettingsDialog ||
        showAboutDialog ||
        showNotesListDialog ||
        (updateStatus is UpdateStatus.UpdateAvailable && showUpdatePromptDialog) ||
        uiState.activeVerseForActions != null ||
        uiState.compareVerseNumber != null ||
        uiState.editingNote != null ||
        uiState.reportingVerse != null ||
        uiState.updateCheckResult != null

    BackHandler(enabled = anyOverlayOpen) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (showBookSelector) {
            showBookSelector = false
        } else if (showChapterSelector) {
            showChapterSelector = false
        } else if (showTranslationSelector) {
            showTranslationSelector = false
        } else if (showParallelTranslationSelector) {
            showParallelTranslationSelector = false
        } else if (showSearchDialog) {
            showSearchDialog = false
        } else if (showChapterComparison) {
            showChapterComparison = false
        } else if (showSettingsDialog) {
            showSettingsDialog = false
        } else if (showAboutDialog) {
            showAboutDialog = false
        } else if (showNotesListDialog) {
            showNotesListDialog = false
        } else if (updateStatus is UpdateStatus.UpdateAvailable && showUpdatePromptDialog) {
            showUpdatePromptDialog = false
        } else if (uiState.updateCheckResult != null) {
            viewModel.dismissUpdateDialog()
        } else if (uiState.activeVerseForActions != null) {
            viewModel.closeVerseActions()
        } else if (uiState.compareVerseNumber != null) {
            viewModel.closeVerseComparison()
        } else if (uiState.editingNote != null) {
            viewModel.closeNoteEditor()
        } else if (uiState.reportingVerse != null) {
            viewModel.closeErrorReport()
        }
    }

    // Hamburger menu opens on the RIGHT side (same side where the button is located)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    SideMenuDrawer(
                        currentLanguage = uiState.appLanguage,
                        isParallelReading = uiState.isParallelReading,
                        onToggleParallelReading = { viewModel.toggleParallelReading() },
                        onSelectTranslations = { showTranslationSelector = true },
                        onSelectNotes = { showNotesListDialog = true },
                        onSelectSearch = { showSearchDialog = true },
                        onSelectChapterComparison = { showChapterComparison = true },
                        onSelectSettings = { showSettingsDialog = true },
                        onSelectAbout = { showAboutDialog = true },
                        onCloseDrawer = { scope.launch { drawerState.close() } }
                    )
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(
                    topBar = {
                        BibleTopAppBar(
                            selectedBook = uiState.selectedBook,
                            selectedChapter = uiState.selectedChapter,
                            selectedTranslation = uiState.selectedTranslation,
                            isParallelReading = uiState.isParallelReading,
                            parallelTranslation = uiState.parallelTranslation,
                            onOpenBookSelector = { showBookSelector = true },
                            onOpenChapterSelector = { showChapterSelector = true },
                            onOpenTranslationSelector = { showTranslationSelector = true },
                            onOpenParallelTranslationSelector = { showParallelTranslationSelector = true },
                            onToggleParallelReading = { viewModel.toggleParallelReading() },
                            onOpenMenu = { scope.launch { drawerState.open() } }
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { paddingValues ->
            ReaderScreen(
                currentLanguage = uiState.appLanguage,
                book = uiState.selectedBook,
                chapter = uiState.selectedChapter,
                translation = uiState.selectedTranslation,
                verses = uiState.verses,
                isLoading = uiState.isLoading,
                isParallelReading = uiState.isParallelReading,
                parallelTranslation = uiState.parallelTranslation,
                parallelVerses = uiState.parallelVerses,
                isParallelLoading = uiState.isParallelLoading,
                parallelLayoutColumns = uiState.parallelLayoutColumns,
                parallelShowDifferences = uiState.parallelShowDifferences,
                onSelectPrimaryTranslation = { showTranslationSelector = true },
                onSelectParallelTranslation = { showParallelTranslationSelector = true },
                onSwapParallelTranslations = { viewModel.swapParallelTranslations() },
                onToggleParallelLayout = { viewModel.toggleParallelLayout() },
                onToggleParallelDifferences = { viewModel.toggleParallelDifferences() },
                onCloseParallelReading = { viewModel.toggleParallelReading(false) },
                textSize = uiState.textSize,
                fontFamily = uiState.fontFamily,
                continuousText = uiState.continuousText,
                hideVerseNumbers = uiState.hideVerseNumbers,
                zenMode = uiState.zenMode,
                highlightedVerse = uiState.highlightedVerse,
                onVerseClick = { verse -> viewModel.openVerseActions(verse) },
                onNextChapter = { viewModel.nextChapter() },
                onPrevChapter = { viewModel.prevChapter() },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
}

    // --- Dialogs & Sheets ---

    if (showBookSelector) {
        BookSelectorDialog(
            currentLanguage = uiState.appLanguage,
            currentBook = uiState.selectedBook,
            onBookSelected = { book ->
                showBookSelector = false
                viewModel.selectBook(book)
                showChapterSelector = true
            },
            onDismiss = { showBookSelector = false }
        )
    }

    if (showChapterSelector) {
        ChapterSelectorDialog(
            currentLanguage = uiState.appLanguage,
            book = uiState.selectedBook,
            availableChapters = uiState.availableChapters,
            currentChapter = uiState.selectedChapter,
            onChapterSelected = { chapter ->
                showChapterSelector = false
                viewModel.selectChapter(chapter)
            },
            onDismiss = { showChapterSelector = false }
        )
    }

    if (showTranslationSelector) {
        TranslationSelectorDialog(
            currentLanguage = uiState.appLanguage,
            translations = uiState.translations,
            selectedTranslation = uiState.selectedTranslation,
            onSelectTranslation = { translation ->
                showTranslationSelector = false
                viewModel.selectTranslation(translation)
            },
            onToggleFavorite = { transId ->
                viewModel.toggleFavorite(transId)
            },
            onDismiss = { showTranslationSelector = false }
        )
    }

    if (showParallelTranslationSelector) {
        TranslationSelectorDialog(
            currentLanguage = uiState.appLanguage,
            translations = uiState.translations,
            selectedTranslation = uiState.parallelTranslation,
            onSelectTranslation = { translation ->
                showParallelTranslationSelector = false
                viewModel.selectParallelTranslation(translation)
            },
            onToggleFavorite = { transId ->
                viewModel.toggleFavorite(transId)
            },
            onDismiss = { showParallelTranslationSelector = false }
        )
    }

    if (showSearchDialog) {
        SearchDialog(
            currentLanguage = uiState.appLanguage,
            searchQuery = uiState.searchQuery,
            searchScope = uiState.searchScope,
            searchResults = uiState.searchResults,
            recentSearches = uiState.recentSearches,
            isSearching = uiState.isSearching,
            onQueryChanged = { viewModel.onSearchQueryChanged(it) },
            onScopeChanged = { viewModel.onSearchScopeChanged(it) },
            onExecuteSearch = { query -> viewModel.executeSearch(query) },
            onClearHistory = { viewModel.clearSearchHistory() },
            onDeleteSearchItem = { viewModel.deleteSearchItem(it) },
            onResultSelected = { result ->
                showSearchDialog = false
                viewModel.navigateTo(result.book, result.chapter, result.verse)
            },
            onDismiss = { showSearchDialog = false }
        )
    }

    if (showChapterComparison) {
        ChapterComparisonDialog(
            currentLanguage = uiState.appLanguage,
            book = uiState.selectedBook,
            chapter = uiState.selectedChapter,
            allTranslations = uiState.translations,
            selectedTranslations = uiState.chapterCompTranslations,
            chapterVerses = uiState.chapterCompVerses,
            isLoading = uiState.isChapterCompLoading,
            onLoadComparison = { ids -> viewModel.loadChapterComparison(ids) },
            onDismiss = { showChapterComparison = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentLanguage = uiState.appLanguage,
            textSize = uiState.textSize,
            fontFamily = uiState.fontFamily,
            themeMode = uiState.themeMode,
            darkVariant = uiState.darkVariant,
            zenMode = uiState.zenMode,
            continuousText = uiState.continuousText,
            hideVerseNumbers = uiState.hideVerseNumbers,
            diffMode = uiState.diffMode,
            comparisonLimit = uiState.comparisonLimit,
            cachedVersesCount = uiState.cachedVersesCount,
            isDownloadingOffline = uiState.isDownloadingBookOffline,
            offlineDownloadProgress = uiState.offlineDownloadProgress,
            onTextSizeChanged = { viewModel.setTextSize(it) },
            onFontFamilyChanged = { viewModel.setFontFamily(it) },
            onThemeModeChanged = { viewModel.setThemeMode(it) },
            onDarkVariantChanged = { viewModel.setDarkVariant(it) },
            onZenModeChanged = { viewModel.setZenMode(it) },
            onContinuousTextChanged = { viewModel.setContinuousText(it) },
            onHideVerseNumbersChanged = { viewModel.setHideVerseNumbers(it) },
            onLanguageChanged = { viewModel.setAppLanguage(it) },
            onDiffModeChanged = { viewModel.setDiffMode(it) },
            onComparisonLimitChanged = { viewModel.setComparisonLimit(it) },
            onClearCache = { viewModel.clearOfflineCache() },
            onDownloadBookOffline = { viewModel.downloadCurrentBookOffline() },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            currentLanguage = uiState.appLanguage,
            onCheckForUpdates = {
                showAboutDialog = false
                viewModel.checkForAppUpdates()
            },
            onDismiss = { showAboutDialog = false }
        )
    }

    // App Update Dialog
    uiState.updateCheckResult?.let { updateResult ->
        AppUpdateDialog(
            currentLanguage = uiState.appLanguage,
            result = updateResult,
            onDismiss = { viewModel.dismissUpdateDialog() }
        )
    }

    // Automatic Update Prompt Dialog when GitHubUpdateService detects an update
    if (updateStatus is UpdateStatus.UpdateAvailable && showUpdatePromptDialog) {
        val release = (updateStatus as UpdateStatus.UpdateAvailable).release
        UpdateAvailableDialog(
            release = release,
            onDismiss = { showUpdatePromptDialog = false }
        )
    }

    if (showNotesListDialog) {
        NotesListDialog(
            currentLanguage = uiState.appLanguage,
            notes = uiState.allNotes,
            onNoteClick = { note ->
                showNotesListDialog = false
                viewModel.navigateTo(note.bookId, note.chapterId, note.verseId)
            },
            onDeleteNote = { id -> viewModel.deleteNote(id) },
            onExportXml = { viewModel.exportNotesXml() },
            onImportXml = { xml -> viewModel.importNotesXml(xml) },
            onDismiss = { showNotesListDialog = false }
        )
    }

    // Verse Actions Sheet
    uiState.activeVerseForActions?.let { verse ->
        VerseActionsBottomSheet(
            currentLanguage = uiState.appLanguage,
            book = uiState.selectedBook,
            chapter = uiState.selectedChapter,
            verse = verse,
            onAddNote = { viewModel.startEditingNote(verse) },
            onCompare = {
                viewModel.closeVerseActions()
                viewModel.openVerseComparison(verse)
            },
            onReportError = { viewModel.startErrorReport(verse) },
            onDismiss = { viewModel.closeVerseActions() }
        )
    }

    // Verse Comparison Dialog
    if (uiState.compareVerseNumber != null) {
        VerseComparisonDialog(
            currentLanguage = uiState.appLanguage,
            book = uiState.selectedBook,
            chapter = uiState.selectedChapter,
            verseNumber = uiState.compareVerseNumber!!,
            baseTranslation = uiState.selectedTranslation,
            comparedVerses = uiState.comparedVerses,
            allTranslations = uiState.translations,
            targetTranslations = uiState.comparisonTargetTranslations,
            showDifferences = uiState.showDifferences,
            diffMode = uiState.diffMode,
            isLoading = uiState.isComparingVerse,
            onToggleDifferences = { viewModel.toggleShowDifferences() },
            onToggleDiffMode = {
                viewModel.setDiffMode(if (uiState.diffMode == DiffMode.LOOSE) DiffMode.STRICT else DiffMode.LOOSE)
            },
            onPrevVerse = { viewModel.comparePreviousVerse() },
            onNextVerse = { viewModel.compareNextVerse() },
            onAddTranslation = { tid -> viewModel.addComparisonTranslation(tid) },
            onRemoveTranslation = { tid -> viewModel.removeComparisonTranslation(tid) },
            onSelectTranslation = { trans ->
                viewModel.selectTranslation(trans)
                viewModel.closeVerseComparison()
            },
            onDismiss = { viewModel.closeVerseComparison() }
        )
    }

    // Note Editor Dialog
    uiState.editingNote?.let { note ->
        NoteEditorDialog(
            currentLanguage = uiState.appLanguage,
            note = note,
            onSave = { content, isGlobal -> viewModel.saveNote(content, isGlobal) },
            onDelete = { id -> viewModel.deleteNote(id) },
            onDismiss = { viewModel.closeNoteEditor() }
        )
    }

    // Error Report Dialog
    uiState.reportingVerse?.let { verse ->
        ErrorReportDialog(
            currentLanguage = uiState.appLanguage,
            book = uiState.selectedBook,
            chapter = uiState.selectedChapter,
            verse = verse,
            translation = uiState.selectedTranslation,
            isSubmitting = uiState.isReportSubmitting,
            onSubmit = { name, email, notes, content, errorType ->
                viewModel.submitErrorReport(name, email, notes, content, errorType)
            },
            onDismiss = { viewModel.closeErrorReport() }
        )
    }
}
