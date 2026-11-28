package com.cmc.launcher.ui.screens.content.download.addon.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmc.launcher.R
import com.cmc.launcher.game.download.addon.platform.Platform
import com.cmc.launcher.game.download.addon.platform.PlatformClasses
import com.cmc.launcher.game.download.addon.platform.isAllNull
import com.cmc.launcher.game.download.addon.utils.getMcmodTitle
import com.cmc.launcher.ui.AndroidStringText
import com.cmc.launcher.ui.components.OwnOutlinedTextField
import com.cmc.launcher.ui.screens.NestedNavKey
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.TitledNavKey
import com.cmc.launcher.ui.screens.content.download.addon.elements.ProjectUrlsContent
import com.cmc.launcher.ui.screens.content.download.addon.elements.ResultProjectLayout
import com.cmc.launcher.ui.screens.content.download.addon.elements.ScreenshotItemLayout
import com.cmc.launcher.ui.screens.main.control_editor.EditorChoices
import com.cmc.launcher.ui.theme.Space

@Composable
fun SearchIdScreen(
    mainScreenKey: TitledNavKey?, downloadScreenKey: TitledNavKey?, openLink: (String) -> Unit,
    swapToDownload: (Platform, PlatformClasses, projectId: String, iconUrl: String?) -> Unit
) {
    val model: SearchIdViewModel = viewModel { SearchIdViewModel() }
    if (mainScreenKey !is NestedNavKey.Download || downloadScreenKey != NormalNavKey.SearchId) return
    val input by model.projectId.collectAsStateWithLifecycle()
    val platform by model.platform.collectAsStateWithLifecycle()
    val operation by model.operation.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val submit = { focus.clearFocus(); model.search() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
            OwnOutlinedTextField(input, model::editId, Modifier.weight(1f), singleLine = true,
                label = { Text(stringResource(R.string.download_addon_id_holder)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { submit() }),
                trailingIcon = { if (input.isNotEmpty()) IconButton(onClick = { model.editId("") }) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.generic_clear))
                } })
            IconButton(enabled = input.isNotBlank() && operation != SearchIdOperation.Loading, onClick = submit) {
                Icon(painterResource(R.drawable.ic_search), stringResource(R.string.generic_search))
            }
        }
        EditorChoices(Platform.entries.map { it.displayName }, Platform.entries.indexOf(platform),
            { model.selectPlatform(Platform.entries[it]) }, Modifier.padding(horizontal = Space.md, vertical = Space.sm))
        HorizontalDivider()
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when (val state = operation) {
                SearchIdOperation.None -> Text(stringResource(R.string.download_addon_id_tip), Modifier.padding(Space.lg))
                SearchIdOperation.Loading -> Text(stringResource(R.string.generic_loading))
                SearchIdOperation.NotFound -> Text(stringResource(R.string.download_addon_id_not_found), Modifier.padding(Space.lg))
                SearchIdOperation.Unsupported -> Text(stringResource(R.string.download_addon_id_unsupported), Modifier.padding(Space.lg))
                is SearchIdOperation.Error -> Column(Modifier.padding(Space.lg), horizontalAlignment = Alignment.CenterHorizontally) {
                    AndroidStringText(state.message)
                    TextButton(onClick = submit) { Text(stringResource(R.string.generic_retry)) }
                }
                is SearchIdOperation.Result -> key(state.project) { SearchIdResult(state, openLink, swapToDownload) }
            }
        }
    }
}

@Composable
private fun SearchIdResult(state: SearchIdOperation.Result, open: (String) -> Unit,
    view: (Platform, PlatformClasses, String, String?) -> Unit) {
    val context = LocalContext.current
    val project = state.project
    val classes = state.classes
    val urls = project.platformUrls(classes)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.md), verticalArrangement = Arrangement.spacedBy(Space.md)) {
        item {
            ResultProjectLayout(platform = project.platform(), classes = classes,
                title = state.mcMod.getMcmodTitle(project.platformTitle(), context), description = project.platformSummary(),
                iconUrl = project.platformIconUrl(), authors = project.platformAuthors(), downloads = project.platformDownloadCount(),
                followsCount = project.platformFollows(), modloaders = project.platformModLoaders(), categories = project.platformCategories(classes)?.sortedBy { it.index() },
                onClick = { view(project.platform(), classes, project.platformId(), project.platformIconUrl()) })
        }
        if (!urls.isAllNull()) item {
            Text(stringResource(R.string.download_addon_links), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                ProjectUrlsContent(project.platform(), urls, state.mcMod, open)
            }
        }
        items(project.platformScreenshots()) { screenshot ->
            ScreenshotItemLayout(screenshot, Modifier.fillMaxWidth())
        }
    }
}