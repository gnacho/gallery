package app.galeria.data

data class SearchHistory(val entries: List<String> = emptyList()) {
    fun record(query: String): SearchHistory {
        val clean = query.trim()
        if (clean.isEmpty()) return this
        return SearchHistory((listOf(clean) + entries.filterNot { it.equals(clean, ignoreCase = true) }).take(8))
    }
}
