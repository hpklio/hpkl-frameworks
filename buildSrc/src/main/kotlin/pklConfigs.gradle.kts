import org.gradle.api.DefaultTask

val pklConfigGenerator =
    project.extensions.create<GeneratePklConfigsExtension>("pklConfigs", project)


val generateTask = tasks.register("generatePkl", GeneratePklTask::class, pklConfigGenerator)

val uploadTask = tasks.register("uploadPkl", UploadPklTask::class, pklConfigGenerator)
