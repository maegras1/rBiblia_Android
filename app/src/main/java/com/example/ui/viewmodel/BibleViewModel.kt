package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BookCatalog
import com.example.data.model.BookInfo
import com.example.data.model.DarkVariant
import com.example.data.model.ErrorReport
import com.example.data.model.SearchResult
import com.example.BuildConfig
import com.example.data.model.SearchScope
import com.example.data.model.TextFontFamily
import com.example.data.model.TextSize
import com.example.data.model.ThemeMode
import com.example.data.model.Translation
import com.example.data.model.Verse
import com.example.data.model.VerseNote
import com.example.data.repository.BibleRepository
import com.example.service.GitHubUpdateService
import com.example.service.UpdateStatus
import com.example.ui.util.DiffMode
import com.example.ui.util.Strings
import com.example.util.AppUpdateManager
import com.example.util.UpdateCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BibleUiState(
    val appLanguage: String = "pl",
    val selectedBook: BookInfo = BookCatalog.getBook("gen", "pl"),
    val selectedChapter: Int = 1,
    val availableChapters: List<Int> = (1..50).toList(),
    val selectedTranslation: Translation? = null,
    val translations: List<Translation> = emptyList(),
    val verses: List<Verse> = emptyList(),
    val isLoading: Boolean = false,
    val isParallelReading: Boolean = false,
    val parallelTranslation: Translation? = null,
    val parallelVerses: List<Verse> = emptyList(),
    val isParallelLoading: Boolean = false,
    val parallelLayoutColumns: Boolean = true,
    val parallelShowDifferences: Boolean = false,
    val textSize: TextSize = TextSize.MEDIUM,
    val fontFamily: TextFontFamily = TextFontFamily.SERIF,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val darkVariant: DarkVariant = DarkVariant.GOLD,
    val zenMode: Boolean = false,
    val continuousText: Boolean = false,
    val hideVerseNumbers: Boolean = false,
    val highlightedVerse: Int? = null,
    val activeVerseForActions: Verse? = null,
    val compareVerseNumber: Int? = null,
    val comparedVerses: Map<String, String> = emptyMap(),
    val comparisonTargetTranslations: List<String> = emptyList(),
    val showDifferences: Boolean = true,
    val diffMode: DiffMode = DiffMode.LOOSE,
    val comparisonLimit: Int = 4,
    val isComparingVerse: Boolean = false,
    val chapterCompTranslations: List<String> = emptyList(),
    val chapterCompVerses: Map<String, List<Verse>> = emptyMap(),
    val isChapterCompLoading: Boolean = false,
    val editingNote: VerseNote? = null,
    val allNotes: List<VerseNote> = emptyList(),
    val reportingVerse: Verse? = null,
    val isReportSubmitting: Boolean = false,
    val searchQuery: String = "",
    val searchScope: SearchScope = SearchScope.ALL,
    val searchResults: List<SearchResult> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val toastMessage: String? = null,
    val cachedVersesCount: Int = 0,
    val isDownloadingBookOffline: Boolean = false,
    val offlineDownloadProgress: Pair<Int, Int>? = null,
    val isCheckingUpdate: Boolean = false,
    val updateCheckResult: UpdateCheckResult? = null
)

class BibleViewModel(
    private val repository: BibleRepository,
    private val updateService: GitHubUpdateService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BibleUiState(
            appLanguage = repository.preferences.appLanguage,
            textSize = repository.preferences.textSize,
            fontFamily = repository.preferences.fontFamily,
            themeMode = repository.preferences.themeMode,
            darkVariant = repository.preferences.darkVariant,
            zenMode = repository.preferences.zenMode,
            continuousText = repository.preferences.continuousText,
            hideVerseNumbers = repository.preferences.hideVerseNumbers,
            selectedChapter = repository.preferences.selectedChapter,
            isParallelReading = repository.preferences.isParallelReading,
            parallelLayoutColumns = repository.preferences.parallelLayoutColumns,
            parallelShowDifferences = repository.preferences.parallelShowDifferences,
            diffMode = try { DiffMode.valueOf(repository.preferences.diffMode) } catch (e: Exception) { DiffMode.LOOSE },
            comparisonLimit = repository.preferences.comparisonLimit
        )
    )
    val uiState: StateFlow<BibleUiState> = _uiState.asStateFlow()

    init {
        val initialLang = repository.preferences.appLanguage
        val bookId = repository.preferences.selectedBook
        val initialBook = repository.getBook(bookId, initialLang)
        val initialDiffMode = try { DiffMode.valueOf(repository.preferences.diffMode) } catch (e: Exception) { DiffMode.LOOSE }
        val initialCompLimit = repository.preferences.comparisonLimit
        _uiState.update {
            it.copy(
                selectedBook = initialBook,
                availableChapters = (1..initialBook.chapterCount).toList(),
                isParallelReading = repository.preferences.isParallelReading,
                parallelLayoutColumns = repository.preferences.parallelLayoutColumns,
                parallelShowDifferences = repository.preferences.parallelShowDifferences,
                diffMode = initialDiffMode,
                comparisonLimit = initialCompLimit
            )
        }

        // Collect Room Database cache statistics
        viewModelScope.launch {
            repository.getCachedVersesCountFlow().collect { count ->
                _uiState.update { it.copy(cachedVersesCount = count) }
            }
        }

        loadInitialData()
    }

    private suspend fun resolveBookForTranslation(
        translationId: String,
        preferredBookId: String = "",
        forceDefault: Boolean = false
    ): Pair<String, Int> {
        val lang = _uiState.value.appLanguage
        val structure = repository.getTranslationStructure(lang, translationId)
        val otBookIds = BookCatalog.getOldTestamentBookIds()
        val ntBookIds = BookCatalog.getNewTestamentBookIds()

        // Helper to determine default book when not forcing a specific valid book
        fun getDefaultBook(): Pair<String, Int> {
            if (structure.isEmpty()) {
                val isNtOnly = translationId.contains("nt", ignoreCase = true)
                return if (isNtOnly) Pair("mat", 1) else Pair("gen", 1)
            }

            val hasOldTestament = structure.keys.any { it in otBookIds }
            val hasNewTestament = structure.keys.any { it in ntBookIds }

            return when {
                // Rule 1: Genesis (gen) 1 if translation contains Old Testament (from beginning of Bible)
                hasOldTestament && structure.containsKey("gen") -> {
                    val ch = structure["gen"]?.firstOrNull() ?: 1
                    Pair("gen", ch)
                }
                hasOldTestament -> {
                    val firstOt = structure.keys.firstOrNull { it in otBookIds } ?: "gen"
                    val ch = structure[firstOt]?.firstOrNull() ?: 1
                    Pair(firstOt, ch)
                }
                // Rule 2: If translation does NOT contain Old Testament -> beginning of New Testament (Matthew 1)
                hasNewTestament && structure.containsKey("mat") -> {
                    val ch = structure["mat"]?.firstOrNull() ?: 1
                    Pair("mat", ch)
                }
                hasNewTestament -> {
                    val firstNt = structure.keys.firstOrNull { it in ntBookIds } ?: "mat"
                    val ch = structure[firstNt]?.firstOrNull() ?: 1
                    Pair(firstNt, ch)
                }
                // Rule 3: In other cases -> first available page (book & chapter) from the translation
                else -> {
                    val firstBook = structure.keys.firstOrNull() ?: "gen"
                    val ch = structure[firstBook]?.firstOrNull() ?: 1
                    Pair(firstBook, ch)
                }
            }
        }

        if (forceDefault || preferredBookId.isBlank()) {
            return getDefaultBook()
        }

        // If preferredBookId is valid and exists in this translation, keep it
        if (structure.isNotEmpty()) {
            if (structure.containsKey(preferredBookId)) {
                val validChapters = structure[preferredBookId] ?: listOf(1)
                val currentCh = _uiState.value.selectedChapter
                val ch = if (validChapters.contains(currentCh)) currentCh else (validChapters.firstOrNull() ?: 1)
                return Pair(preferredBookId, ch)
            } else {
                // Requested book not found in this translation -> apply default book rule
                return getDefaultBook()
            }
        } else {
            val isNtOnly = translationId.contains("nt", ignoreCase = true)
            val isOtBook = BookCatalog.getBook(preferredBookId, lang).group == com.example.data.model.BookGroup.OT
            if (isNtOnly && isOtBook) {
                return Pair("mat", 1)
            }
            return Pair(preferredBookId, 1)
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val lang = _uiState.value.appLanguage
            try {
                repository.syncBooksFromApi(lang)
                val translationsList = repository.getTranslations(lang)
                val prefTransId = repository.preferences.selectedTranslation
                val chosenTrans = translationsList.find { it.id == prefTransId }
                    ?: translationsList.find { it.language == lang }
                    ?: translationsList.firstOrNull()

                val prefParallelId = repository.preferences.parallelTranslation
                val chosenParallel = translationsList.find { it.id == prefParallelId }
                    ?: translationsList.firstOrNull { it.language == lang && it.id != chosenTrans?.id }
                    ?: translationsList.firstOrNull { it.id != chosenTrans?.id }

                // Apply initial book resolution
                val (resolvedBookId, resolvedChapter) = if (chosenTrans != null) {
                    resolveBookForTranslation(chosenTrans.id, repository.preferences.selectedBook)
                } else {
                    Pair(repository.preferences.selectedBook, repository.preferences.selectedChapter)
                }
                val resolvedBook = repository.getBook(resolvedBookId, lang)

                _uiState.update {
                    it.copy(
                        translations = translationsList,
                        selectedTranslation = chosenTrans,
                        parallelTranslation = chosenParallel,
                        selectedBook = resolvedBook,
                        selectedChapter = resolvedChapter,
                        availableChapters = (1..resolvedBook.chapterCount).toList(),
                        chapterCompTranslations = repository.preferences.comparisonTranslations.toList()
                    )
                }

                loadVerses()
                loadNotes()
                loadRecentSearches()
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "Błąd wczytywania tłumaczeń: ${e.message}") }
            }
        }
    }

    fun loadVerses() {
        val state = _uiState.value
        val trans = state.selectedTranslation ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val versesList = repository.getVerses(
                    language = state.appLanguage,
                    translationId = trans.id,
                    bookId = state.selectedBook.id,
                    chapterId = state.selectedChapter
                )
                _uiState.update { it.copy(verses = versesList, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, toastMessage = "Błąd wczytywania wersetów: ${e.message}") }
            }
        }

        if (_uiState.value.isParallelReading) {
            loadParallelVerses()
        }
    }

    fun loadParallelVerses() {
        val state = _uiState.value
        val parallelTrans = state.parallelTranslation ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isParallelLoading = true) }
            kotlinx.coroutines.delay(200)
            try {
                val pVerses = repository.getVerses(
                    language = state.appLanguage,
                    translationId = parallelTrans.id,
                    bookId = state.selectedBook.id,
                    chapterId = state.selectedChapter
                )
                _uiState.update { it.copy(parallelVerses = pVerses, isParallelLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isParallelLoading = false) }
            }
        }
    }

    fun toggleParallelReading(enabled: Boolean? = null) {
        val current = _uiState.value.isParallelReading
        val next = enabled ?: !current
        repository.preferences.isParallelReading = next
        _uiState.update { it.copy(isParallelReading = next) }
        if (next) {
            if (_uiState.value.parallelTranslation == null) {
                val currentTransId = _uiState.value.selectedTranslation?.id
                val parallel = _uiState.value.translations.find {
                    it.language == _uiState.value.appLanguage && it.id != currentTransId
                } ?: _uiState.value.translations.firstOrNull { it.id != currentTransId }
                _uiState.update { it.copy(parallelTranslation = parallel) }
                if (parallel != null) {
                    repository.preferences.parallelTranslation = parallel.id
                }
            }
            loadParallelVerses()
        }
    }

    fun selectParallelTranslation(translation: Translation) {
        repository.preferences.parallelTranslation = translation.id
        _uiState.update { it.copy(parallelTranslation = translation) }
        loadParallelVerses()
    }

    fun swapParallelTranslations() {
        val state = _uiState.value
        val t1 = state.selectedTranslation ?: return
        val t2 = state.parallelTranslation ?: return
        val v1 = state.verses
        val v2 = state.parallelVerses

        repository.preferences.selectedTranslation = t2.id
        repository.preferences.parallelTranslation = t1.id

        _uiState.update {
            it.copy(
                selectedTranslation = t2,
                parallelTranslation = t1,
                verses = v2,
                parallelVerses = v1
            )
        }
    }

    fun toggleParallelLayout(columns: Boolean? = null) {
        val current = _uiState.value.parallelLayoutColumns
        val next = columns ?: !current
        repository.preferences.parallelLayoutColumns = next
        _uiState.update { it.copy(parallelLayoutColumns = next) }
    }

    fun toggleParallelDifferences(show: Boolean? = null) {
        val current = _uiState.value.parallelShowDifferences
        val next = show ?: !current
        repository.preferences.parallelShowDifferences = next
        _uiState.update { it.copy(parallelShowDifferences = next) }
    }

    fun selectBook(book: BookInfo) {
        repository.preferences.selectedBook = book.id
        val chapters = (1..book.chapterCount).toList()
        _uiState.update {
            it.copy(
                selectedBook = book,
                availableChapters = chapters,
                selectedChapter = 1,
                highlightedVerse = null
            )
        }
        repository.preferences.selectedChapter = 1
        loadVerses()
    }

    fun selectChapter(chapter: Int) {
        repository.preferences.selectedChapter = chapter
        _uiState.update { it.copy(selectedChapter = chapter, highlightedVerse = null) }
        loadVerses()
    }

    fun selectTranslation(translation: Translation) {
        repository.preferences.selectedTranslation = translation.id
        _uiState.update { it.copy(selectedTranslation = translation) }
        viewModelScope.launch {
            val currentBookId = _uiState.value.selectedBook.id
            val (resolvedBookId, resolvedChapter) = resolveBookForTranslation(translation.id, currentBookId)
            if (resolvedBookId != currentBookId) {
                val lang = _uiState.value.appLanguage
                val newBook = repository.getBook(resolvedBookId, lang)
                repository.preferences.selectedBook = resolvedBookId
                repository.preferences.selectedChapter = resolvedChapter
                _uiState.update {
                    it.copy(
                        selectedBook = newBook,
                        availableChapters = (1..newBook.chapterCount).toList(),
                        selectedChapter = resolvedChapter,
                        highlightedVerse = null
                    )
                }
            }
            loadVerses()
        }
    }

    fun toggleFavorite(translationId: String) {
        viewModelScope.launch {
            val list = _uiState.value.translations
            val item = list.find { it.id == translationId } ?: return@launch
            val newFav = !item.isFavorite
            repository.toggleFavorite(translationId, newFav)
            _uiState.update { state ->
                state.copy(
                    translations = state.translations.map {
                        if (it.id == translationId) it.copy(isFavorite = newFav) else it
                    }
                )
            }
        }
    }

    fun nextChapter() {
        val current = _uiState.value.selectedChapter
        val max = _uiState.value.selectedBook.chapterCount
        if (current < max) {
            selectChapter(current + 1)
        }
    }

    fun prevChapter() {
        val current = _uiState.value.selectedChapter
        if (current > 1) {
            selectChapter(current - 1)
        }
    }

    fun navigateTo(bookId: String, chapterId: Int, verseId: Int) {
        val lang = _uiState.value.appLanguage
        val book = repository.getBook(bookId, lang)
        repository.preferences.selectedBook = bookId
        repository.preferences.selectedChapter = chapterId
        _uiState.update {
            it.copy(
                selectedBook = book,
                availableChapters = (1..book.chapterCount).toList(),
                selectedChapter = chapterId,
                highlightedVerse = verseId
            )
        }
        loadVerses()
    }

    fun openVerseActions(verse: Verse) {
        _uiState.update { it.copy(activeVerseForActions = verse) }
    }

    fun closeVerseActions() {
        _uiState.update { it.copy(activeVerseForActions = null) }
    }

    fun openVerseComparison(verse: Verse) {
        val state = _uiState.value
        val baseTrans = state.selectedTranslation
        val parallelTrans = state.parallelTranslation

        // Immediately seed base verse so dialog opens instantly with content!
        val initialMap = mutableMapOf<String, String>()
        val baseVerseText = if (verse.text.isNotBlank()) {
            verse.text
        } else {
            state.verses.find { it.number == verse.number }?.text ?: ""
        }

        if (baseTrans != null && baseVerseText.isNotBlank()) {
            initialMap[baseTrans.id] = baseVerseText
        }
        if (state.isParallelReading && parallelTrans != null) {
            val pVerse = state.parallelVerses.find { it.number == verse.number }
            if (pVerse != null && pVerse.text.isNotBlank()) {
                initialMap[parallelTrans.id] = pVerse.text
            }
        }

        // Determine targets: preferences or favorites or top translations in current language
        val prefTargets = repository.preferences.comparisonTranslations.filter { it != baseTrans?.id }
        val targetList = mutableListOf<String>()
        if (prefTargets.isNotEmpty()) {
            targetList.addAll(prefTargets)
        } else {
            val favs = state.translations.filter { it.isFavorite && it.id != baseTrans?.id }.map { it.id }
            if (favs.isNotEmpty()) {
                targetList.addAll(favs.take(state.comparisonLimit))
            } else {
                val sameLang = state.translations.filter {
                    it.language.equals(state.appLanguage, ignoreCase = true) && it.id != baseTrans?.id
                }.map { it.id }
                targetList.addAll(sameLang.take(3))
            }
        }
        if (parallelTrans != null && parallelTrans.id != baseTrans?.id && !targetList.contains(parallelTrans.id)) {
            targetList.add(parallelTrans.id)
        }

        val finalTargets = targetList.distinct().take(state.comparisonLimit.coerceAtLeast(3))

        _uiState.update {
            it.copy(
                compareVerseNumber = verse.number,
                comparedVerses = initialMap,
                comparisonTargetTranslations = finalTargets,
                activeVerseForActions = null,
                isComparingVerse = true
            )
        }

        loadComparisonForVerse(verse.number, finalTargets, initialMap)
    }

    private fun loadComparisonForVerse(
        verseNum: Int,
        targets: List<String>,
        existingMap: Map<String, String>
    ) {
        val state = _uiState.value
        val baseTrans = state.selectedTranslation

        viewModelScope.launch {
            _uiState.update { it.copy(isComparingVerse = true) }
            val map = existingMap.toMutableMap()

            // Ensure base translation is present
            if (baseTrans != null && !map.containsKey(baseTrans.id)) {
                val baseText = state.verses.find { it.number == verseNum }?.text ?: ""
                if (baseText.isNotBlank()) {
                    map[baseTrans.id] = baseText
                }
            }

            for (tid in targets) {
                if (map.containsKey(tid) && map[tid] != "[NOT_FOUND]") continue
                try {
                    val verses = repository.getVerses(
                        language = state.appLanguage,
                        translationId = tid,
                        bookId = state.selectedBook.id,
                        chapterId = state.selectedChapter
                    )
                    val found = verses.find { it.number == verseNum }
                    if (found != null && found.text.isNotBlank()) {
                        map[tid] = found.text
                    } else {
                        map[tid] = "[NOT_FOUND]"
                    }
                } catch (e: Exception) {
                    map[tid] = "[NOT_FOUND]"
                }
                _uiState.update { curr ->
                    curr.copy(comparedVerses = map.toMap())
                }
                kotlinx.coroutines.delay(20)
            }
            _uiState.update { it.copy(isComparingVerse = false) }
        }
    }

    fun comparePreviousVerse() {
        val currentVerse = _uiState.value.compareVerseNumber ?: return
        if (currentVerse > 1) {
            val newNum = currentVerse - 1
            val baseTrans = _uiState.value.selectedTranslation
            val initialMap = mutableMapOf<String, String>()
            val baseText = _uiState.value.verses.find { it.number == newNum }?.text ?: ""
            if (baseTrans != null && baseText.isNotBlank()) {
                initialMap[baseTrans.id] = baseText
            }
            if (_uiState.value.isParallelReading && _uiState.value.parallelTranslation != null) {
                val pText = _uiState.value.parallelVerses.find { it.number == newNum }?.text ?: ""
                if (pText.isNotBlank()) {
                    initialMap[_uiState.value.parallelTranslation!!.id] = pText
                }
            }
            val targets = _uiState.value.comparisonTargetTranslations
            _uiState.update { it.copy(compareVerseNumber = newNum, comparedVerses = initialMap) }
            loadComparisonForVerse(newNum, targets, initialMap)
        }
    }

    fun compareNextVerse() {
        val currentVerse = _uiState.value.compareVerseNumber ?: return
        val maxVerse = _uiState.value.verses.maxOfOrNull { it.number } ?: 999
        if (currentVerse < maxVerse) {
            val newNum = currentVerse + 1
            val baseTrans = _uiState.value.selectedTranslation
            val initialMap = mutableMapOf<String, String>()
            val baseText = _uiState.value.verses.find { it.number == newNum }?.text ?: ""
            if (baseTrans != null && baseText.isNotBlank()) {
                initialMap[baseTrans.id] = baseText
            }
            if (_uiState.value.isParallelReading && _uiState.value.parallelTranslation != null) {
                val pText = _uiState.value.parallelVerses.find { it.number == newNum }?.text ?: ""
                if (pText.isNotBlank()) {
                    initialMap[_uiState.value.parallelTranslation!!.id] = pText
                }
            }
            val targets = _uiState.value.comparisonTargetTranslations
            _uiState.update { it.copy(compareVerseNumber = newNum, comparedVerses = initialMap) }
            loadComparisonForVerse(newNum, targets, initialMap)
        }
    }

    fun addComparisonTranslation(translationId: String) {
        val currentVerse = _uiState.value.compareVerseNumber ?: return
        val targets = (_uiState.value.comparisonTargetTranslations + translationId).distinct()
        val set = repository.preferences.comparisonTranslations.toMutableSet()
        set.add(translationId)
        repository.preferences.comparisonTranslations = set
        _uiState.update { it.copy(comparisonTargetTranslations = targets) }
        loadComparisonForVerse(currentVerse, listOf(translationId), _uiState.value.comparedVerses)
    }

    fun removeComparisonTranslation(translationId: String) {
        val targets = _uiState.value.comparisonTargetTranslations.filter { it != translationId }
        val set = repository.preferences.comparisonTranslations.toMutableSet()
        set.remove(translationId)
        repository.preferences.comparisonTranslations = set
        _uiState.update { curr ->
            curr.copy(
                comparisonTargetTranslations = targets,
                comparedVerses = curr.comparedVerses.filterKeys { it != translationId }
            )
        }
    }

    fun closeVerseComparison() {
        _uiState.update {
            it.copy(
                compareVerseNumber = null,
                comparedVerses = emptyMap(),
                comparisonTargetTranslations = emptyList(),
                isComparingVerse = false
            )
        }
    }

    fun toggleShowDifferences() {
        _uiState.update { it.copy(showDifferences = !it.showDifferences) }
    }

    fun setDiffMode(mode: DiffMode) {
        repository.preferences.diffMode = mode.name
        _uiState.update { it.copy(diffMode = mode) }
    }

    fun setComparisonLimit(limit: Int) {
        val clamped = limit.coerceIn(2, 6)
        repository.preferences.comparisonLimit = clamped
        _uiState.update { it.copy(comparisonLimit = clamped) }
    }

    fun loadChapterComparison(translationIds: List<String>) {
        val state = _uiState.value
        repository.preferences.comparisonTranslations = translationIds.toSet()
        _uiState.update {
            it.copy(
                chapterCompTranslations = translationIds,
                isChapterCompLoading = true,
                chapterCompVerses = emptyMap()
            )
        }
        viewModelScope.launch {
            try {
                val map = mutableMapOf<String, List<Verse>>()
                for (tid in translationIds) {
                    val vList = repository.getVerses(
                        language = state.appLanguage,
                        translationId = tid,
                        bookId = state.selectedBook.id,
                        chapterId = state.selectedChapter
                    )
                    map[tid] = vList
                }
                _uiState.update { it.copy(chapterCompVerses = map, isChapterCompLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isChapterCompLoading = false, toastMessage = "Comparison failed: ${e.message}")
                }
            }
        }
    }

    fun startEditingNote(verse: Verse) {
        val state = _uiState.value
        val transId = state.selectedTranslation?.id
        viewModelScope.launch {
            val existing = repository.getNotesForChapter(
                bookId = state.selectedBook.id,
                chapterId = state.selectedChapter,
                translationId = transId ?: ""
            ).find { it.verseId == verse.number }

            val note = existing ?: VerseNote(
                translationId = transId,
                bookId = state.selectedBook.id,
                chapterId = state.selectedChapter,
                verseId = verse.number,
                content = "",
                isGlobal = true
            )
            _uiState.update { it.copy(editingNote = note, activeVerseForActions = null) }
        }
    }

    fun closeNoteEditor() {
        _uiState.update { it.copy(editingNote = null) }
    }

    fun saveNote(content: String, isGlobal: Boolean) {
        val note = _uiState.value.editingNote ?: return
        viewModelScope.launch {
            val updated = note.copy(
                content = content,
                isGlobal = isGlobal,
                translationId = if (isGlobal) null else _uiState.value.selectedTranslation?.id,
                timestamp = System.currentTimeMillis()
            )
            repository.saveNote(updated)
            _uiState.update { it.copy(editingNote = null, toastMessage = "Note saved") }
            loadVerses()
            loadNotes()
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            _uiState.update { it.copy(editingNote = null, toastMessage = "Note deleted") }
            loadVerses()
            loadNotes()
        }
    }

    private fun loadNotes() {
        viewModelScope.launch {
            val notes = repository.getAllNotes()
            _uiState.update { it.copy(allNotes = notes) }
        }
    }

    fun exportNotesXml(): String {
        val notes = _uiState.value.allNotes
        val sb = StringBuilder()
        sb.append("<notes>\n")
        val grouped = notes.groupBy { if (it.isGlobal || it.translationId.isNullOrBlank()) "_global" else it.translationId }
        for ((transId, nList) in grouped) {
            sb.append("  <translation id=\"$transId\">\n")
            for (note in nList) {
                val escaped = note.content
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                sb.append("    <note book=\"${note.bookId}\" chapter=\"${note.chapterId}\" verse=\"${note.verseId}\">$escaped</note>\n")
            }
            sb.append("  </translation>\n")
        }
        sb.append("</notes>\n")
        return sb.toString()
    }

    fun importNotesXml(xml: String): Int {
        var count = 0
        try {
            val transRegex = Regex("""<translation\s+id="([^"]+)">([\s\S]*?)</translation>""")
            val noteRegex = Regex("""<note\s+book="([^"]+)"\s+chapter="(\d+)"\s+verse="(\d+)">([\s\S]*?)</note>""")
            val transMatches = transRegex.findAll(xml)
            viewModelScope.launch {
                for (tm in transMatches) {
                    val transId = tm.groupValues[1]
                    val isGlobal = transId == "_global" || transId.isBlank()
                    val actualTransId = if (isGlobal) "" else transId
                    val notesBlock = tm.groupValues[2]
                    for (nm in noteRegex.findAll(notesBlock)) {
                        val bookId = nm.groupValues[1]
                        val chapterId = nm.groupValues[2].toIntOrNull() ?: 1
                        val verseId = nm.groupValues[3].toIntOrNull() ?: 1
                        val content = nm.groupValues[4]
                            .replace("&quot;", "\"")
                            .replace("&gt;", ">")
                            .replace("&lt;", "<")
                            .replace("&amp;", "&")
                        repository.saveNote(
                            VerseNote(
                                bookId = bookId,
                                chapterId = chapterId,
                                verseId = verseId,
                                translationId = if (isGlobal) null else actualTransId,
                                content = content,
                                isGlobal = isGlobal,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        count++
                    }
                }
                loadNotes()
                loadVerses()
                _uiState.update { it.copy(toastMessage = "Zaimportowano $count notatek") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(toastMessage = "Błąd importu XML: ${e.message}") }
        }
        return count
    }

    fun startErrorReport(verse: Verse) {
        _uiState.update { it.copy(reportingVerse = verse, activeVerseForActions = null) }
    }

    fun closeErrorReport() {
        _uiState.update { it.copy(reportingVerse = verseOrNull()) }
    }

    private fun verseOrNull(): Verse? = null

    fun submitErrorReport(
        name: String,
        email: String,
        notes: String,
        content: String,
        errorType: String
    ) {
        val state = _uiState.value
        val verse = state.reportingVerse ?: return
        val trans = state.selectedTranslation?.id ?: ""
        _uiState.update { it.copy(isReportSubmitting = true) }
        viewModelScope.launch {
            val report = ErrorReport(
                name = name,
                email = email,
                notes = notes,
                content = content,
                originalContent = verse.text,
                translation = trans,
                book = state.selectedBook.id,
                chapter = state.selectedChapter,
                verse = verse.number,
                errorType = errorType
            )
            val success = repository.submitReport(state.appLanguage, report)
            _uiState.update {
                it.copy(
                    isReportSubmitting = false,
                    reportingVerse = null,
                    toastMessage = if (success) "Report submitted successfully!" else "Report failed to send"
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSearchScopeChanged(scope: SearchScope) {
        _uiState.update { it.copy(searchScope = scope) }
    }

    fun executeSearch() {
        val state = _uiState.value
        val q = state.searchQuery.trim()
        if (q.isBlank()) return
        val trans = state.selectedTranslation?.id ?: return
        _uiState.update { it.copy(isSearching = true) }
        viewModelScope.launch {
            try {
                val results = repository.search(state.appLanguage, trans, q)
                val filtered = when (state.searchScope) {
                    SearchScope.ALL -> results
                    SearchScope.OT -> results.filter { sr ->
                        val b = repository.getBook(sr.book, state.appLanguage)
                        b.group == com.example.data.model.BookGroup.OT
                    }
                    SearchScope.NT -> results.filter { sr ->
                        val b = repository.getBook(sr.book, state.appLanguage)
                        b.group == com.example.data.model.BookGroup.NT
                    }
                }
                _uiState.update { it.copy(searchResults = filtered, isSearching = false) }
                loadRecentSearches()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearching = false, toastMessage = "Search error: ${e.message}") }
            }
        }
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            val recent = repository.getRecentSearches()
            _uiState.update { it.copy(recentSearches = recent) }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
            _uiState.update { it.copy(recentSearches = emptyList()) }
        }
    }

    fun setTextSize(size: TextSize) {
        repository.preferences.textSize = size
        _uiState.update { it.copy(textSize = size) }
    }

    fun setFontFamily(family: TextFontFamily) {
        repository.preferences.fontFamily = family
        _uiState.update { it.copy(fontFamily = family) }
    }

    fun setThemeMode(mode: ThemeMode) {
        repository.preferences.themeMode = mode
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setDarkVariant(variant: DarkVariant) {
        repository.preferences.darkVariant = variant
        _uiState.update { it.copy(darkVariant = variant) }
    }

    fun setZenMode(enabled: Boolean) {
        repository.preferences.zenMode = enabled
        _uiState.update { it.copy(zenMode = enabled) }
    }

    fun setContinuousText(enabled: Boolean) {
        repository.preferences.continuousText = enabled
        _uiState.update { it.copy(continuousText = enabled) }
    }

    fun setHideVerseNumbers(enabled: Boolean) {
        repository.preferences.hideVerseNumbers = enabled
        _uiState.update { it.copy(hideVerseNumbers = enabled) }
    }

    fun setAppLanguage(lang: String) {
        repository.preferences.appLanguage = lang
        viewModelScope.launch {
            repository.syncBooksFromApi(lang)
            val currentBookId = _uiState.value.selectedBook.id
            val updatedBook = repository.getBook(currentBookId, lang)
            _uiState.update {
                it.copy(
                    appLanguage = lang,
                    selectedBook = updatedBook
                )
            }
            loadInitialData()
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    // --- Offline Cache Management (Room Database) ---

    fun clearOfflineCache() {
        viewModelScope.launch {
            try {
                repository.clearRoomCache()
                val lang = _uiState.value.appLanguage
                _uiState.update { it.copy(toastMessage = Strings.get("cache_cleared", lang)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "Błąd czyszczenia cache: ${e.message}") }
            }
        }
    }

    fun downloadCurrentBookOffline() {
        val state = _uiState.value
        val trans = state.selectedTranslation ?: return
        val book = state.selectedBook
        val lang = state.appLanguage

        _uiState.update {
            it.copy(
                isDownloadingBookOffline = true,
                offlineDownloadProgress = 0 to book.chapterCount
            )
        }

        viewModelScope.launch {
            try {
                repository.downloadBookToCache(
                    language = lang,
                    translationId = trans.id,
                    bookId = book.id,
                    totalChapters = book.chapterCount
                ) { current, total ->
                    _uiState.update { it.copy(offlineDownloadProgress = current to total) }
                }
                _uiState.update {
                    it.copy(
                        toastMessage = "${book.name} (${trans.name}) - ${Strings.get("book_downloaded", lang)}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "Błąd pobierania offline: ${e.message}") }
            } finally {
                _uiState.update {
                    it.copy(
                        isDownloadingBookOffline = false,
                        offlineDownloadProgress = null
                    )
                }
            }
        }
    }

    // --- GitHub Updates Check ---

    fun checkForAppUpdates() {
        _uiState.update { it.copy(isCheckingUpdate = true) }
        viewModelScope.launch {
            if (updateService != null) {
                val status = updateService.checkLatestRelease()
                val result = when (status) {
                    is UpdateStatus.UpdateAvailable -> {
                        updateService.showUpdateNotification(status.release)
                        UpdateCheckResult.UpdateAvailable(
                            latestVersion = status.release.tagName,
                            releaseName = status.release.releaseName,
                            releaseNotes = status.release.releaseNotes,
                            apkDownloadUrl = status.release.apkDownloadUrl,
                            releaseUrl = status.release.releasePageUrl
                        )
                    }
                    is UpdateStatus.UpToDate -> UpdateCheckResult.UpToDate
                    is UpdateStatus.Error -> UpdateCheckResult.Error(status.message)
                    else -> UpdateCheckResult.UpToDate
                }
                _uiState.update {
                    it.copy(
                        isCheckingUpdate = false,
                        updateCheckResult = result
                    )
                }
            } else {
                val result = AppUpdateManager.checkForUpdates(
                    currentVersion = BuildConfig.VERSION_NAME,
                    repoOwnerAndName = AppUpdateManager.GITHUB_REPO
                )
                _uiState.update {
                    it.copy(
                        isCheckingUpdate = false,
                        updateCheckResult = result
                    )
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(updateCheckResult = null) }
    }
}
