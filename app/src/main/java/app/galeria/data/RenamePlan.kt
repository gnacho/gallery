package app.galeria.data

object RenamePlan {
    fun fileName(originalName: String, requestedName: String): String {
        val requested = requestedName.trim()
        require(requested.isNotEmpty()) { "The name cannot be blank" }
        require(!requested.contains('/') && !requested.contains('\\')) { "The name cannot contain a path" }
        val originalDot = originalName.lastIndexOf('.')
        val extension = if (originalDot > 0) originalName.substring(originalDot) else ""
        val requestedDot = requested.lastIndexOf('.')
        val base = if (extension.isNotEmpty() && requestedDot > 0) requested.substring(0, requestedDot) else requested
        require(base.isNotBlank()) { "The name cannot be blank" }
        return base + extension
    }
}
