import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class UploadPklTask @Inject constructor(
    private var extension : GeneratePklConfigsExtension,
) : DefaultTask() {

    @TaskAction
    fun upload() {
        val version = project.version.toString()
        extension.generators.forEach {
            val zipPath = it.outputDir.file("dist/${it.name}@${version}.zip").get().asFile.absolutePath
            val metadataPath = it.outputDir.file("dist/${it.name}@${version}").get().asFile.absolutePath
            val runtime = Runtime.getRuntime()
            runtime.exec(
                "gh release upload v${version} ${zipPath}"
            )
            runtime.exec(
                "mkdir -p packages/hpkl-frameworks"
            )
            runtime.exec(
                "cp $metadataPath packages/hpkl-frameworks/${it.name}@${version}"
            )
        }
    }
}
