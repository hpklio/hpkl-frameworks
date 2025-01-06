package generator

import java.util.regex.Pattern

class PklGenerator(classLoader: ClassLoader, secretFields: Set<Pattern>) {
    val secretPredicates = secretFields.map { it.asPredicate() }

    data class TypeMapper(
        val type: String,
        val defaultValueMapper: (Any) -> String?
    )

    companion object {
        private val AnyType = TypeMapper("Any") { null }
        private val FloatType = TypeMapper("Float") { it.toString().toDouble().toString() }
        private val StringType = TypeMapper("String") {
                "\"${it.toString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")}\""
        }
        private val EmptyStringType = TypeMapper("String") { null }
        private val StringListType = TypeMapper("Listing<String>") { listDefaultValue(it) }

        val wellKnownTypes = mapOf(
            "java.lang.String" to StringType,
            "org.springframework.util.MimeType" to StringType,
            "org.springframework.core.io.Resource" to StringType,
            "org.springframework.http.MediaType" to StringType,
            "java.net.URI" to StringType,
            "java.sql.Driver" to StringType,
            "java.util.concurrent.ScheduledExecutorService" to StringType,
            "spring.jooq.sql-dialect" to StringType,
            "org.jooq.SQLDialect" to StringType,
            "java.lang.Short" to simpleType("Int16"),
            "java.lang.Integer" to simpleType("Int32"),
            "java.lang.Long" to simpleType("Int"),
            "long" to simpleType("Int"),
            "int" to simpleType("Int32"),
            "short" to simpleType("Int16"),
            "java.lang.Double" to FloatType,
            "double" to FloatType,
            "java.lang.Float" to FloatType,
            "float" to FloatType,
            "java.lang.Boolean" to simpleType("Boolean"),
            "org.springframework.util.unit.DataSize" to TypeMapper("DataSize") {
                it.toString().toPklDataSize()
            },
            "java.time.Duration" to TypeMapper("Duration") {
                it.toString().toPklDuration()
            },
            "java.lang.String[]" to StringListType,
            "java.util.List<java.lang.String>" to StringListType,
            "java.util.List<java.lang.Character>" to StringListType,
            "java.lang.Character[]" to StringListType,
            "java.util.Set<java.lang.String>" to StringListType,
            "java.util.Map<java.lang.String,java.lang.String>"
                    to TypeMapper("Mapping<String,String>") { null },
            "java.util.Properties" to TypeMapper("Mapping<String,String>") { null },
            "java.io.File" to EmptyStringType,
            "java.util.Locale" to EmptyStringType,
            "java.nio.charset.Charset" to EmptyStringType,
            "java.util.regex.Pattern"  to EmptyStringType,
            "java.lang.Class<?>"  to EmptyStringType,
            "java.net.InetAddress"  to EmptyStringType,
            "java.util.TimeZone" to EmptyStringType,
            "java.lang.Object" to AnyType
        )

        private fun simpleType(type: String) = TypeMapper(type) { it.toString() }

        private fun listDefaultValue(it: Any?) : String {
            val list = when (it) {
                is List<*> -> it
                else -> it.toString().split(",")
            }
            return "new Listing { ${list.joinToString(" ; ") { "\"${it.toString()}\"" }} }"
        }
    }

    val writtenClasses : MutableSet<String> = HashSet()

    fun generate(name: String, tree: List<MetadataGroupTree>) : PklModule {
        val mainClassName = name.split(".").last()
        val mainClassNameCap = mainClassName.replaceFirstChar { it.uppercase() }

        val group = tree.firstOrNull()?.group
        val moduleName = name.replace(mainClassName, mainClassNameCap)


        val allClasses = tree.flatMap { it.innerClasses }.toSet()

        val classes = tree.map {
            generateClass(it, moduleName, allClasses)?.let {
                listOf(it.first) + it.second
            }
        }.filterNotNull().flatMap { it }

        val clazz = classes.find {
            it is PklClass && it.name.equals(mainClassName, true)
        } as PklClass

        return PklModule(
            moduleName,
            group?.description,
            clazz,
            setOf(),
            classes.toSet().subtract(setOf(clazz))
        )
    }

    private val writtenTypes : MutableSet<String> = HashSet()
    private val writtenExternalTypes : MutableMap<String, String> = HashMap()
    private val javaClassWriter = JavaClassLoader(classLoader)

    private fun isCollection(type: String) : Boolean {
        val collections = listOf(
            "java.util.Collection",
            "java.util.List",
            "java.util.Set",
            "java.util.Map",
        )

        return collections.firstOrNull { type.startsWith(it) } != null
    }

    private fun generateClass(tree: MetadataGroupTree,
                              moduleName: String,
                            allClasses: Set<String>) : Pair<PklClassElement, List<PklClassElement>>? {

        if (!writtenClasses.add(tree.className)) {
            return null
        }

        if (tree.className == "Any") {
            return null
        }

        if (tree.enums != null) {
            return Pair(PklTypeAlias(
                tree.className,
                tree.enums
            ), listOf())
        }

        val isCollection = isCollection(tree.group.type)

        val result = mutableListOf<PklClassElement>()

        if (!isCollection) {
            tree.children.forEach {
                if (tree.properties?.firstOrNull { p -> it.key == p.name } == null) {
                    generateClass(it.value, moduleName, allClasses)?.let { cls ->
                        result.add(cls.first)
                        result.addAll(cls.second)
                    }
                }
            }
            tree.properties?.forEach {
                val propertyType = it.type
                if (!wellKnownTypes.containsKey(propertyType) && !isCollection(propertyType)) {
                    javaClassWriter.tryToLoadClass(propertyType)?.let { cl ->
                        generateClass(cl, moduleName, allClasses)?.let { cls ->
                            result.add(cls.first)
                            result.addAll(cls.second)
                        }
                    }
                }
            }
        }

        val process = isCollection
                || tree.group.type == "empty"
                || writtenTypes.add(tree.group.type)

        if (process) {
            val className = tree.group.name.split(".").last().toCamelCase()

//            val isModuleClass = className.equals(mainClassName, ignoreCase = true)


            val properties = tree.properties?.map {
                if (wellKnownTypes.containsKey(it.type)) {
                    val typeMapper = wellKnownTypes[it.type]!!
                    val defaultValue = it.defaultValue?.let { v -> typeMapper.defaultValueMapper(v) }
                    generateProperty(it, typeMapper.type, defaultValue)
                } else if (writtenExternalTypes.containsKey(it.type)) {
                    val writtenType = writtenExternalTypes[it.type]!!
                    val defaultValue = it.defaultValue?.let { dv -> "\"${dv.toString().replace("-", "_").uppercase()}\"" }
                    generateProperty(it, writtenType, defaultValue)
                } else if (isCollection(it.type)) {
                    generateCollection(result, it, moduleName, allClasses)
                } else if (!it.type.startsWith("java.")) {
                    val innerClassName = javaClassWriter.tryToLoadClass(it.type)?.let { cl ->
                            generateClass(cl, moduleName, allClasses)
                            cl.className
                    } ?: "Any"
                    generateProperty(it, innerClassName, null)
                } else {
                    generateProperty(it, "Any", null)
                }
            } ?: listOf()

            val children = if (!isCollection) {
                tree.children.map {
                    if (tree.properties?.firstOrNull { p -> it.key == p.name } == null) {
                        generateChild(it.key, it.value, allClasses)
                    } else {
                        null
                    }
                }.filterNotNull()
            } else listOf()

            val name = capitalize(className)
            val allProperties = (properties + children).map { p ->
                val fullPath = if (!moduleName.endsWith(name)) {
                    "$moduleName.$name.${p.name}"
                } else {"$moduleName.${p.name}"}
                if (secretPredicates.any { it.test(p.name.lowercase()) || it.test(fullPath.lowercase()) }) {
                    p.copy(secret = true)
                } else {
                    p
                }
            }

            return Pair(PklClass(
                open = !tree.final,
                name = name,
                properties = allProperties,
                comment = tree.group.description,
                packageName = moduleName
            ), result)

        } else {
            return null
        }
    }

    private fun generateCollection(
        classes: MutableList<PklClassElement>,
        property: MetadataProperty,
        mainClassName: String,
        allClasses: Set<String>
    ) : PklProperty {
        return if (property.type.startsWith("java.util.List")) {
            val genericClass = property.type
                .removePrefix("java.util.List")
                .removePrefix("<")
                .removeSuffix(">")
            generateList(classes,"Listing", genericClass, property, mainClassName, allClasses) {
                list -> "new Listing { ${list.joinToString(" ; ")} }"
            }
        } else if (property.type.startsWith("java.util.Map")) {
            val genericClasses = property.type
                .removePrefix("java.util.Map")
                .removePrefix("<")
                .removeSuffix(">")
                .split(",")
            generateMapProperty(classes, genericClasses[0], genericClasses[1], property, mainClassName, allClasses)
        } else if (property.type.startsWith("java.util.Set")) {
            val genericClass = property.type.removePrefix("java.util.Set")
                .removePrefix("<")
                .removeSuffix(">")
            generateList(classes,"Set", genericClass, property, mainClassName, allClasses)
                { list  -> "Set(${list.joinToString(" , ")})" }
        } else PklProperty(
            property.name.toCamelCase(), "Any", null, property.description
        )
    }

    private fun generateList(
        classes: MutableList<PklClassElement>,
        collectionClass: String,
        genericClass: String,
        property: MetadataProperty,
        mainClassName: String,
        allClasses: Set<String>,
        defaultValueMapper: (List<String>) -> String?
    ) : PklProperty {
        val typeMapper = wellKnownTypes[genericClass] ?:
            javaClassWriter.tryToLoadClass(genericClass)?.let {
                generateClass(it, mainClassName, allClasses)?.let { cls ->
                    classes.add(cls.first)
                    classes.addAll(cls.second)
                }
                TypeMapper(it.className) { null }
            } ?: AnyType

        val type = "$collectionClass<${typeMapper.type}>"
        val name = property.name.toCamelCase()

        val defaultValue = if (property.defaultValue != null) {
            val list = when (property.defaultValue) {
                is List<*> -> property.defaultValue.map { typeMapper.defaultValueMapper.invoke(it!!) }
                else -> listOf(property.defaultValue.let { typeMapper.defaultValueMapper.invoke(it) })
            }.filterNotNull()
            defaultValueMapper.invoke(list)
        } else {
            null
        }

        return PklProperty(
            name, type, defaultValue, property.description
        )
    }

    private fun generateMapProperty(
        classes: MutableList<PklClassElement>,
        keyClass: String,
        valueClass: String,
        property: MetadataProperty,
        mainClassName: String,
        allClasses: Set<String>
    ) : PklProperty {

        val keyTypeMapper = wellKnownTypes[keyClass] ?: javaClassWriter.tryToLoadClass(keyClass)?.let {
            generateClass(it, mainClassName, allClasses)?.let { cls ->
                classes.add(cls.first)
                classes.addAll(cls.second)
            }
            TypeMapper(it.className) { null }
        } ?: AnyType

        val valueTypeMapper = wellKnownTypes[valueClass] ?: javaClassWriter.tryToLoadClass(valueClass)?.let {
            generateClass(it, mainClassName, allClasses)?.let { cls ->
                classes.add(cls.first)
                classes.addAll(cls.second)
            }
            TypeMapper(it.className) { null }
        } ?: AnyType

        val type = "Mapping<${keyTypeMapper.type},${valueTypeMapper.type}>"

        // TODO: process default values
        return PklProperty(
            property.name.toCamelCase(),
            type,
            null,
            property.description
        )
    }

    private fun appendFieldDescription(cw: StringBuilder, description: String?) {
        if (description != null) {
            cw.append("  /// ${description.replace("\n", "\n  ///")}\n")
        }
    }

    private fun generateProperty(
        property: MetadataProperty,
        type: String,
        defaultValue: String? = null
    ) : PklProperty {

        return PklProperty(
            property.name.toCamelCase(),
            type,
            defaultValue,
            property.description
        )
    }

    private fun generateChild(
        name: String,
        property: MetadataGroupTree,
        allClasses: Set<String>
    ) : PklProperty {
        val className = if (allClasses.contains(property.className)) {
            property.className
        } else "Any"

        return PklProperty(
            name.toCamelCase(),
            className,
            null,
            null
        )
    }


    private fun capitalize(str: String): String =
        str.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

fun String.toPklDuration(): String? {
    val pattern = "([0-9]+)([a-z]+)".toRegex()
    return pattern.find(this)?.let {
        val unit = when (val parsedUnit = it.groups[2]?.value) {
            "S", "H", "D" -> parsedUnit.lowercase()
            "s", "h", "d", "ns", "ms", "us" -> parsedUnit.lowercase()
            "m", "M" -> "min"
            else -> "ms"
        }
        "${it.groups[1]?.value}.${unit}"
    }
}

fun String.toPklDataSize(): String? {
    val pattern = "([0-9]+)([a-z]+)".toRegex()
    return pattern.find(this)?.let {
        "${it.groups[1]?.value}.${it.groups[2]?.value?.lowercase()}"
    }
}

fun String.toCamelCase(): String {
    val pattern = "[_-][a-zA-Z]".toRegex()
    return replace(pattern) { it.value.last().uppercase() }
}