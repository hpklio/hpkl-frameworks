import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import spec.DependencySpec
import spec.GeneratorSpec
import java.util.concurrent.ConcurrentHashMap


data class ResolvedDependency(
    val dependency: DependencySpec,
    val artifactFiles: FileCollection,
    val simplifiedArtifactName: String)

@Suppress("UNCHECKED_CAST")
data class Notation(
    val group: String,
    val name: String,
    val version: String,
    val classifier: String?
) {
    fun toMap() : Map<String, String> = mapOf(
        "group" to group,
        "name" to name,
        "version" to version,
        "classifier" to classifier
    ).filter { it.value != null }.toMap() as Map<String, String>
}

class DependenciesLocator {

    val cache: MutableMap<String, ResolvedDependency> = ConcurrentHashMap()

    fun resolveLocator(project: Project, generator: GeneratorSpec, dependency: DependencySpec) : ResolvedDependency {
        val notation = artifactParts(dependency.artifact)

        val name = "dependencyLocator_${generator.name}_${dependency.id}"

        val res =  cache.getOrPut(name) {
            val action : Action<Configuration> = Action {
                this.setVisible(false)
                this.setTransitive(false)
            }
            val config = project.configurations.create(name, action)
            project.dependencies.add(config.name, notation.toMap())
            val resIn = ResolvedDependency(
                dependency,
                config,
                "${notation.group}:${notation.name}:${notation.version}"
            )
            resIn
        }
        return res
    }

    companion object {
        fun artifactParts(artifactCoordinate: String) : Notation {

            val artifactCoordinateTokenized = artifactCoordinate.split("@")

            val artifact = artifactCoordinateTokenized[0]

            val artifactTokenized = artifact.split(":")
            return Notation(
                group = artifactTokenized[0],
                name = artifactTokenized[1],
                version = artifactTokenized[2],
                classifier = artifactTokenized.elementAtOrNull(3)
            )
        }
    }

}