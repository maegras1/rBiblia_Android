package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.room.BookReadingProgressEntity
import com.example.data.local.room.ChapterReadingProgressEntity
import com.example.data.model.BookCatalog
import com.example.data.model.BookCategory
import com.example.data.model.BookGroup
import com.example.data.model.BookInfo
import com.example.ui.util.Strings

private enum class SelectorStep {
    BOOK,
    CHAPTER,
    VERSE
}

fun categoryAccentColor(category: BookCategory): Color = when (category) {
    BookCategory.PENTATEUCH, BookCategory.GOSPELS_ACTS -> Color(0xFFD99B26)
    BookCategory.HISTORICAL, BookCategory.PAULINE_EPISTLES -> Color(0xFF2E9E7B)
    BookCategory.WISDOM -> Color(0xFF7E6BC4)
    BookCategory.MAJOR_PROPHETS, BookCategory.APOCALYPSE -> Color(0xFFD15B5B)
    BookCategory.MINOR_PROPHETS, BookCategory.GENERAL_EPISTLES -> Color(0xFF3B82C4)
    BookCategory.DEUTEROCANONICAL -> Color(0xFFB87B4B)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSelectorDialog(
    currentLanguage: String,
    currentBook: BookInfo,
    currentChapter: Int = 1,
    bookProgressMap: Map<String, BookReadingProgressEntity> = emptyMap(),
    currentBookChaptersProgress: Map<Int, ChapterReadingProgressEntity> = emptyMap(),
    viewMode: String = "CARDS",
    onViewModeChange: (String) -> Unit = {},
    onBookSelected: (BookInfo) -> Unit,
    onNavigateTo: ((bookId: String, chapter: Int, verse: Int?) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val allBooks = remember(currentLanguage) { BookCatalog.getAllBooks(currentLanguage) }

    var currentStep by remember { mutableStateOf(SelectorStep.BOOK) }
    var pendingBook by remember { mutableStateOf(currentBook) }
    var pendingChapter by remember { mutableIntStateOf(currentChapter) }
    var pickVerseAfterChapter by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<BookCategory?>(null) }

    var selectedTabIndex by remember {
        mutableIntStateOf(
            when (currentBook.group) {
                BookGroup.OT -> 0
                BookGroup.NT -> 1
                BookGroup.DC -> 2
            }
        )
    }
    var searchQuery by remember { mutableStateOf("") }

    val targetGroup = when (selectedTabIndex) {
        0 -> BookGroup.OT
        1 -> BookGroup.NT
        else -> BookGroup.DC
    }

    val categoriesInTab = remember(allBooks, targetGroup) {
        allBooks.filter { it.group == targetGroup }.map { it.category }.distinct()
    }

    val filteredBooks = remember(allBooks, selectedTabIndex, searchQuery, selectedCategoryFilter) {
        val query = searchQuery.trim().lowercase()
        allBooks.filter { book ->
            val matchesGroup = searchQuery.isNotEmpty() || book.group == targetGroup
            val matchesCategory = searchQuery.isNotEmpty() ||
                selectedCategoryFilter == null ||
                book.category == selectedCategoryFilter
            val matchesQuery = query.isEmpty() ||
                book.name.lowercase().contains(query) ||
                book.sigla.lowercase().contains(query) ||
                book.id.lowercase().contains(query)
            matchesGroup && matchesCategory && matchesQuery
        }
    }

    val groupedBooks = remember(filteredBooks) {
        filteredBooks.groupBy { it.category }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(enabled = currentStep != SelectorStep.BOOK) {
            currentStep = when (currentStep) {
                SelectorStep.VERSE -> SelectorStep.CHAPTER
                SelectorStep.CHAPTER -> SelectorStep.BOOK
                SelectorStep.BOOK -> SelectorStep.BOOK
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // --- Top Header with Back Button, Title & Density Toggle ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (currentStep != SelectorStep.BOOK) {
                            IconButton(
                                onClick = {
                                    currentStep = when (currentStep) {
                                        SelectorStep.VERSE -> SelectorStep.CHAPTER
                                        SelectorStep.CHAPTER -> SelectorStep.BOOK
                                        SelectorStep.BOOK -> SelectorStep.BOOK
                                    }
                                },
                                modifier = Modifier.testTag("selector_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = Strings.get("back_to_books", currentLanguage)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = when (currentStep) {
                                    SelectorStep.BOOK -> Strings.get("select_book", currentLanguage)
                                    SelectorStep.CHAPTER -> pendingBook.name
                                    SelectorStep.VERSE -> "${pendingBook.name} $pendingChapter"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentStep != SelectorStep.BOOK) {
                                Text(
                                    text = when (currentStep) {
                                        SelectorStep.CHAPTER -> Strings.get("select_chapter", currentLanguage)
                                        SelectorStep.VERSE -> Strings.get("select_verse", currentLanguage)
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // View Density Switcher (only shown on BOOK step)
                    if (currentStep == SelectorStep.BOOK) {
                        ViewDensitySwitcher(
                            currentMode = viewMode,
                            currentLanguage = currentLanguage,
                            onModeSelected = onViewModeChange
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- Interactive Breadcrumb Stepper Bar (Point 1) ---
                BreadcrumbStepperBar(
                    currentLanguage = currentLanguage,
                    currentStep = currentStep,
                    selectedBook = pendingBook,
                    selectedChapter = pendingChapter,
                    pickVerseEnabled = pickVerseAfterChapter,
                    onClickBookStep = { currentStep = SelectorStep.BOOK },
                    onClickChapterStep = { currentStep = SelectorStep.CHAPTER },
                    onToggleVerseStep = {
                        pickVerseAfterChapter = !pickVerseAfterChapter
                        if (pickVerseAfterChapter && currentStep == SelectorStep.CHAPTER) {
                            // Keep in chapter step until a chapter is tapped, or allow direct switch
                        } else if (!pickVerseAfterChapter && currentStep == SelectorStep.VERSE) {
                            currentStep = SelectorStep.CHAPTER
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.weight(1f),
                    label = "selector_step_transition"
                ) { step ->
                    when (step) {
                        SelectorStep.BOOK -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Search Box
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text(Strings.get("search_hint", currentLanguage)) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    trailingIcon = if (searchQuery.isNotEmpty()) {
                                        {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = null)
                                            }
                                        }
                                    } else null,
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("book_search_input")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Category Tabs (OT / NT / DC) when search is empty
                                if (searchQuery.isEmpty()) {
                                    PrimaryTabRow(
                                        selectedTabIndex = selectedTabIndex,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Tab(
                                            selected = selectedTabIndex == 0,
                                            onClick = {
                                                selectedTabIndex = 0
                                                selectedCategoryFilter = null
                                            },
                                            text = {
                                                Text(
                                                    text = Strings.get("old_testament", currentLanguage),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        )
                                        Tab(
                                            selected = selectedTabIndex == 1,
                                            onClick = {
                                                selectedTabIndex = 1
                                                selectedCategoryFilter = null
                                            },
                                            text = {
                                                Text(
                                                    text = Strings.get("new_testament", currentLanguage),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        )
                                        Tab(
                                            selected = selectedTabIndex == 2,
                                            onClick = {
                                                selectedTabIndex = 2
                                                selectedCategoryFilter = null
                                            },
                                            text = {
                                                Text(
                                                    text = Strings.get("deuterocanonical", currentLanguage),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        )
                                    }

                                    // Literary Category Color Chips Bar (Point 2)
                                    if (categoriesInTab.size > 1) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState())
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            for (cat in categoriesInTab) {
                                                val accent = categoryAccentColor(cat)
                                                val isCatSelected = selectedCategoryFilter == cat
                                                FilterChip(
                                                    selected = isCatSelected,
                                                    onClick = {
                                                        selectedCategoryFilter = if (isCatSelected) null else cat
                                                    },
                                                    label = {
                                                        Text(
                                                            text = cat.getLabel(currentLanguage),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(accent)
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = accent.copy(alpha = 0.2f)
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }

                                // Book selection handler (Point 1 integrated flow)
                                val handleBookClick: (BookInfo) -> Unit = { book ->
                                    pendingBook = book
                                    if (onNavigateTo != null) {
                                        if (book.chapterCount <= 1 && !pickVerseAfterChapter) {
                                            // 1-chapter book: jump immediately to chapter 1
                                            onNavigateTo(book.id, 1, null)
                                            onDismiss()
                                        } else if (book.chapterCount <= 1 && pickVerseAfterChapter) {
                                            pendingChapter = 1
                                            currentStep = SelectorStep.VERSE
                                        } else {
                                            pendingChapter = if (book.id == currentBook.id) currentChapter else 1
                                            currentStep = SelectorStep.CHAPTER
                                        }
                                    } else {
                                        onBookSelected(book)
                                    }
                                }

                                // Render according to selected density mode (Point 2 & Point 3)
                                val gridColumns = when (viewMode) {
                                    "COMPACT" -> GridCells.Adaptive(minSize = 60.dp)
                                    "LIST" -> GridCells.Fixed(1)
                                    else -> GridCells.Adaptive(minSize = 102.dp)
                                }

                                LazyVerticalGrid(
                                    columns = gridColumns,
                                    contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (viewMode == "COMPACT" || searchQuery.isNotEmpty()) {
                                        // In COMPACT 5x mode, show all books without section header breaks so the entire NT/OT fits on 1 screen
                                        items(
                                            items = filteredBooks,
                                            key = { it.id }
                                        ) { book ->
                                            BookGridItem(
                                                book = book,
                                                isSelected = book.id == currentBook.id,
                                                bookProgress = bookProgressMap[book.id],
                                                viewMode = viewMode,
                                                currentLanguage = currentLanguage,
                                                onClick = { handleBookClick(book) }
                                            )
                                        }
                                    } else {
                                        // In CARDS (3x) and LIST modes, group by Literary Category with colored section headers
                                        groupedBooks.forEach { (category, booksInCategory) ->
                                            item(
                                                key = "header_${category.name}",
                                                span = { GridItemSpan(maxLineSpan) }
                                            ) {
                                                CategorySectionHeader(
                                                    category = category,
                                                    count = booksInCategory.size,
                                                    currentLanguage = currentLanguage
                                                )
                                            }
                                            items(
                                                items = booksInCategory,
                                                key = { it.id }
                                            ) { book ->
                                                BookGridItem(
                                                    book = book,
                                                    isSelected = book.id == currentBook.id,
                                                    bookProgress = bookProgressMap[book.id],
                                                    viewMode = viewMode,
                                                    currentLanguage = currentLanguage,
                                                    onClick = { handleBookClick(book) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SelectorStep.CHAPTER -> {
                            val chapters = (1..pendingBook.chapterCount.coerceAtLeast(1)).toList()
                            val completedChaptersSet = remember(pendingBook.id, bookProgressMap, currentBookChaptersProgress) {
                                if (pendingBook.id == currentBook.id && currentBookChaptersProgress.isNotEmpty()) {
                                    currentBookChaptersProgress.filter { it.value.isCompleted }.keys
                                } else {
                                    bookProgressMap[pendingBook.id]
                                        ?.completedChaptersCsv
                                        ?.split(",")
                                        ?.mapNotNull { it.trim().toIntOrNull() }
                                        ?.toSet()
                                        ?: emptySet()
                                }
                            }
                            val lastReadChap = bookProgressMap[pendingBook.id]?.lastReadChapter ?: 0

                            Column(modifier = Modifier.fillMaxSize()) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 56.dp),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(chapters, key = { it }) { ch ->
                                        val isCurrent = pendingBook.id == currentBook.id && ch == currentChapter
                                        val isCompleted = completedChaptersSet.contains(ch)
                                        val isLastRead = ch == lastReadChap && !isCurrent
                                        val accent = categoryAccentColor(pendingBook.category)

                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = when {
                                                    isCurrent -> MaterialTheme.colorScheme.primary
                                                    isCompleted -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
                                                    isLastRead -> accent.copy(alpha = 0.18f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                }
                                            ),
                                            border = if (isLastRead) BorderStroke(1.5.dp, accent) else null,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .testTag("chapter_item_$ch")
                                                .clickable {
                                                    pendingChapter = ch
                                                    if (pickVerseAfterChapter) {
                                                        currentStep = SelectorStep.VERSE
                                                    } else {
                                                        if (onNavigateTo != null) {
                                                            onNavigateTo(pendingBook.id, ch, null)
                                                        } else {
                                                            onBookSelected(pendingBook)
                                                        }
                                                        onDismiss()
                                                    }
                                                }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(54.dp)
                                            ) {
                                                Text(
                                                    text = ch.toString(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        isCurrent -> MaterialTheme.colorScheme.onPrimary
                                                        isCompleted -> MaterialTheme.colorScheme.onSecondaryContainer
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )

                                                if (isCompleted) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Completed",
                                                        tint = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(4.dp)
                                                            .size(13.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SelectorStep.VERSE -> {
                            val maxVerses = remember(pendingBook.id, pendingChapter) {
                                estimateMaxVerses(pendingBook.id, pendingChapter)
                            }
                            val versesList = (1..maxVerses).toList()

                            Column(modifier = Modifier.fillMaxSize()) {
                                OutlinedButton(
                                    onClick = {
                                        onNavigateTo?.invoke(pendingBook.id, pendingChapter, null)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                        .testTag("read_chapter_from_start_button")
                                ) {
                                    Text(
                                        text = Strings.get("read_from_start", currentLanguage),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 54.dp),
                                    contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(versesList, key = { it }) { vNum ->
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .testTag("verse_selector_item_$vNum")
                                                .clickable {
                                                    onNavigateTo?.invoke(pendingBook.id, pendingChapter, vNum)
                                                    onDismiss()
                                                }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                            ) {
                                                Text(
                                                    text = vNum.toString(),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbStepperBar(
    currentLanguage: String,
    currentStep: SelectorStep,
    selectedBook: BookInfo,
    selectedChapter: Int,
    pickVerseEnabled: Boolean,
    onClickBookStep: () -> Unit,
    onClickChapterStep: () -> Unit,
    onToggleVerseStep: () -> Unit
) {
    val accent = categoryAccentColor(selectedBook.category)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Step 1: Book
                val isBookStep = currentStep == SelectorStep.BOOK
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isBookStep) MaterialTheme.colorScheme.primary else accent.copy(alpha = 0.18f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClickBookStep() }
                        .testTag("breadcrumb_step_book")
                ) {
                    Text(
                        text = "${Strings.get("step_book", currentLanguage)}: ${selectedBook.sigla}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isBookStep) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )

                // Step 2: Chapter
                val isChapterStep = currentStep == SelectorStep.CHAPTER
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isChapterStep) {
                        MaterialTheme.colorScheme.primary
                    } else if (currentStep == SelectorStep.VERSE) {
                        accent.copy(alpha = 0.18f)
                    } else {
                        Color.Transparent
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClickChapterStep() }
                        .testTag("breadcrumb_step_chapter")
                ) {
                    Text(
                        text = "${Strings.get("step_chapter", currentLanguage)}: $selectedChapter",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isChapterStep) FontWeight.Bold else FontWeight.Medium,
                        color = if (isChapterStep) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Optional Step 3 Toggle: Pick Verse
            FilterChip(
                selected = pickVerseEnabled || currentStep == SelectorStep.VERSE,
                onClick = onToggleVerseStep,
                label = {
                    Text(
                        text = "+ ${Strings.get("step_verse", currentLanguage)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier
                    .height(28.dp)
                    .testTag("toggle_verse_step_chip")
            )
        }
    }
}

@Composable
private fun ViewDensitySwitcher(
    currentMode: String,
    currentLanguage: String,
    onModeSelected: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(2.dp)
        ) {
            val modes = listOf(
                Triple("COMPACT", Icons.Default.Apps, Strings.get("view_compact", currentLanguage)),
                Triple("CARDS", Icons.Default.GridView, Strings.get("view_cards", currentLanguage)),
                Triple("LIST", Icons.AutoMirrored.Filled.ViewList, Strings.get("view_list", currentLanguage))
            )
            modes.forEach { (modeKey, icon, label) ->
                val selected = currentMode == modeKey
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onModeSelected(modeKey) }
                        .testTag("book_view_mode_${modeKey.lowercase()}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySectionHeader(
    category: BookCategory,
    count: Int,
    currentLanguage: String
) {
    val accent = categoryAccentColor(category)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category.getLabel(currentLanguage),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = accent.copy(alpha = 0.16f)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun BookGridItem(
    book: BookInfo,
    isSelected: Boolean,
    bookProgress: BookReadingProgressEntity?,
    viewMode: String,
    currentLanguage: String,
    onClick: () -> Unit
) {
    val accent = categoryAccentColor(book.category)
    val completedCount = bookProgress?.completedChaptersCount ?: 0
    val hasProgress = completedCount > 0 || (bookProgress?.lastReadChapter ?: 0) > 1
    val progressFraction = (completedCount.toFloat() / book.chapterCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    when (viewMode) {
        "COMPACT" -> {
            // 5-Column Compact Sigla Tile (fits entire NT or OT on screen without scrolling)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    }
                ),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else accent.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .testTag("book_item_${book.id}")
                    .clickable(onClick = onClick)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Category Accent Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(accent)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = book.sigla,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        if (hasProgress) {
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(2.5.dp),
                                color = accent,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }

        "LIST" -> {
            // 1-Column Detailed Row with Sigla badge, full name, category label & progress bar
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("book_item_${book.id}")
                    .clickable(onClick = onClick)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accent.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                            modifier = Modifier.size(width = 48.dp, height = 38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = book.sigla,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${book.category.getLabel(currentLanguage)} • ${book.chapterCount} ${Strings.get("chapters_short", currentLanguage)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (hasProgress) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.width(68.dp)
                        ) {
                            Text(
                                text = "$completedCount/${book.chapterCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = accent,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        }
                    }
                }
            }
        }

        else -> {
            // Default 3-Column CARDS Grid Item with Category Accent Strip
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    }
                ),
                border = if (isSelected) {
                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(0.5.dp, accent.copy(alpha = 0.35f))
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .testTag("book_item_${book.id}")
                    .clickable(onClick = onClick)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(accent)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = book.sigla,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = book.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (hasProgress) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$completedCount/${book.chapterCount}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = accent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(3.dp),
                                color = accent,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun estimateMaxVerses(bookId: String, chapter: Int): Int {
    if (bookId == "psa" && chapter == 119) return 176
    if (bookId == "num" && chapter == 7) return 89
    if (bookId == "luk" && chapter == 1) return 80
    if (bookId == "mat" && chapter == 26) return 75
    return 50
}
