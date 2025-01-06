import generator.PklClass
import generator.PklClassElement
import generator.PklModule
import generator.PklTypeAlias
import generator.PklProperty
import org.pkl.core.parser.Lexer
import java.io.File
import java.io.Writer

class PklModuleWriter {


    fun write(file: File, module: PklModule) {
        file.bufferedWriter(Charsets.UTF_8).use {
            module.description?.let { description -> it.appendLine("/// $description") }
            it.appendLine("open module ${module.name}")
            it.appendLine()

            if (hasSecretFields(module)) {
                writeImport(it, "@hpkl-k8s-app/renderer.pkl")
            }

            module.imports?.forEach { import -> writeImport(it, import) }
            it.appendLine()

            module.elements?.filter { it != module.clazz }?.forEach {
                element -> writeClassElement(it, element)
            }
            it.appendLine()

            writeProperties(it, module.clazz.properties, "")
        }
    }

    private fun hasSecretFields(module: PklModule) : Boolean {
        return module.clazz.properties.any { it.secret } ||
                module.elements?.any { it is PklClass && it.properties.any { p -> p.secret} } ?: false
    }

    private fun writeImport(writer: Writer, import: String) {
        writer.appendLine("import \"$import\"")
    }

    fun writeClassElement(writer: Writer, element: PklClassElement) {
        when (element) {
            is PklClass -> writePklClass(writer, element)
            is PklTypeAlias -> writeTypeAlias(writer, element)
        }
    }

    private fun writeTypeAlias(writer: Writer, typeAlias: PklTypeAlias) {
        writer.append("typealias ${Lexer.maybeQuoteIdentifier(typeAlias.name)} = ")
        writer.append(typeAlias.values.joinToString(" | ") { "\"${it}\"" })
        writer.append("\n\n")
    }

    private fun writePklClass(writer: Writer, clazz: PklClass) {
        clazz.comment?.let {
            writer.appendLine("/// ${it}")
        }

        if (clazz.open) {
            writer.append("open ")
        }

        val name = Lexer.maybeQuoteIdentifier(clazz.name)
        writer.appendLine("class $name {")


        writeProperties(writer, clazz.properties)
        writer.appendLine("}")
        writer.appendLine()
    }

    fun writeProperties(writer: Writer, properties: List<PklProperty>, indent: String = "  ") {
        properties.forEach{ p ->
            if (p.secret) {
                writer.appendLine("${indent}@renderer.SecretField")
            }
            val name = Lexer.maybeQuoteIdentifier(p.name)
            writer.append("${indent}${name}: ${p.type}")
            if (p.defaultValue != null) {
                writer.append(" = ${p.defaultValue}")
            } else {
                writer.append("?")
            }
            writer.appendLine()
        }
    }
}