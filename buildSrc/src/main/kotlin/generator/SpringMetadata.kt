package generator

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class MetadataGroup(
    val name: String,
    val type: String,
    val sourceType: String? = null,
    val sourceMethod: String? = null,
    val description: String? = null
)

data class MetadataGroupTree(
    val group: MetadataGroup,
    val properties: List<MetadataProperty>?,
    val children: MutableMap<String, MetadataGroupTree> = mutableMapOf(),
    val enums: Set<String>? = null,
    val final : Boolean = false
) {
    val className: String = if (group.type != "java.lang.Object") {
        capitalize(group.name.split(".").last()).toCamelCase()
    } else {
        "Any"
    }

    val classes : List<String>
        get() = listOf(className)  + children.flatMap { it.value.classes }
    val innerClasses : List<String>
        get() = children.flatMap { it.value.classes }

}

data class MetadataProperty(
    val name: String,
    val type: String,
    val description: String?,
    val defaultValue: Any?,
    val sourceType: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MetadataPropertyParse(
    val name: String?,
    val type: String?,
    val description: String?,
    val defaultValue: Any?,
    val sourceType: String?
)

private fun capitalize(str: String): String =
    str.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

