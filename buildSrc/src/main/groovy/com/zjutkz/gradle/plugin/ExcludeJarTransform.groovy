package com.zjutkz.gradle.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.google.common.collect.Sets
import com.google.common.io.ByteStreams
import org.gradle.api.Project

import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
/**
 * Created by kangzhe on 19/5/16.
 */

class ExcludeJarTransform extends Transform {

    private static final TRANSFORM_NAME = "ExcludeJar";

    private Project mProject
    private List<String> mExcludeList;

    ExcludeJarTransform(Project project) {
        mProject = project
        init(project)
    }

    @Override
    String getName() {
        return TRANSFORM_NAME
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        if (mProject.plugins.hasPlugin(AppPlugin)) {
            return TransformManager.SCOPE_FULL_PROJECT
        } else if (mProject.plugins.hasPlugin(LibraryPlugin)) {
            return Sets.immutableEnumSet(QualifiedContent.Scope.PROJECT)
        }
        return null;
    }

    @Override
    boolean isIncremental() {
        return true
    }

    private void init(Project project) {
        project.afterEvaluate {
            ExcludeJarExtension extension = project.extensions.getByName(ExcludeJarExtension.NAME)
            if (extension != null) {
                if (extension.jarExcludes != null) {
                    mExcludeList = extension.jarExcludes.stream()
                            .filter { !it.isAllWhitespace() }
                            .collect()
                }
            }
        }
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        TransformOutputProvider outputProvider = transformInvocation.outputProvider
        if (!transformInvocation.incremental) {
            outputProvider.deleteAll()
        }
        transformInvocation.inputs.forEach { TransformInput input ->
            input.jarInputs.forEach { JarInput jarInput ->
                if (!transformInvocation.incremental) {
                    File targetJar = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                    excludeFilesFromJar(jarInput.file, targetJar)
                } else {
                    switch(jarInput.status) {
                        case Status.NOTCHANGED:
                            break;
                        case Status.ADDED:
                        case Status.CHANGED:
                            File targetJar = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                            excludeFilesFromJar(jarInput.file, targetJar)
                            break;
                        case Status.REMOVED:
                            File outJarFile = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                            FileUtils.deleteIfExists(outJarFile)
                            break;
                    }
                }
            }
            input.directoryInputs.forEach { DirectoryInput directoryInput ->
                File targetDir = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                if (targetDir.exists()) {
                    FileUtils.deleteDirectoryContents(targetDir)
                }
                targetDir.mkdirs()
                FileUtils.copyDirectory(directoryInput.file, targetDir)
            }
        }
    }

    private void excludeFilesFromJar(File srcJar, File targetJar) {
        if (targetJar.exists()) {
            targetJar.delete()
        }
        targetJar.createNewFile()
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(srcJar))
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(targetJar))
        def zipEntry = null
        while ((zipEntry = zipInputStream.nextEntry) != null) {
            if (match(mExcludeList, zipEntry.name)) {
                continue
            }
            zipOutputStream.putNextEntry(zipEntry)
            ByteStreams.copy(zipInputStream, zipOutputStream)
            zipOutputStream.closeEntry()
        }
        zipInputStream.close()
        zipOutputStream.close()
    }

    private boolean match(List<String> patternList, String file) {
        if (patternList.isEmpty() || file == null) {
            return false
        }

        for (String pattern: patternList) {
            String packageName = pattern.replace('.', '/');
            if (file.contains(packageName)) {
                return true
            }
        }
        return false
    }
}
