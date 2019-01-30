package com.cat.plugin

import com.google.auto.service.AutoService
import com.google.gson.Gson
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import fm.qingting.router.annotations.HookParams
import java.io.FileWriter
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic


/**
 * Created by lee on 2018/3/12.
 */
@AutoService(Processor::class)
@SuppressWarnings("unused")
class AnnotationProcessor : AbstractProcessor() {


    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(HookParams::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(set: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val hookitems = mutableListOf<HookItem>()
        val mutableSet = roundEnv.getElementsAnnotatedWith(HookParams::class.java)
        for (method in ElementFilter.methodsIn(mutableSet)) {
            val targetClasses = mutableListOf<String>()
            var scope = mutableListOf<String>()
            var impactPackages = mutableListOf<String>()
            var policyInclude = true
            if (method.kind != ElementKind.METHOD) {
                throw IllegalStateException(method.simpleName.toString() + "is not a method")
            }
//            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING,
//                    "method descriptor:" + AsmUtils.getMethodDescriptor(method))

            val classElement = method?.enclosingElement as TypeElement
//            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING,
//                    "class:" + classElement.asType().toString().replace("\\.".toRegex(), "/"))

            val annotationMirrors = method.annotationMirrors
            annotationMirrors.forEach {
                processingEnv.messager.printMessage(Diagnostic.Kind.WARNING,
                        "annotation:" + it.toString())
                val elementValues = it.elementValues
                elementValues.entries.forEach { element ->
                    val key = element.key.simpleName.toString()
                    val value = element.value.value
                    when (key) {
                        "targetClass" -> {
                            val items = value as List<*>
                            items.forEach { target ->
                                targetClasses.add(target.toString().replace("\\.".toRegex(), "/"))
                            }
                        }
                        "policy" -> {
                            policyInclude = value != "EXCLUDE"
                        }

                        "scope" -> {
                            val items = value as List<*>
                            items.forEach { target ->
                                scope.add(target.toString().replace("\\.".toRegex(), "/"))
                            }
                        }

                        "impactPackage" -> {
                            val items = value as List<*>
                            items.forEach { target ->
                                impactPackages.add(target.toString().replace("\"", "").replace("\\.".toRegex(), "/"))
                            }
                        }
                    }
                }
            }

            val hookItem = HookItem(method.simpleName.toString(), AsmUtils.getMethodDescriptor(method), HookItem.Scope())
            hookItem.originalOwners.addAll(targetClasses)
            if (policyInclude) {
                hookItem.scope.include.addAll(scope)
            } else {
                hookItem.scope.exclude.addAll(scope)
            }
            hookItem.targetOwner = classElement.asType().toString().replace("\\.".toRegex(), "/")
            hookItem.scope.include.addAll(impactPackages)
            hookItem.scope.exclude.add(classElement.asType().toString().replace("\\.".toRegex(), "/"))
            PreHookTransform.hookItems.add(hookItem)
            hookitems.add(hookItem)
        }
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING,
                "hookitems1:" + Gson().toJson(hookitems))
        if(hookitems.size!=0) {
            val builder = TypeSpec.classBuilder("hookitems")
            val scBuilder = CodeBlock.builder()
            scBuilder.addStatement("java.lang.String hookitems = \""+Gson().toJson(hookitems).replace("\"","\\\""))
            builder.addStaticBlock(scBuilder.build())
            val javaFile = JavaFile.builder("com.cat.hook", builder.build()).build()
            javaFile.writeTo(this.processingEnv.filer)
        }
        return true
    }


//    @Throws(IOException::class)
//    private fun generateClassRouter(moduleName: String?, map: Map<String, Element>, methodMap: Map<String, ExecutableElement>) {
//        if (map.isEmpty() && methodMap.isEmpty()) {
//            return
//        }
//        val methodKeyCache = HashSet<String>()
//        val builder = classBuilder(if (moduleName != null && moduleName.isNotEmpty())
//            moduleName
//        else
//            CLASS_NAME)
//                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
//        var mcBuilder = MethodSpec.constructorBuilder()
//                .addModifiers(Modifier.PRIVATE)
//        builder.addMethod(mcBuilder.build())
//        mcBuilder = MethodSpec.methodBuilder("init")
//                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//        builder.addMethod(mcBuilder.build())
//        //
//        val scBuilder = CodeBlock.builder()
//        scBuilder.addStatement("java.util.Set<String> methodPathCache=new java.util.HashSet<>()")
//        for (key in map.keys) {
//            scBuilder.addStatement("Router.INSTANCE.registerUrl(\"$key\",${map[key]}.class)")
//        }
//        if (methodMap.keys.isNotEmpty()) {
//            scBuilder.beginControlFlow("RouterIntercept methodRouteIntercept = new RouterIntercept()")
//                    .beginControlFlow("public boolean launch($contextClassName context, $uriClassName uri, $callbackClassName callback, $bundleClassName options, $lifeCycleClassName lifeCycle)")
//            scBuilder.beginControlFlow("switch(uri.getHost()+uri.getPath())")
//            methodMap.keys.forEach {
//                val method = methodMap[it]
//                if (checkHasNoErrors(method)) {
//                    val classElement = method?.enclosingElement as TypeElement
//                    scBuilder.addStatement("case \"$it\":")
//                            .addStatement("${classElement.qualifiedName}.${method.simpleName}(${getParameterString(method.parameters)})")
//                            .addStatement("return true")
//                    methodKeyCache.add(it)
//                }
//            }
//            scBuilder.endControlFlow().addStatement("return false")
//                    .endControlFlow()
//                    .addStatement("}")
//            methodKeyCache.forEach {
//                scBuilder.addStatement("methodPathCache.add(\"$it\")")
//            }
//            scBuilder.addStatement("Router.INSTANCE.registerMethodIntercept(methodRouteIntercept,methodPathCache)")
//        }
//        if (processingEnv.options.containsKey(ATTACH_NAME)) {
//
//            assertIsEmpty(processingEnv.options[ATTACH_NAME])
//            processingEnv.options[ATTACH_NAME]?.split(",")?.forEach {
//                scBuilder.addStatement(String.format(Locale.getDefault(),
//                        "fm.qingting.router.%s.init()",
//                        it))
//            }
//
//        }
//        builder.addStaticBlock(scBuilder.build())
//        val javaFile = JavaFile.builder("fm.qingting.router", builder.build()).build()
//        javaFile.writeTo(this.processingEnv.filer)
//    }
//
//    private fun assertIsEmpty(value: String?) {
//        if (value == null || value.trim { it <= ' ' }.isEmpty()) {
//            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING,
//                    "annotationProcessorOptions arguments error")
//        }
//    }
//
//    private fun checkHasNoErrors(element: ExecutableElement?): Boolean {
//        if (element?.modifiers?.contains(Modifier.STATIC) == false) {
//            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Subscriber method must be static", element)
//            return false
//        }
//
//        if (element?.modifiers?.contains(Modifier.PUBLIC) == false) {
//            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Subscriber method must be public", element)
//            return false
//        }
//
//        return true
//    }
//
//    private fun getParameterString(parameterList: List<VariableElement>): String {
//        var result = ""
//        parameterList.forEach {
//            when (it.asType().toString()) {
//                contextClassName.toString() -> {
//                    if (!result.isEmpty()) {
//                        result += ","
//                    }
//                    result += "context"
//                }
//                uriClassName.toString() -> {
//                    if (!result.isEmpty()) {
//                        result += ","
//                    }
//                    result += "uri"
//                }
//                "fm.qingting.router.RouterTaskCallBack" -> {
//                    if (!result.isEmpty()) {
//                        result += ","
//                    }
//                    result += "callback"
//                }
//
//                bundleClassName.toString() -> {
//                    if (!result.isEmpty()) {
//                        result += ","
//                    }
//                    result += "options"
//                }
//                lifeCycleClassName.toString() -> {
//                    if (!result.isEmpty()) {
//                        result += ","
//                    }
//                    result += "lifeCycle"
//                }
//            }
//        }
//        return result
//    }

    companion object {
        private const val CLASS_NAME = "HookList"
    }

}