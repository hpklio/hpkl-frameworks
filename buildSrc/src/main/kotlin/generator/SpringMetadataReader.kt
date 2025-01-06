package generator

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import spec.GeneratorSpec
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.URLClassLoader
import java.util.regex.Pattern


class SpringMetadataReader(private val spec: GeneratorSpec, secretFields: Set<Pattern>, private val files: List<File>) {
    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val allSecretFields = spec.secretFields + secretFields

    fun generate() : List<Pair<File,PklModule>> {

        val jars = files.filter { it.name.endsWith(".jar") }
        val classLoader = URLClassLoader( jars.map {
            URL("jar:file://${it.absolutePath}!/")
        }.toTypedArray(), this.javaClass.classLoader )

        return jars.map {
                URL("jar:file://${it.absolutePath}!/META-INF/spring-configuration-metadata.json")
        }.mapNotNull {
            try {
                it.openStream().use<InputStream?, JsonNode>(objectMapper::readTree)
            } catch (e: IOException) {
                null
            }
        }.flatMap { p ->  processMetadata(p, classLoader, allSecretFields) }.groupBy(
            { it.first }, {it.second}
        ).map {
            val module = it.value.reduce { acc, m -> acc.merge(m) }
            Pair(it.key, module)
        }
    }

    private fun processMetadata(tree: JsonNode,
                                classLoader: ClassLoader,
                                secretFields: Set<Pattern>) : List<Pair<File,PklModule>> {
        val groups = tree.get("groups")
        val properties = tree.get("properties")

        val propertiesMap = collectProperties(properties)

        return groups.asSequence().mapNotNull {
            try {
                objectMapper.treeToValue(it, MetadataGroup::class.java)
            } catch (e: JsonProcessingException) {
                System.err.println("Error parsing, ${e.message}")
                null
            }
        }.sortedBy { it.name }.map { g -> Pair(fileName(g), g) }.groupBy(
            { it.first }, { it.second }
        ).map {
            Pair(it.key, toTree(it.value, propertiesMap))
        }.toList().map {
            val pklFileGenerator = PklGenerator(classLoader, secretFields)
            val group = it.second.minBy { g -> g.group.name }.group
            val file = spec.outputDir.file(it.first).get().asFile
            Pair(file, pklFileGenerator.generate(group.name, it.second))
        }.groupBy({ it.first }, { it.second }).map {
            val module = it.value.reduce { acc, m -> acc.merge(m) }
            Pair(it.key, module)
        }
    }

    private fun fileName(group: MetadataGroup): String {
        val split = group.name.split(".")
        val name = if (!split.first().equals("spring", true)) {
            split.first()
        } else if (split.size > 1) {
            split[1]
        } else {
            split.last()
        }

        return "${name.replaceFirstChar { it.uppercase() }}.pkl"
    }

    private fun toTree(groups: List<MetadataGroup>, properties: Map<String, List<MetadataProperty>>): List<MetadataGroupTree> {
        val map : MutableMap<String, MetadataGroupTree> = HashMap()
        for (group in groups.sortedBy { it.name }) {
            val split = group.name.split(".")
            val last = split.last()
            var source  = map
            var currentPath = ""
            for (part in split) {
                if (currentPath.isNotEmpty()) currentPath += "."
                currentPath += part
                if (part == "spring") continue
                val tree = source.getOrPut(part) {
                    if (part == last) {
                        MetadataGroupTree(
                            group,
                            properties[group.type]
                        )
                    } else {
                        MetadataGroupTree(
                            MetadataGroup(currentPath, "empty", "empty", null),
                            listOf(),
                        )
                    }
                }
                source = tree.children
            }
        }
        return map.values.toList()
    }

    private fun collectProperties(properties: JsonNode) : Map<String, List<MetadataProperty>> {
        val javaType = objectMapper.typeFactory.constructCollectionType(
            List::class.java, MetadataPropertyParse::class.java
        )
        val treeToValue = objectMapper.treeToValue<List<MetadataPropertyParse>>(properties, javaType)
        return treeToValue.filter {
            it.name != null && it.type != null && it.sourceType != null
        }.map {
            val propertyName = it.name!!.split(".").last()
            MetadataProperty(propertyName, it.type!!, it.description, it.defaultValue, it.sourceType!!)
        }.groupBy { it.sourceType }.map { p ->
            Pair(p.key, p.value.distinctBy { v -> v.name })
        }.toMap()
    }


}