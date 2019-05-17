package com.zjutkz.gradle.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project;

/**
 * Created by kangzhe on 19/5/16.
 */

class ExcludeJarPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create(ExcludeJarExtension.NAME, ExcludeJarExtension)
        BaseExtension android = null
        if (project.plugins.hasPlugin(AppPlugin)) {
            android = project.extensions.getByType(AppExtension)
        } else if(project.plugins.hasPlugin(LibraryPlugin)) {
            android = project.extensions.getByType(LibraryExtension)
        }

        if (android != null) {
            android.registerTransform(new ExcludeJarTransform(project))
        }
    }
}
