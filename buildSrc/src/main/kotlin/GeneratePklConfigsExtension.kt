import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import spec.GeneratorSpec
import java.util.regex.Pattern
import javax.inject.Inject

open class GeneratePklConfigsExtension @Inject constructor(project: Project, objectFactory: ObjectFactory) {
    var generators: NamedDomainObjectContainer<GeneratorSpec>
            = objectFactory.domainObjectContainer(GeneratorSpec::class.java)
    var secretFields : MutableSet<Pattern> = HashSet()

    fun generators(
        action: Action<NamedDomainObjectContainer<GeneratorSpec>>
    ) {
        action.execute(generators)
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