package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.util.Strings

@Composable
fun SideMenuDrawer(
    currentLanguage: String,
    onSelectTranslations: () -> Unit,
    onSelectNotes: () -> Unit,
    onSelectSearch: () -> Unit,
    onSelectChapterComparison: () -> Unit,
    onSelectSettings: () -> Unit,
    onSelectAbout: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .testTag("side_menu_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // App Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "rBiblia",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Bible Study & Reader",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            SideMenuItem(
                icon = Icons.Default.Book,
                title = Strings.get("translations", currentLanguage),
                testTag = "drawer_item_translations",
                onClick = {
                    onCloseDrawer()
                    onSelectTranslations()
                }
            )

            SideMenuItem(
                icon = Icons.Default.EditNote,
                title = Strings.get("notes", currentLanguage),
                testTag = "drawer_item_notes",
                onClick = {
                    onCloseDrawer()
                    onSelectNotes()
                }
            )

            SideMenuItem(
                icon = Icons.Default.Search,
                title = Strings.get("search", currentLanguage),
                testTag = "drawer_item_search",
                onClick = {
                    onCloseDrawer()
                    onSelectSearch()
                }
            )

            SideMenuItem(
                icon = Icons.Default.Compare,
                title = Strings.get("chapter_comparison", currentLanguage),
                testTag = "drawer_item_comparison",
                onClick = {
                    onCloseDrawer()
                    onSelectChapterComparison()
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            SideMenuItem(
                icon = Icons.Default.Settings,
                title = Strings.get("settings", currentLanguage),
                testTag = "drawer_item_settings",
                onClick = {
                    onCloseDrawer()
                    onSelectSettings()
                }
            )

            SideMenuItem(
                icon = Icons.Default.Info,
                title = Strings.get("about", currentLanguage),
                testTag = "drawer_item_about",
                onClick = {
                    onCloseDrawer()
                    onSelectAbout()
                }
            )
        }
    }
}

@Composable
private fun SideMenuItem(
    icon: ImageVector,
    title: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
