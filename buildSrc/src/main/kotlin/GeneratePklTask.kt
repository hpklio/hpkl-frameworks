import generator.SpringMetadataReader
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.pkl.cli.CliProjectPackager
import org.pkl.cli.CliProjectResolver
import org.pkl.commons.cli.CliBaseOptions
import org.pkl.commons.cli.CliTestOptions
import spec.GeneratorSpec
import spec.GeneratorType
import java.io.PrintWriter
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject


abstract class GeneratePklTask @Inject constructor(
    private var extension : GeneratePklConfigsExtension,
    private val objectFactory: ObjectFactory,
    private val providerFactory: ProviderFactory
) : DefaultTask() {

    private val dependenciesLocator = DependenciesLocator()

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    fun getDependencies() : ConfigurableFileCollection {
        val dependencies = providerFactory.provider {
            extension.generators.flatMap {
                it.dependencies.map {
                    dependency -> dependenciesLocator.resolveLocator(project, it, dependency).artifactFiles
                }
            }
        }
        return this.objectFactory.fileCollection().from(dependencies)
    }

    @OutputDirectories
    fun getOutputDirectries() : ConfigurableFileCollection {
        return this.objectFactory.fileCollection().from(
            extension.generators.map {
                it.outputDir
            }
        )
    }


    @TaskAction
    fun generate() {
        extension.generators.forEach {
            generate(it, extension.secretFields)
        }
    }

    fun generate(spec: GeneratorSpec, secretFields: Set<Pattern>) {
        println("Generating ${spec.name}")
        val files = spec.dependencies.flatMap {
            val resolvedDependency = dependenciesLocator.resolveLocator(project, spec, it)
            resolvedDependency.artifactFiles.files
        }

        if (spec.kind.get() == GeneratorType.SPRING) {

            val generator = SpringMetadataReader(spec, secretFields, files)
            val modules = generator.generate()

            val writer = PklModuleWriter()
            modules.forEach { p ->
                writer.write(p.first, p.second)
            }


            val outputDir = spec.outputDir.get()
            javaClass.getResourceAsStream("templates/PklProject.template").use { stream ->
                outputDir.file("PklProject").asFile.bufferedWriter(Charsets.UTF_8).use { w ->
                    stream?.reader().use { reader ->
                        reader?.readLines()?.forEach { line ->
                            w.write(line.replace("{{frameworkName}}", spec.name))
                            w.write("\n")
                        }
                    }
                }
            }


            val baseOptions = CliBaseOptions(
                Collections.emptyList(), //@NotNull List<URI> sourceModules,
                listOf(
                    "repl:",
                    "file:",
                    "modulepath:",
                    "https:",
                    "pkl:",
                    "package:",
                    "projectpackage:"
                ).map { Pattern.compile(it) }, //@Nullable List<Pattern> allowedModules,
                listOf(
                    "env:",
                    "prop:",
                    "file:",
                    "modulepath:",
                    "https:",
                    "package:"
                ).map { Pattern.compile(it) }, // @Nullable List<Pattern> allowedResources,
                null, // @Nullable Map<String, String> environmentVariables,
                null, // @Nullable Map<String, String> externalProperties,
                null, // @Nullable List<? extends Path> modulePath,
                outputDir.asFile.toPath(), // @NotNull Path workingDir,
                null, // @Nullable Path rootDir
                null, // @Nullable URI settings,
                null, // @Nullable Path projectDir,
                null, // @Nullable Duration timeout,
                null, // @Nullable Path moduleCacheDir,
                null, // @Nullable Color color,
                true, // boolean noCache
                false, // boolean omitProjectSettings
                false, // boolean noProject
                false, // boolean testMode
                0, //int testPort
                Collections.emptyList(), // @NotNull List<? extends Path> caCertificates,
                null, // @Nullable URI httpProxy
                null, // @Nullable List<String> httpNoProxy
                mapOf(), // @NotNull Map<String, PklEvaluatorSettings.ExternalReader> externalModuleReaders
                mapOf(), // @NotNull Map<String, PklEvaluatorSettings.ExternalReader> externalResourceReaders
            )

            val resolver = CliProjectResolver(
                baseOptions,
                listOf(outputDir.asFile.toPath()),
                PrintWriter(System.out),
                PrintWriter(System.err)
            )
            resolver.run()

            val packager = CliProjectPackager(
                baseOptions,
                listOf(outputDir.asFile.toPath()),
                CliTestOptions(),
                outputDir.dir("dist").asFile.absolutePath,
                true,
                PrintWriter(System.out),
                PrintWriter(System.err)
            )

            packager.run()
        }
    }


}