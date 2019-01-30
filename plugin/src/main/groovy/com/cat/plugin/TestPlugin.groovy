package com.cat.plugin

import com.android.build.api.dsl.extension.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class TestPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        AppExtension appExtension = (AppExtension)target.getProperties().get("android")
        appExtension.registerTransform()
        println("------------------start hook----------------------")
        PreHookTransform preHookTransform = new PreHookTransform()
        File file = new File(target.buildDir, "QTInject.json")
        preHookTransform.hookItemsFile = file
        target.android.registerTransform(preHookTransform)
        println("------------------hook finish----------------------->")
    }
}
