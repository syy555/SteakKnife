package com.cat.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class TestPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        println("------------------开始----------------------")
        println("自定义插件!")
        target.android.registerTransform(new JavasistTransform())
        println("------------------结束----------------------->")
    }
}
