package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SearchResult
import com.example.data.model.SearchScope
import com.example.ui.util.Strings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchDialog(
    currentLanguage: String,
    searchQuery: String,
    searchScope: SearchScope,
    searchResults: List<SearchResult>,
    recentSearches: List<String>,
    isSearching: Boolean,
    onQueryChanged: (String) -> Unit,
    onScopeChanged: (SearchScope) -> Unit,
    onExecuteSearch: () -> Unit,
    onClearHistory: () -> Unit,
    onResultSelected: (SearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.get("search", currentLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChanged,
                    placeholder = { Text(Strings.get("search_hint", currentLanguage)) },
                    leadingIcon = {
                        IconButton(onClick = onExecuteSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onExecuteSearch() }),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Scope Filters
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = searchScope == SearchScope.ALL,
                        onClick = { onScopeChanged(SearchScope.ALL) },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = searchScope == SearchScope.OT,
                        onClick = { onScopeChanged(SearchScope.OT) },
                        label = { Text(Strings.get("old_testament", currentLanguage).take(6)) }
                    )
                    FilterChip(
                        selected = searchScope == SearchScope.NT,
                        onClick = { onScopeChanged(SearchScope.NT) },
                        label = { Text(Strings.get("new_testament", currentLanguage).take(6)) }
                    )
                }

                // Recent Searches
                if (searchQuery.isEmpty() && recentSearches.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Text(
                                text = Strings.get("recent_searches", currentLanguage),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        TextButton(onClick = onClearHistory) {
                            Text(Strings.get("clear_history", currentLanguage))
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        recentSearches.forEach { q ->
                            AssistChip(
                                onClick = {
                                    onQueryChanged(q)
                                    onExecuteSearch()
                                },
                                label = { Text(q) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Results State
                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (searchResults.isNotEmpty()) {
                    Text(
                        text = String.format(Strings.get("results_found", currentLanguage), searchResults.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(searchResults) { result ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_result_${result.book}_${result.chapter}_${result.verse}")
                                    .clickable { onResultSelected(result) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "${result.bookName} ${result.chapter}:${result.verse}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = result.content,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                } else if (searchQuery.length >= 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Strings.get("no_results", currentLanguage),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
