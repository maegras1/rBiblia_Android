package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.util.Strings
import kotlin.math.abs

@Composable
fun ReaderScreen(
    currentLanguage: String,
    book: BookInfo,
    chapter: Int,
    translation: Translation?,
    verses: List<Verse>,
    isLoading: Boolean,
    textSize: TextSize,
    fontFamily: TextFontFamily,
    continuousText: Boolean,
    hideVerseNumbers: Boolean,
    zenMode: Boolean,
    highlightedVerse: Int?,
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                    onDragEnd = {
                        if (totalDrag < -100f) {
                            onNextChapter() // swipe left -> next chapter
                        } else if (totalDrag > 100f) {
                            onPrevChapter() // swipe right -> prev chapter
                        }
                        totalDrag = 0f
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
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${book.name} $chapter",
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (translation != null) {
                        Text(
                            text = translation.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Verses View: Continuous Text or Verse-by-Verse
                if (continuousText) {
                    // Continuous Text Mode
                    val continuousString = buildAnnotatedString {
                        for (v in verses) {
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
                                    fontSize = baseFontSize
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

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    verse.hasNote -> HighlightNoteColor
                                    else -> Color.Transparent
                                },
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
                                .padding(horizontal = 16.dp, vertical = 6.dp),
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

                            Text(
                                text = "${book.sigla} $chapter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

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
    }
}
