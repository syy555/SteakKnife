package com.cat.plugin

import com.android.build.api.transform.Transform
import org.gradle.api.Plugin
import org.gradle.api.Project

class HookPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val appExtension = project.properties["android"]!!
        val method = appExtension.javaClass.getMethod("registerTransform", Transform::class.java,Class.forName("[Ljava.lang.Object;"))
        method.isAccessible = true
        method.invoke(appExtension,HookTransform(), arrayOf<Any>())
    }
}
