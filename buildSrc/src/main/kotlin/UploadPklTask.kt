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
            var envp = System.getenv().entries.map { "${it.key}=${it.value}" }.toTypedArray()
            val ghProcess = runtime.exec(
                "gh release upload v${version} ${zipPath}",
                envp,
                project.projectDir
            )

            if (ghProcess.waitFor() != 0) {
                throw RuntimeException(ghProcess.errorStream.bufferedReader().readText())
            }

            val mkDir = runtime.exec(
                "mkdir -p packages/hpkl-frameworks"
            )

            if (mkDir.waitFor() != 0) {
                throw RuntimeException(mkDir.errorStream.bufferedReader().readText())
            }

            val cpProc = runtime.exec(
                "cp $metadataPath packages/hpkl-frameworks/${it.name}@${version}"
            )

            if (cpProc.waitFor() != 0) {
                throw RuntimeException(cpProc.errorStream.bufferedReader().readText())
            }
        }
    }
}
