package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookInfo
import com.example.data.model.Translation
import com.example.ui.util.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleTopAppBar(
    currentLanguage: String = "pl",
    selectedBook: BookInfo,
    selectedChapter: Int,
    selectedTranslation: Translation?,
    isParallelReading: Boolean = false,
    parallelTranslation: Translation? = null,
    onOpenBookSelector: () -> Unit = {},
    onOpenChapterSelector: () -> Unit = {},
    onOpenTranslationSelector: () -> Unit = {},
    onOpenParallelTranslationSelector: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onToggleParallelReading: () -> Unit = {},
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier.testTag("bible_top_app_bar"),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isParallelReading) {
                    // Primary Translation Pill with FULL NAME
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("translation_selector_button")
                            .clickable { onOpenTranslationSelector() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = selectedTranslation?.name ?: "Uwspółcześniona Biblia Gdańska",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Translation",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // Primary Translation Pill (Parallel mode)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("translation_selector_button")
                            .clickable { onOpenTranslationSelector() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = selectedTranslation?.name ?: "Uwspółcześniona Biblia Gdańska",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Translation",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "‖",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Secondary Translation Pill (Parallel mode)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("parallel_translation_selector_button")
                            .clickable { onOpenParallelTranslationSelector() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = parallelTranslation?.name ?: "Biblia Tysiąclecia",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Second Translation",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        actions = {
            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier.testTag("top_search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = Strings.get("search", currentLanguage),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier.testTag("side_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu"
                )
            }
        }
    )
}
