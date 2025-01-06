package generator

import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

class JavaClassLoader(
    private val classLoader: ClassLoader
) {
    private val counter = AtomicInteger()
    val writtenExternalTypes : MutableMap<String, MetadataGroupTree> = HashMap()
    val writtenExternalClasses : MutableMap<Class<*>, MetadataGroupTree> = HashMap()

    fun tryToLoadClass(propertyType: String) : MetadataGroupTree? {
        try {
            val clazz = classLoader.loadClass(propertyType)

            return if (clazz.isEnum) {
                parseEnum(propertyType, clazz)
            } else {
                parseClass(propertyType, clazz)
            }?.let {
                writtenExternalTypes[propertyType] = it
                it
            }
        } catch (e: Throwable) {
            println("Class not found: ${propertyType}")
        }
        return null
    }

    fun parseClass(propertyType: String, clazz: Class<*>) : MetadataGroupTree {
        return writtenExternalClasses.computeIfAbsent(
            clazz
        ) {
            val className = propertyType.split(".").last()
                .replace("$", "").let {
                    if (!writtenExternalTypes.contains(it)) {
                        it
                    } else {
                        it + counter.incrementAndGet()
                    }
                }

            val properties = loadClassProperties(clazz)


            MetadataGroupTree(
                group = MetadataGroup(
                    className,
                    className,
                ),
                properties.values.toList(),
                final = Modifier.isFinal(clazz.modifiers)
            )
        }
    }

    private fun loadClassProperties(clazz: Class<*>): Map<String, MetadataProperty> {
        val result = HashMap<String, MetadataProperty>()
        clazz.declaredFields.forEach {
            if (!Modifier.isStatic(it.modifiers)
                && !Modifier.isFinal(it.modifiers)
                && Modifier.isPublic(it.modifiers)) {
                result[it.name] = MetadataProperty(
                    it.name,
                    it.type.canonicalName,
                    null,
                    null,
                    "java"
                )
            }
        }

        val methods = clazz.declaredMethods.map {
            it.name.lowercase() to it
        }.toMap()

        clazz.declaredMethods.forEach {

            if (!Modifier.isStatic(it.modifiers)
                && Modifier.isPublic(it.modifiers)
                && it.name.startsWith("get")
                ) {
                val name = it.name.removePrefix("get")
                if (methods.contains("set"+name.lowercase())) {
                    result[name.replaceFirstChar { it.lowercase() }] = MetadataProperty(
                        name.replaceFirstChar { it.lowercase() },
                        it.returnType.canonicalName,
                        null,
                        null,
                        "java"
                    )
                }
            }
        }
        if (clazz.superclass != null) {
            result.putAll(loadClassProperties(clazz.superclass))
        }

        return result
    }

    fun parseEnum(propertyType: String, clazz: Class<*>) : MetadataGroupTree? {
        val enumName = propertyType.split(".").last()
            .split("$").last().toCamelCase()
        try {
            return MetadataGroupTree(
                group = MetadataGroup(
                    enumName,
                    propertyType,
                ),
                null,
                mutableMapOf(),
                clazz.enumConstants.map { it.toString() }.toSet()
            )

        } catch (e: Throwable) {
            return null
        }
    }
}