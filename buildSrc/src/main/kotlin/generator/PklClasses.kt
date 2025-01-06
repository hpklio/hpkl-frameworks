package generator

interface PklClassElement

data class PklClass(
    val open: Boolean = true,
    val name: String,
    val packageName: String,
    val properties: List<PklProperty>,
    val comment: String? = null
) : PklClassElement

data class PklModule  (
    val name: String,
    val description: String?,
    val clazz : PklClass,
    val imports: Set<String>?,
    val elements: Set<PklClassElement>?
) {
    fun merge(other: PklModule): PklModule {
        val nclazz = this.clazz.copy(properties = this.clazz.properties + other.clazz.properties)
        return this.copy(
            clazz = nclazz,
            imports = (imports ?: setOf()) + (other.imports ?: setOf()),
            elements = (elements ?: setOf()) + (other.elements ?: setOf()),
        )
    }
}

data class PklProperty(
    val name: String,
    val type: String,
    val defaultValue: String?,
    val comment: String?,
    val secret: Boolean = false,
)

data class PklTypeAlias(
    val name: String,
    val values: Collection<String>
) : PklClassElement

