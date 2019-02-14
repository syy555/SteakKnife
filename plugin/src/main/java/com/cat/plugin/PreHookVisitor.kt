package com.cat.plugin

import com.cat.plugin.HookTransform.Companion.hookItems
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM5

class PreHookVisitor : ClassVisitor(ASM5) {
    private var className: String = ""

    private var isHookDelegate = false

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<String>?) {
        super.visit(version, access, name, signature, superName, interfaces)
        className = name
    }

//
//    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
//        if (descriptor?.startsWith("Lfm/qingting/router/annotations/HookDelegate;") == true) {
//            isHookDelegate = true
//        }
//        return super.visitAnnotation(descriptor, visible)
//    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<String>?): MethodVisitor? {
        var mv = super.visitMethod(access, name, descriptor, signature, exceptions)
//        if (!isHookDelegate) {
//            return mv
//        }
        mv = object : MethodVisitor(ASM5, mv) {
            var methodName = name
            var methodDesc = descriptor
            val targetClasses = mutableListOf<String>()
            var scope = mutableListOf<String>()
            var impactPackages = mutableListOf<String>()
            var policyInclude = true
            val hookItem = HookItem(name, descriptor, HookItem.Scope())
            override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                println("Pre-hook $className:$methodName$methodDesc")
                var av = super.visitAnnotation(descriptor, visible)
                val targetClasses = mutableListOf<String>()
                if (descriptor.startsWith("Lfm/qingting/router/annotations/HookParams;")) {
                    println("Found hook annation in $className:$methodName$methodDesc")
                    av = object : AnnotationVisitor(ASM5, av) {
                        override fun visitEnum(name: String?, descriptor: String?, value: String?) {
                            println("visit annotation $name + $value")
                            policyInclude = name == "policy" && value != "EXCLUDE"
                            println("policyInclude: $policyInclude")
                            super.visitEnum(name, descriptor, value)
                        }

                        override fun visit(name: String?, value: Any?) {
                            if(name == "isStatic"){
                                hookItem.isStatic = value as Boolean
                            }
                        }

                        override fun visitArray(truename: String?): AnnotationVisitor? {
                            var av = super.visitArray(name)
                            av = object : AnnotationVisitor(ASM5, av) {

                                override fun visit(name: String?, value: Any?) {
                                    when (truename) {
                                        "targetClass" -> {
                                            targetClasses.add(value.toString().substring(1, value.toString().length - 1))
                                        }

                                        "scope" -> {
                                            scope.add(value.toString().substring(1, value.toString().length - 1))
                                        }

                                        "impactPackage" -> if (value is String) {
                                            impactPackages.add(value.replace("\\.".toRegex(), "/"))
                                        }
                                    }
                                    super.visit(name, value)
                                }
                            }
                            return av
                        }

                        override fun visitEnd() {
                            super.visitEnd()
                            hookItem.originalOwners.addAll(targetClasses)
                            if (policyInclude) {
                                hookItem.scope.include.addAll(scope)
                            } else {
                                hookItem.scope.exclude.addAll(scope)
                            }
                            hookItem.targetOwner = className
                            hookItem.scope.include.addAll(impactPackages)
                            hookItem.scope.exclude.add(className)
                            hookItems.add(hookItem)
                        }
                    }
                }
                return av
            }
        }
        return mv
    }
}
