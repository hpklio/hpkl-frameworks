package spec

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern
import javax.inject.Inject

enum class GeneratorType {
    SPRING
}

abstract class GeneratorSpec @Inject constructor(
    project: Project, objects: ObjectFactory
) : Named {
    private val counter: AtomicLong = AtomicLong()

    var dependencies : MutableList<DependencySpec> = ArrayList()
    var secretFields : MutableSet<Pattern> = HashSet()

    val kind = objects.property(GeneratorType::class.java).convention(GeneratorType.SPRING)

    val outputDir : DirectoryProperty =
        objects.directoryProperty().convention(
            project.layout.buildDirectory.map { it: Directory ->
                it.dir("generated").dir("pkl").dir(name)
            }
        )

    fun dependency(value: String) {
        dependencies.add(
            DependencySpec(
                id = counter.incrementAndGet(),
                artifact = value
            )
        )
    }

    fun secret(value: String) {
        secretFields.add(Pattern.compile(value))
    }

    fun secret(value: Pattern) {
        secretFields.add(value)
    }

    fun secretPatterns(values: Collection<Pattern>) {
        secretFields.addAll(values)
    }

    fun secrets(values: Collection<String>) {
        secretFields.addAll(values.map { Pattern.compile(it) })
    }
}