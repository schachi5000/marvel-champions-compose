package net.schacher.mcc.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.schacher.mcc.shared.design.OptionsEntry
import net.schacher.mcc.shared.design.OptionsGroup
import net.schacher.mcc.shared.screens.search.SearchBar

@Preview
@Composable
fun SearchBarPreview() {
    SearchBar(onDoneClick = {}, onQueryChange = {})
}

@Preview
@Composable
fun OptionsGroupPreview() {
    OptionsGroup("Title") {
        OptionsEntry(label = "Eintrag", icon = { }) {
        }
    }
}