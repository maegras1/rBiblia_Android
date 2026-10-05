package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.abs
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookInfo
import com.example.data.model.TextFontFamily
import com.example.data.model.TextSize
import com.example.data.model.Translation
import com.example.data.model.Verse
import com.example.ui.theme.HighlightNoteColor
import com.example.ui.theme.getFontFamily
import com.example.ui.util.DiffUtil
import com.example.ui.util.Strings

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
    onVerseClick: (Verse) -> Unit,
    onNextChapter: () -> Unit,
    onPrevChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val baseFontSize = 17.sp * textSize.scale
    val numberFontSize = 12.sp * textSize.scale
    val font = getFontFamily(fontFamily)

    // Scroll to highlighted verse when it changes
    LaunchedEffect(highlightedVerse, verses) {
        if (highlightedVerse != null && verses.isNotEmpty()) {
            val index = verses.indexOfFirst { it.number == highlightedVerse }
            if (index != -1) {
                listState.animateScrollToItem(index)
            }
        } else if (verses.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    var totalDrag by remember { mutableFloatStateOf(0f) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    val haptic = LocalHapticFeedback.current
    var hasHapticTriggered by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
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
                        // Parallel Reading Header with Quick Swap, Translation Pickers, Layout & Diff Controls
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

                    // Notice if parallel translation does not contain this book
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
                                    // Verse Number Header Row
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

                                    if (parallelLayoutColumns) {
                                        // Two Columns Layout (Side-by-side)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Left translation
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = translation?.id?.uppercase() ?: "T1",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = v1?.text ?: if (isLoading) "..." else "—",
                                                    fontFamily = font,
                                                    fontSize = baseFontSize,
                                                    lineHeight = (baseFontSize.value * 1.45f).sp,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                            }

                                            // Divider
                                            VerticalDivider(
                                                modifier = Modifier
                                                    .height(48.dp)
                                                    .align(Alignment.CenterVertically),
                                                color = MaterialTheme.colorScheme.outlineVariant
                                            )

                                            // Right translation
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
                                        // Stacked / Interlinear Layout (Full-width rows)
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Primary translation row
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Text(
                                                    text = translation?.name ?: (translation?.id?.uppercase() ?: "T1"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = v1?.text ?: if (isLoading) "..." else "—",
                                                    fontFamily = font,
                                                    fontSize = baseFontSize,
                                                    lineHeight = (baseFontSize.value * 1.45f).sp,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                            }

                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                            // Secondary translation row
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
                    // Continuous Text Mode
                    val continuousString = buildAnnotatedString {
                        for (v in verses) {
                            val persistentHex = verseHighlights[v.number]
                            val customHighlightColor = persistentHex?.let {
                                runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
                            }
                            if (!hideVerseNumbers) {
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = numberFontSize
                                    )
                                ) {
                                    append("${v.number} ")
                                }
                            }
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = baseFontSize,
                                    background = customHighlightColor?.copy(alpha = 0.32f) ?: Color.Transparent
                                )
                            ) {
                                append("${v.text} ")
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("continuous_verses_view")
                    ) {
                        item {
                            Text(
                                text = continuousString,
                                fontFamily = font,
                                lineHeight = (baseFontSize.value * 1.6f).sp,
                                modifier = Modifier.fillMaxWidth()
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
                                    .clickable { onVerseClick(verse) }
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
                                        text = verse.text,
                                        fontFamily = font,
                                        fontSize = baseFontSize,
                                        lineHeight = (baseFontSize.value * 1.55f).sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(1f)
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
                    }
                }

                // Bottom Navigation Bar (Chapter Flip)
                AnimatedVisibility(
                    visible = !zenMode,
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
                            // 1. Previous Chapter
                            IconButton(
                                onClick = onPrevChapter,
                                modifier = Modifier.testTag("prev_chapter_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = Strings.get("prev_chapter", currentLanguage)
                                )
                            }

                            // 2. Notes Button (Web mobile view)
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

                            // 3. Central Book & Chapter Selector
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

                            // 4. Parallel Reading / Comparison Button (Web mobile view)
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

                            // 5. Next Chapter
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

        if (currentTouch != null && abs(totalDrag) > 10f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dragIntensity = (abs(totalDrag) / 160f).coerceIn(0f, 1f)
                val glowRadius = 90.dp.toPx() * (0.85f + 0.35f * dragIntensity)
                val glowAlpha = (0.22f + 0.28f * dragIntensity)

                // Soft luminous radial glow under the finger
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

                // Directional edge highlight
                if (totalDrag < -30f) {
                    // Swiping left -> next chapter from right
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
                    // Swiping right -> prev chapter from left
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
