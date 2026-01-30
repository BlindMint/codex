package us.blindmint.codex.presentation.settings

data class SearchableSettingsItem(
    val id: String,
    val title: String,
    val category: String,
    val subcategory: String? = null,
    val type: SearchableSettingsType,
    val onClick: () -> Unit
)

enum class SearchableSettingsType {
    CATEGORY,
    SUBCATEGORY,
    SUBSETTING
}
