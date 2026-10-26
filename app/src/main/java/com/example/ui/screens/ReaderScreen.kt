package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.room.PartialHighlightEntity
import com.example.data.model.BookInfo
import com.example.data.model.TextFontFamily
import com.example.data.model.TextSize
import com.example.data.model.Translation
import com.example.data.model.Verse
import com.example.ui.theme.HighlightNoteColor
import com.example.ui.theme.getFontFamily
import com.example.ui.util.DiffUtil
import com.example.ui.util.Strings
import kotlin.math.abs

data class FragmentSelectionState(
    val verse: Verse,
    val anchorStart: Int,
    val anchorEnd: Int,
    val startIndex: Int,
    val endIndex: Int
)

data class WordToken(
    val word: String,
    val startIndex: Int,
    val endIndex: Int
)

private fun extractWordTokens(text: String): List<WordToken> {
    val result = mutableListOf<WordToken>()
    var i = 0
    while (i < text.length) {
        while (i < text.length && text[i].isWhitespace()) i++
        if (i >= text.length) break
        val start = i
        while (i < text.length && !text[i].isWhitespace()) i++
        val end = i
        result.add(WordToken(text.substring(start, end), start, end))
    }
    return result
}

private fun findWordBoundsAt(text: String, charOffset: Int): Pair<Int, Int> {
    if (text.isEmpty()) return 0 to 0
    val tokens = extractWordTokens(text)
    if (tokens.isEmpty()) return 0 to text.length
    val safeOffset = charOffset.coerceIn(0, text.length)
    val containing = tokens.firstOrNull { safeOffset in it.startIndex..it.endIndex }
        ?: tokens.minByOrNull { token ->
            minOf(abs(safeOffset - token.startIndex), abs(safeOffset - token.endIndex))
        }
        ?: tokens.first()
    return containing.startIndex to containing.endIndex
}

private fun resolveContinuousTouch(
    offset: Offset,
    layoutResult: TextLayoutResult?,
    annotated: AnnotatedString,
    versesMap: Map<Int, Verse>
): Pair<Verse, Int>? {
    val lr = layoutResult ?: return null
    if (annotated.isEmpty()) return null
    val rawOffset = lr.getOffsetForPosition(offset).coerceIn(0, (annotated.length - 1).coerceAtLeast(0))

    val annotation = annotated.getStringAnnotations(tag = "VERSE_TEXT", start = rawOffset, end = rawOffset)
        .firstOrNull()
        ?: annotated.getStringAnnotations(
            tag = "VERSE_TEXT",
            start = (rawOffset - 1).coerceAtLeast(0),
            end = (rawOffset + 1).coerceAtMost(annotated.length)
        ).firstOrNull()
        ?: return null

    val parts = annotation.item.split(":")
    if (parts.size < 2) return null
    val verseNum = parts[0].toIntOrNull() ?: return null
    val textStart = parts[1].toIntOrNull() ?: return null
    val verse = versesMap[verseNum] ?: return null
    val relativeIndex = (rawOffset - textStart).coerceIn(0, verse.text.length)
    return verse to relativeIndex
}

private fun buildVerseTextAnnotated(
    verseText: String,
    partialHighlights: List<PartialHighlightEntity>,
    activeSelection: FragmentSelectionState?,
    isCurrentSelectedVerse: Boolean,
    selectionHighlightColor: Color
): AnnotatedString = buildAnnotatedString {
    append(verseText)
    for (ph in partialHighlights) {
        val s = ph.startIndex.coerceIn(0, verseText.length)
        val e = ph.endIndex.coerceIn(0, verseText.length)
        if (s < e) {
            val phColor = runCatching { Color(android.graphics.Color.parseColor(ph.colorHex)) }.getOrNull()
            if (phColor != null) {
                addStyle(
                    SpanStyle(background = phColor.copy(alpha = 0.46f)),
                    start = s,
                    end = e
                )
            }
        }
    }
    if (isCurrentSelectedVerse && activeSelection != null) {
        val selStart = activeSelection.startIndex.coerceIn(0, verseText.length)
        val selEnd = activeSelection.endIndex.coerceIn(0, verseText.length)
        if (selStart < selEnd) {
            addStyle(
                SpanStyle(
                    background = selectionHighlightColor.copy(alpha = 0.28f),
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                ),
                start = selStart,
                end = selEnd
            )
        }
    }
}

@Composable
fun ReaderScreen(
    currentLanguage: String,
    book: BookInfo,
    chapter: Int,
    translation: Translation?,
    verses: List<Verse>,
    isLoading: Boolean,
    isParallelReading: Boolean = false,
    parallelTranslation: Translation? = null,
    parallelVerses: List<Verse> = emptyList(),
    isParallelLoading: Boolean = false,
    parallelLayoutColumns: Boolean = true,
    parallelShowDifferences: Boolean = false,
    onSelectPrimaryTranslation: () -> Unit = {},
    onSelectParallelTranslation: () -> Unit = {},
    onSwapParallelTranslations: () -> Unit = {},
    onToggleParallelLayout: () -> Unit = {},
    onToggleParallelDifferences: () -> Unit = {},
    onCloseParallelReading: () -> Unit = {},
    onOpenBookSelector: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onToggleParallelReading: () -> Unit = {},
    textSize: TextSize,
    fontFamily: TextFontFamily,
    continuousText: Boolean,
    hideVerseNumbers: Boolean,
    zenMode: Boolean,
    highlightedVerse: Int?,
    verseHighlights: Map<Int, String> = emptyMap(),
    partialHighlights: Map<Int, List<PartialHighlightEntity>> = emptyMap(),
    activeFragmentVerse: Verse? = null,
    onSetPartialHighlight: (verseNumber: Int, startIndex: Int, endIndex: Int, colorHex: String, colorName: String) -> Unit = { _, _, _, _, _ -> },
    onRemovePartialHighlightRange: (verseNumber: Int, startIndex: Int, endIndex: Int) -> Unit = { _, _, _ -> },
    onDeletePartialHighlightById: (Long) -> Unit = {},
    onClearVersePartialHighlights: (Int) -> Unit = {},
    onDismissFragmentHighlighter: () -> Unit = {},
    isChapterCompleted: Boolean = false,
    onToggleChapterCompleted: () -> Unit = {},
    onVerseClick: (Verse) -> Unit,
    onNextChapter: () -> Unit,
    onPrevChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val baseFontSize = 17.sp * textSize.scale
    val numberFontSize = 12.sp * textSize.scale
    val font = getFontFamily(fontFamily)
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    var activeSelection by remember { mutableStateOf<FragmentSelectionState?>(null) }

    // Clear fragment selection when book or chapter changes
    LaunchedEffect(book.id, chapter, translation?.id) {
        activeSelection = null
    }

    // Open fragment selection if requested from VerseActionsBottomSheet ("Zakreśl fragment")
    LaunchedEffect(activeFragmentVerse) {
        if (activeFragmentVerse != null) {
            val bounds = findWordBoundsAt(activeFragmentVerse.text, 0)
            activeSelection = FragmentSelectionState(
                verse = activeFragmentVerse,
                anchorStart = bounds.first,
                anchorEnd = bounds.second,
                startIndex = bounds.first,
                endIndex = bounds.second
            )
        }
    }

    BackHandler(enabled = activeSelection != null) {
        activeSelection = null
        onDismissFragmentHighlighter()
    }

    // Scroll to highlighted verse when it changes
    LaunchedEffect(highlightedVerse, verses) {
        if (highlightedVerse != null && verses.isNotEmpty()) {
            if (continuousText) {
                listState.animateScrollToItem(0)
            } else {
                val index = verses.indexOfFirst { it.number == highlightedVerse }
                if (index != -1) {
                    listState.animateScrollToItem(index)
                }
            }
        } else if (verses.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    var totalDrag by remember { mutableFloatStateOf(0f) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    var hasHapticTriggered by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(activeSelection != null) {
                if (activeSelection != null) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        totalDrag = 0f
                        touchPosition = offset
                        hasHapticTriggered = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        totalDrag += dragAmount
                        touchPosition = change.position
                        if (!hasHapticTriggered && abs(totalDrag) >= 100f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            hasHapticTriggered = true
                        }
                    },
                    onDragEnd = {
                        if (totalDrag < -100f) {
                            onNextChapter() // swipe left -> next chapter
                        } else if (totalDrag > 100f) {
                            onPrevChapter() // swipe right -> prev chapter
                        }
                        totalDrag = 0f
                        touchPosition = null
                        hasHapticTriggered = false
                    },
                    onDragCancel = {
                        totalDrag = 0f
                        touchPosition = null
                        hasHapticTriggered = false
                    }
                )
            }
            .testTag("reader_screen_container")
    ) {
        if (isLoading && verses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Chapter Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${book.name} $chapter",
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (isParallelReading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Primary Translation Pill
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .clickable { onSelectPrimaryTranslation() }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = translation?.id?.uppercase() ?: "T1",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                // Swap Button
                                IconButton(
                                    onClick = onSwapParallelTranslations,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(horizontal = 2.dp)
                                        .testTag("swap_parallel_translations_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = Strings.get("swap_translations", currentLanguage),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Secondary Translation Pill
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .clickable { onSelectParallelTranslation() }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = parallelTranslation?.id?.uppercase() ?: Strings.get("select_parallel_translation", currentLanguage).take(6),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Layout Toggle (Columns vs Stacked)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (parallelLayoutColumns) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .clickable { onToggleParallelLayout() }
                                        .padding(vertical = 2.dp)
                                        .testTag("toggle_parallel_layout_button")
                                ) {
                                    Text(
                                        text = if (parallelLayoutColumns) "‖‖ Kolumny" else "≡ Wiersze",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (parallelLayoutColumns) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Diff Highlight Toggle
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (parallelShowDifferences) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clickable { onToggleParallelDifferences() }
                                        .padding(vertical = 2.dp)
                                        .testTag("toggle_parallel_diff_button")
                                ) {
                                    Text(
                                        text = Strings.get("highlight_diff", currentLanguage),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (parallelShowDifferences) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Close Button
                                IconButton(
                                    onClick = onCloseParallelReading,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close parallel reading",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else if (translation != null) {
                        Text(
                            text = translation.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Verses View: Parallel Reading Mode OR Single Translation Mode
                if (isParallelReading) {
                    val allVerseNumbers = remember(verses, parallelVerses) {
                        (verses.map { it.number } + parallelVerses.map { it.number }).distinct().sorted()
                    }

                    val versesMap = remember(verses) { verses.associateBy { it.number } }
                    val parallelMap = remember(parallelVerses) { parallelVerses.associateBy { it.number } }

                    if (parallelVerses.isEmpty() && !isParallelLoading && verses.isNotEmpty() && parallelTranslation != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onSelectParallelTranslation() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "ℹ ${Strings.get("parallel_missing_book", currentLanguage)} (${parallelTranslation.id.uppercase()}). Dotknij, aby zmienić.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("parallel_verses_list_view")
                    ) {
                        items(allVerseNumbers, key = { it }) { verseNum ->
                            val v1 = versesMap[verseNum]
                            val v2 = parallelMap[verseNum]
                            val isHighlighted = verseNum == highlightedVerse
                            val hasNote = v1?.hasNote == true || v2?.hasNote == true
                            val persistentHex = verseHighlights[verseNum]
                            val customHighlightColor = persistentHex?.let {
                                runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
                            }
                            val versePartials = partialHighlights[verseNum] ?: emptyList()

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        customHighlightColor != null -> customHighlightColor.copy(alpha = 0.28f)
                                        hasNote -> HighlightNoteColor
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    }
                                ),
                                border = if (customHighlightColor != null) {
                                    BorderStroke(1.5.dp, customHighlightColor.copy(alpha = 0.75f))
                                } else null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("parallel_verse_card_$verseNum")
                                    .clickable {
                                        val activeVerse = v1 ?: v2 ?: Verse(number = verseNum, text = "")
                                        onVerseClick(activeVerse)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        ) {
                                            Text(
                                                text = "${Strings.get("verse_numbers", currentLanguage).take(3)} $verseNum",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (hasNote) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = "Has note",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    val v1Annotated = buildVerseTextAnnotated(
                                        verseText = v1?.text ?: if (isLoading) "..." else "—",
                                        partialHighlights = if (v1 != null) versePartials else emptyList(),
                                        activeSelection = activeSelection,
                                        isCurrentSelectedVerse = activeSelection?.verse?.number == verseNum,
                                        selectionHighlightColor = MaterialTheme.colorScheme.primary
                                    )

                                    if (parallelLayoutColumns) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = translation?.id?.uppercase() ?: "T1",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = v1Annotated,
                                                    fontFamily = font,
                                                    fontSize = baseFontSize,
                                                    lineHeight = (baseFontSize.value * 1.45f).sp,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                            }

                                            VerticalDivider(
                                                modifier = Modifier
                                                    .height(48.dp)
                                                    .align(Alignment.CenterVertically),
                                                color = MaterialTheme.colorScheme.outlineVariant
                                            )

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = parallelTranslation?.id?.uppercase() ?: "T2",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val v2Text = v2?.text
                                                if (v2Text != null && parallelShowDifferences && v1?.text != null) {
                                                    val annotated = DiffUtil.highlightDifferences(v1.text, v2Text)
                                                    Text(
                                                        text = annotated,
                                                        fontFamily = font,
                                                        fontSize = baseFontSize,
                                                        lineHeight = (baseFontSize.value * 1.45f).sp,
                                                        color = MaterialTheme.colorScheme.onBackground
                                                    )
                                                } else {
                                                    Text(
                                                        text = v2Text ?: if (isParallelLoading) "..." else "—",
                                                        fontFamily = font,
                                                        fontSize = baseFontSize,
                                                        lineHeight = (baseFontSize.value * 1.45f).sp,
                                                        color = MaterialTheme.colorScheme.onBackground
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Text(
                                                    text = translation?.name ?: (translation?.id?.uppercase() ?: "T1"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = v1Annotated,
                                                    fontFamily = font,
                                                    fontSize = baseFontSize,
                                                    lineHeight = (baseFontSize.value * 1.45f).sp,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                            }

                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Text(
                                                    text = parallelTranslation?.name ?: (parallelTranslation?.id?.uppercase() ?: "T2"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val v2Text = v2?.text
                                                if (v2Text != null && parallelShowDifferences && v1?.text != null) {
                                                    val annotated = DiffUtil.highlightDifferences(v1.text, v2Text)
                                                    Text(
                                                        text = annotated,
                                                        fontFamily = font,
                                                        fontSize = baseFontSize,
                                                        lineHeight = (baseFontSize.value * 1.45f).sp,
                                                        color = MaterialTheme.colorScheme.onBackground
                                                    )
                                                } else {
                                                    Text(
                                                        text = v2Text ?: if (isParallelLoading) "..." else "—",
                                                        fontFamily = font,
                                                        fontSize = baseFontSize,
                                                        lineHeight = (baseFontSize.value * 1.45f).sp,
                                                        color = MaterialTheme.colorScheme.onBackground
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (continuousText) {
                    // Continuous Text Mode with interactive verse tap & long-press/drag fragment selection
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
                    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
                    val versesMap = remember(verses) { verses.associateBy { it.number } }

                    val continuousString = remember(
                        verses,
                        hideVerseNumbers,
                        verseHighlights,
                        partialHighlights,
                        highlightedVerse,
                        activeSelection,
                        baseFontSize,
                        numberFontSize,
                        primaryColor,
                        primaryContainerColor,
                        onBackgroundColor
                    ) {
                        buildAnnotatedString {
                            for (v in verses) {
                                val blockStart = this.length
                                val persistentHex = verseHighlights[v.number]
                                val customHighlightColor = persistentHex?.let {
                                    runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
                                }
                                val isHighlighted = v.number == highlightedVerse

                                if (!hideVerseNumbers) {
                                    withStyle(
                                        SpanStyle(
                                            color = primaryColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = numberFontSize
                                        )
                                    ) {
                                        append("${v.number}")
                                        if (v.hasNote) {
                                            append("•")
                                        }
                                        append(" ")
                                    }
                                }

                                val textStart = this.length
                                val baseBg = when {
                                    isHighlighted -> primaryContainerColor.copy(alpha = 0.55f)
                                    customHighlightColor != null -> customHighlightColor.copy(alpha = 0.32f)
                                    v.hasNote -> HighlightNoteColor
                                    else -> Color.Transparent
                                }
                                withStyle(
                                    SpanStyle(
                                        color = onBackgroundColor,
                                        fontSize = baseFontSize,
                                        background = baseBg
                                    )
                                ) {
                                    append(v.text)
                                }
                                val textEnd = this.length

                                // Apply saved partial highlights for this verse
                                val versePartials = partialHighlights[v.number] ?: emptyList()
                                for (ph in versePartials) {
                                    val s = textStart + ph.startIndex.coerceIn(0, v.text.length)
                                    val e = textStart + ph.endIndex.coerceIn(0, v.text.length)
                                    if (s < e) {
                                        val phColor = runCatching { Color(android.graphics.Color.parseColor(ph.colorHex)) }.getOrNull()
                                        if (phColor != null) {
                                            addStyle(
                                                SpanStyle(background = phColor.copy(alpha = 0.48f)),
                                                start = s,
                                                end = e
                                            )
                                        }
                                    }
                                }

                                // Apply active fragment selection highlight if on this verse
                                val currentSel = activeSelection
                                if (currentSel != null && currentSel.verse.number == v.number) {
                                    val selStart = textStart + currentSel.startIndex.coerceIn(0, v.text.length)
                                    val selEnd = textStart + currentSel.endIndex.coerceIn(0, v.text.length)
                                    if (selStart < selEnd) {
                                        addStyle(
                                            SpanStyle(
                                                background = primaryColor.copy(alpha = 0.28f),
                                                fontWeight = FontWeight.SemiBold,
                                                textDecoration = TextDecoration.Underline
                                            ),
                                            start = selStart,
                                            end = selEnd
                                        )
                                    }
                                }

                                append(" ")
                                val blockEnd = this.length

                                addStringAnnotation(
                                    tag = "VERSE",
                                    annotation = v.number.toString(),
                                    start = blockStart,
                                    end = blockEnd
                                )
                                addStringAnnotation(
                                    tag = "VERSE_TEXT",
                                    annotation = "${v.number}:$textStart:$textEnd",
                                    start = blockStart,
                                    end = blockEnd
                                )
                            }
                        }
                    }

                    var continuousLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("continuous_verses_view")
                    ) {
                        item(key = "continuous_text_body") {
                            Text(
                                text = continuousString,
                                fontFamily = font,
                                lineHeight = (baseFontSize.value * 1.6f).sp,
                                onTextLayout = { continuousLayoutResult = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("continuous_text_content")
                                    .pointerInput(continuousString, versesMap) {
                                        detectTapGestures(
                                            onTap = { tapOffset ->
                                                val resolved = resolveContinuousTouch(
                                                    offset = tapOffset,
                                                    layoutResult = continuousLayoutResult,
                                                    annotated = continuousString,
                                                    versesMap = versesMap
                                                ) ?: return@detectTapGestures

                                                val (tappedVerse, relIndex) = resolved
                                                val currentSel = activeSelection
                                                if (currentSel != null) {
                                                    if (currentSel.verse.number == tappedVerse.number) {
                                                        val (wStart, wEnd) = findWordBoundsAt(tappedVerse.text, relIndex)
                                                        val newStart = minOf(currentSel.anchorStart, wStart)
                                                        val newEnd = maxOf(currentSel.anchorEnd, wEnd)
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        activeSelection = currentSel.copy(
                                                            startIndex = newStart,
                                                            endIndex = newEnd
                                                        )
                                                    } else {
                                                        activeSelection = null
                                                        onDismissFragmentHighlighter()
                                                        onVerseClick(tappedVerse)
                                                    }
                                                } else {
                                                    onVerseClick(tappedVerse)
                                                }
                                            }
                                        )
                                    }
                                    .pointerInput(continuousString, versesMap) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { startOffset ->
                                                val resolved = resolveContinuousTouch(
                                                    offset = startOffset,
                                                    layoutResult = continuousLayoutResult,
                                                    annotated = continuousString,
                                                    versesMap = versesMap
                                                ) ?: return@detectDragGesturesAfterLongPress

                                                val (pressedVerse, relIndex) = resolved
                                                val (wStart, wEnd) = findWordBoundsAt(pressedVerse.text, relIndex)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                activeSelection = FragmentSelectionState(
                                                    verse = pressedVerse,
                                                    anchorStart = wStart,
                                                    anchorEnd = wEnd,
                                                    startIndex = wStart,
                                                    endIndex = wEnd
                                                )
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val currentSel = activeSelection ?: return@detectDragGesturesAfterLongPress
                                                val resolved = resolveContinuousTouch(
                                                    offset = change.position,
                                                    layoutResult = continuousLayoutResult,
                                                    annotated = continuousString,
                                                    versesMap = versesMap
                                                ) ?: return@detectDragGesturesAfterLongPress

                                                val (dragVerse, relIndex) = resolved
                                                if (dragVerse.number == currentSel.verse.number) {
                                                    val (wStart, wEnd) = findWordBoundsAt(currentSel.verse.text, relIndex)
                                                    val newStart = minOf(currentSel.anchorStart, wStart)
                                                    val newEnd = maxOf(currentSel.anchorEnd, wEnd)
                                                    if (newStart != currentSel.startIndex || newEnd != currentSel.endIndex) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        activeSelection = currentSel.copy(
                                                            startIndex = newStart,
                                                            endIndex = newEnd
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                            )
                        }

                        item(key = "continuous_chapter_completion_footer") {
                            ChapterCompletionFooter(
                                isChapterCompleted = isChapterCompleted,
                                currentLanguage = currentLanguage,
                                onToggleChapterCompleted = onToggleChapterCompleted
                            )
                        }
                    }
                } else {
                    // Verse by Verse Mode
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("verses_list_view")
                    ) {
                        items(verses, key = { it.number }) { verse ->
                            val isHighlighted = verse.number == highlightedVerse
                            val persistentHex = verseHighlights[verse.number]
                            val customHighlightColor = persistentHex?.let {
                                runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
                            }
                            val versePartials = partialHighlights[verse.number] ?: emptyList()
                            val isSelectedForFragment = activeSelection?.verse?.number == verse.number

                            val verseAnnotated = buildVerseTextAnnotated(
                                verseText = verse.text,
                                partialHighlights = versePartials,
                                activeSelection = activeSelection,
                                isCurrentSelectedVerse = isSelectedForFragment,
                                selectionHighlightColor = MaterialTheme.colorScheme.primary
                            )

                            var verseLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    customHighlightColor != null -> customHighlightColor.copy(alpha = 0.28f)
                                    verse.hasNote -> HighlightNoteColor
                                    else -> Color.Transparent
                                },
                                border = if (customHighlightColor != null) {
                                    BorderStroke(1.dp, customHighlightColor.copy(alpha = 0.65f))
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("verse_item_${verse.number}")
                                    .clickable {
                                        val currentSel = activeSelection
                                        if (currentSel != null && currentSel.verse.number != verse.number) {
                                            activeSelection = null
                                            onDismissFragmentHighlighter()
                                        }
                                        onVerseClick(verse)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    if (!hideVerseNumbers) {
                                        Text(
                                            text = verse.number.toString(),
                                            fontFamily = font,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = numberFontSize,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .width(32.dp)
                                                .padding(top = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = verseAnnotated,
                                        fontFamily = font,
                                        fontSize = baseFontSize,
                                        lineHeight = (baseFontSize.value * 1.55f).sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        onTextLayout = { verseLayoutResult = it },
                                        modifier = Modifier
                                            .weight(1f)
                                            .pointerInput(verse.number, verse.text) {
                                                detectTapGestures(
                                                    onTap = { tapOffset ->
                                                        val currentSel = activeSelection
                                                        if (currentSel != null && currentSel.verse.number == verse.number) {
                                                            val lr = verseLayoutResult
                                                            if (lr != null) {
                                                                val relIndex = lr.getOffsetForPosition(tapOffset)
                                                                    .coerceIn(0, verse.text.length)
                                                                val (wStart, wEnd) = findWordBoundsAt(verse.text, relIndex)
                                                                val newStart = minOf(currentSel.anchorStart, wStart)
                                                                val newEnd = maxOf(currentSel.anchorEnd, wEnd)
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                activeSelection = currentSel.copy(
                                                                    startIndex = newStart,
                                                                    endIndex = newEnd
                                                                )
                                                            }
                                                        } else {
                                                            activeSelection = null
                                                            onDismissFragmentHighlighter()
                                                            onVerseClick(verse)
                                                        }
                                                    }
                                                )
                                            }
                                            .pointerInput(verse.number, verse.text) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { startOffset ->
                                                        val lr = verseLayoutResult
                                                        val relIndex = lr?.getOffsetForPosition(startOffset)
                                                            ?.coerceIn(0, verse.text.length) ?: 0
                                                        val (wStart, wEnd) = findWordBoundsAt(verse.text, relIndex)
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        activeSelection = FragmentSelectionState(
                                                            verse = verse,
                                                            anchorStart = wStart,
                                                            anchorEnd = wEnd,
                                                            startIndex = wStart,
                                                            endIndex = wEnd
                                                        )
                                                    },
                                                    onDrag = { change, _ ->
                                                        change.consume()
                                                        val currentSel = activeSelection ?: return@detectDragGesturesAfterLongPress
                                                        if (currentSel.verse.number == verse.number) {
                                                            val lr = verseLayoutResult ?: return@detectDragGesturesAfterLongPress
                                                            val relIndex = lr.getOffsetForPosition(change.position)
                                                                .coerceIn(0, verse.text.length)
                                                            val (wStart, wEnd) = findWordBoundsAt(verse.text, relIndex)
                                                            val newStart = minOf(currentSel.anchorStart, wStart)
                                                            val newEnd = maxOf(currentSel.anchorEnd, wEnd)
                                                            if (newStart != currentSel.startIndex || newEnd != currentSel.endIndex) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                activeSelection = currentSel.copy(
                                                                    startIndex = newStart,
                                                                    endIndex = newEnd
                                                                )
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    )

                                    if (verse.hasNote) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = "Has note",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(start = 4.dp, top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "chapter_completion_footer") {
                            ChapterCompletionFooter(
                                isChapterCompleted = isChapterCompleted,
                                currentLanguage = currentLanguage,
                                onToggleChapterCompleted = onToggleChapterCompleted
                            )
                        }
                    }
                }

                // Floating Fragment Highlighter Panel (shown on Tap & Hold or "Zakreśl fragment")
                AnimatedVisibility(
                    visible = activeSelection != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    activeSelection?.let { sel ->
                        FragmentHighlighterPanel(
                            currentLanguage = currentLanguage,
                            book = book,
                            chapter = chapter,
                            selection = sel,
                            existingPartialHighlights = partialHighlights[sel.verse.number] ?: emptyList(),
                            font = font,
                            onUpdateRange = { newStart, newEnd, newAnchorStart, newAnchorEnd ->
                                activeSelection = sel.copy(
                                    anchorStart = newAnchorStart,
                                    anchorEnd = newAnchorEnd,
                                    startIndex = newStart,
                                    endIndex = newEnd
                                )
                            },
                            onApplyColor = { hex, name ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSetPartialHighlight(
                                    sel.verse.number,
                                    sel.startIndex,
                                    sel.endIndex,
                                    hex,
                                    name
                                )
                                activeSelection = null
                                onDismissFragmentHighlighter()
                            },
                            onRemoveRangeHighlight = {
                                onRemovePartialHighlightRange(
                                    sel.verse.number,
                                    sel.startIndex,
                                    sel.endIndex
                                )
                                activeSelection = null
                                onDismissFragmentHighlighter()
                            },
                            onDeletePartialById = onDeletePartialHighlightById,
                            onClearAllForVerse = {
                                onClearVersePartialHighlights(sel.verse.number)
                            },
                            onCopyFragment = {
                                val s = sel.startIndex.coerceIn(0, sel.verse.text.length)
                                val e = sel.endIndex.coerceIn(0, sel.verse.text.length)
                                if (s < e) {
                                    val sub = sel.verse.text.substring(s, e)
                                    clipboardManager.setText(
                                        AnnotatedString("„$sub” (${book.sigla.uppercase()} $chapter:${sel.verse.number})")
                                    )
                                }
                                activeSelection = null
                                onDismissFragmentHighlighter()
                            },
                            onOpenVerseMenu = {
                                val v = sel.verse
                                activeSelection = null
                                onDismissFragmentHighlighter()
                                onVerseClick(v)
                            },
                            onDismiss = {
                                activeSelection = null
                                onDismissFragmentHighlighter()
                            }
                        )
                    }
                }

                // Bottom Navigation Bar (Chapter Flip)
                AnimatedVisibility(
                    visible = !zenMode && activeSelection == null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        tonalElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bottom_chapter_nav")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onPrevChapter,
                                modifier = Modifier.testTag("prev_chapter_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = Strings.get("prev_chapter", currentLanguage)
                                )
                            }

                            IconButton(
                                onClick = onOpenNotes,
                                modifier = Modifier.testTag("bottom_nav_notes_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = Strings.get("notes", currentLanguage),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .testTag("bottom_book_chapter_selector")
                                    .clickable { onOpenBookSelector() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = "${book.sigla} $chapter",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = Strings.get("select_book", currentLanguage),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onToggleParallelReading,
                                modifier = Modifier.testTag("bottom_nav_parallel_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerticalSplit,
                                    contentDescription = Strings.get("parallel_reading", currentLanguage),
                                    tint = if (isParallelReading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = onNextChapter,
                                modifier = Modifier.testTag("next_chapter_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = Strings.get("next_chapter", currentLanguage)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Swipe Feedback Glow under finger ---
        val currentTouch = touchPosition
        val primaryColor = MaterialTheme.colorScheme.primary
        val primaryContainer = MaterialTheme.colorScheme.primaryContainer

        if (currentTouch != null && abs(totalDrag) > 10f && activeSelection == null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dragIntensity = (abs(totalDrag) / 160f).coerceIn(0f, 1f)
                val glowRadius = 90.dp.toPx() * (0.85f + 0.35f * dragIntensity)
                val glowAlpha = (0.22f + 0.28f * dragIntensity)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = glowAlpha),
                            primaryContainer.copy(alpha = glowAlpha * 0.45f),
                            Color.Transparent
                        ),
                        center = currentTouch,
                        radius = glowRadius
                    ),
                    center = currentTouch,
                    radius = glowRadius
                )

                if (totalDrag < -30f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, primaryColor.copy(alpha = 0.18f * dragIntensity)),
                            startX = size.width - 70.dp.toPx(),
                            endX = size.width
                        ),
                        topLeft = Offset(size.width - 70.dp.toPx(), 0f),
                        size = Size(70.dp.toPx(), size.height)
                    )
                } else if (totalDrag > 30f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.18f * dragIntensity), Color.Transparent),
                            startX = 0f,
                            endX = 70.dp.toPx()
                        ),
                        topLeft = Offset.Zero,
                        size = Size(70.dp.toPx(), size.height)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterCompletionFooter(
    isChapterCompleted: Boolean,
    currentLanguage: String,
    onToggleChapterCompleted: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        OutlinedButton(
            onClick = onToggleChapterCompleted,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("toggle_chapter_completed_button")
        ) {
            Text(
                text = when {
                    isChapterCompleted && currentLanguage == "pl" -> "✓ Rozdział przeczytany"
                    isChapterCompleted && currentLanguage == "de" -> "✓ Kapitel gelesen"
                    isChapterCompleted -> "✓ Chapter completed"
                    currentLanguage == "pl" -> "Oznacz rozdział jako przeczytany"
                    currentLanguage == "de" -> "Kapitel als gelesen markieren"
                    else -> "Mark chapter as read"
                },
                fontWeight = if (isChapterCompleted) FontWeight.Bold else FontWeight.Medium,
                color = if (isChapterCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FragmentHighlighterPanel(
    currentLanguage: String,
    book: BookInfo,
    chapter: Int,
    selection: FragmentSelectionState,
    existingPartialHighlights: List<PartialHighlightEntity>,
    font: FontFamily,
    onUpdateRange: (startIndex: Int, endIndex: Int, anchorStart: Int, anchorEnd: Int) -> Unit,
    onApplyColor: (colorHex: String, colorName: String) -> Unit,
    onRemoveRangeHighlight: () -> Unit,
    onDeletePartialById: (Long) -> Unit,
    onClearAllForVerse: () -> Unit,
    onCopyFragment: () -> Unit,
    onOpenVerseMenu: () -> Unit,
    onDismiss: () -> Unit
) {
    val highlightColors = listOf(
        Triple("#FFF59D", "yellow", Color(0xFFFFF59D)),
        Triple("#A5D6A7", "green", Color(0xFFA5D6A7)),
        Triple("#90CAF9", "blue", Color(0xFF90CAF9)),
        Triple("#F48FB1", "pink", Color(0xFFF48FB1)),
        Triple("#FFE082", "orange", Color(0xFFFFE082)),
        Triple("#CE93D8", "purple", Color(0xFFCE93D8))
    )

    val verseText = selection.verse.text
    val tokens = remember(verseText) { extractWordTokens(verseText) }
    val safeStart = selection.startIndex.coerceIn(0, verseText.length)
    val safeEnd = selection.endIndex.coerceIn(0, verseText.length)
    val selectedSnippet = if (safeStart < safeEnd) verseText.substring(safeStart, safeEnd) else ""

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fragment_highlighter_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Header: Reference + Verse Menu + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${book.sigla} $chapter:${selection.verse.number}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (currentLanguage) {
                            "pl" -> "Dotknij słów, aby rozszerzyć zakres"
                            "de" -> "Wörter antippen zum Erweitern"
                            else -> "Tap words to adjust selection"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onOpenVerseMenu,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("fragment_open_verse_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (currentLanguage) {
                                "pl" -> "Menu wersetu"
                                "de" -> "Vers-Menü"
                                else -> "Verse menu"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("fragment_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close fragment highlighter",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Interactive Word Pills for precision fragment selection
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp)
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tokens.forEachIndexed { idx, token ->
                        val isWordSelected = token.startIndex < safeEnd && token.endIndex > safeStart
                        val existingPh = existingPartialHighlights.firstOrNull {
                            token.startIndex < it.endIndex && token.endIndex > it.startIndex
                        }
                        val existingColor = existingPh?.let {
                            runCatching { Color(android.graphics.Color.parseColor(it.colorHex)) }.getOrNull()
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                isWordSelected -> MaterialTheme.colorScheme.primaryContainer
                                existingColor != null -> existingColor.copy(alpha = 0.45f)
                                else -> MaterialTheme.colorScheme.surface
                            },
                            border = if (isWordSelected) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            },
                            modifier = Modifier
                                .testTag("fragment_word_$idx")
                                .clickable {
                                    // If tapping an already-selected boundary word when multiple words are selected, shrink or reset to that single word
                                    val isSingleWordSelected = safeStart == token.startIndex && safeEnd == token.endIndex
                                    if (isSingleWordSelected) {
                                        onUpdateRange(token.startIndex, token.endIndex, token.startIndex, token.endIndex)
                                    } else if (token.startIndex == safeStart && token.endIndex < safeEnd) {
                                        // Shrink from left to next word
                                        val nextWord = tokens.getOrNull(idx + 1)
                                        if (nextWord != null && nextWord.startIndex < safeEnd) {
                                            onUpdateRange(nextWord.startIndex, safeEnd, nextWord.startIndex, safeEnd)
                                        } else {
                                            onUpdateRange(token.startIndex, token.endIndex, token.startIndex, token.endIndex)
                                        }
                                    } else if (token.endIndex == safeEnd && token.startIndex > safeStart) {
                                        // Shrink from right to previous word
                                        val prevWord = tokens.getOrNull(idx - 1)
                                        if (prevWord != null && prevWord.endIndex > safeStart) {
                                            onUpdateRange(safeStart, prevWord.endIndex, safeStart, prevWord.endIndex)
                                        } else {
                                            onUpdateRange(token.startIndex, token.endIndex, token.startIndex, token.endIndex)
                                        }
                                    } else {
                                        // Expand selection to include this word
                                        val newStart = minOf(selection.anchorStart, token.startIndex)
                                        val newEnd = maxOf(selection.anchorEnd, token.endIndex)
                                        onUpdateRange(newStart, newEnd, selection.anchorStart, selection.anchorEnd)
                                    }
                                }
                        ) {
                            Text(
                                text = token.word,
                                fontFamily = font,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isWordSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isWordSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Selected snippet preview & quick range actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "„$selectedSnippet”",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            onUpdateRange(0, verseText.length, 0, verseText.length)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = when (currentLanguage) {
                                "pl" -> "Cały tekst"
                                "de" -> "Alles"
                                else -> "Select all"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Color Palette + Erase + Copy Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    highlightColors.forEach { (hex, name, color) ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = 1.5.dp,
                                    color = Color.Black.copy(alpha = 0.28f),
                                    shape = CircleShape
                                )
                                .testTag("fragment_color_$name")
                                .clickable { onApplyColor(hex, name) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRemoveRangeHighlight,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("fragment_remove_highlight_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = Strings.get("remove_highlight", currentLanguage),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onCopyFragment,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("fragment_copy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy fragment",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
