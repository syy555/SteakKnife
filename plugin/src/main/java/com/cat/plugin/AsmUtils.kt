package com.cat.plugin

import java.lang.reflect.Method
import java.lang.reflect.Type
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

object AsmUtils {

    val isAndroid: Boolean
        get() = isAndroid(System.getProperty("java.vm.name"))

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {

        println(AsmUtils.getDesc(System::class.java))
        println(AsmUtils.getDesc(String::class.java))
        println(AsmUtils.getDesc(Int::class.java))
        println(AsmUtils.getDesc(Int::class.javaPrimitiveType!!))

        val method = AsmUtils::class.java.getDeclaredMethod("main", Array<String>::class.java)
        println("javah -jni")
        println(AsmUtils.getDesc(method))
        println(AsmUtils.getType(System::class.java))
        println(AsmUtils.getType(AsmUtils::class.java))
    }

    fun isAndroid(vmName: String): Boolean {
        val lowerVMName = vmName.toLowerCase()
        return lowerVMName.contains("dalvik") || lowerVMName.contains("lemur")
    }

    fun getDesc(method: Method): String {
        val buf = StringBuffer()
        buf.append("(")
        val types = method.parameterTypes
        for (i in types.indices) {
            buf.append(getDesc(types[i]))
        }
        buf.append(")")
        buf.append(getDesc(method.returnType))
        return buf.toString()
    }

    fun getDesc(returnType: Class<*>): String {
        if (returnType.isPrimitive) {
            return getPrimitiveLetter(returnType)
        }
        return if (returnType.isArray) {
            "[" + getDesc(returnType.componentType)
        } else "L" + getType(returnType) + ";"
    }

    fun getType(parameterType: Class<*>): String {
        if (parameterType.isArray) {
            return "[" + getDesc(parameterType.componentType)
        }
        if (!parameterType.isPrimitive) {
            val clsName = parameterType.name
            return clsName.replace("\\.".toRegex(), "/")
        }
        return getPrimitiveLetter(parameterType)
    }

    fun getPrimitiveLetter(type: Class<*>): String {
        if (Integer.TYPE == type) {
            return "I"
        }
        if (Void.TYPE == type) {
            return "V"
        }
        if (java.lang.Boolean.TYPE == type) {
            return "Z"
        }
        if (Character.TYPE == type) {
            return "C"
        }
        if (java.lang.Byte.TYPE == type) {
            return "B"
        }
        if (java.lang.Short.TYPE == type) {
            return "S"
        }
        if (java.lang.Float.TYPE == type) {
            return "F"
        }
        if (java.lang.Long.TYPE == type) {
            return "J"
        }
        if (java.lang.Double.TYPE == type) {
            return "D"
        }
        throw IllegalStateException("Type: " + type.canonicalName + " is not a primitive type")
    }

    fun getMethodType(clazz: Class<*>, methodName: String): Type? {
        try {
            val method = clazz.getMethod(methodName, *arrayOfNulls<Class<*>>(0) as Array<Class<*>>)
            return method.genericReturnType
        } catch (ex: Exception) {
            return null
        }

    }

    fun getFieldType(clazz: Class<*>, fieldName: String): Type? {
        try {
            val field = clazz.getField(fieldName)
            return field.genericType
        } catch (ex: Exception) {
            return null
        }

    }

    fun getMethodDescriptor(method: ExecutableElement): String {
        if (method.kind != ElementKind.METHOD) {
            throw IllegalStateException(method.simpleName.toString() + "is not a method")
        }
        val parameters: List<VariableElement> = method.parameters
        val returnType: TypeMirror = method.returnType
        val stringBuilder = StringBuilder()
        stringBuilder.append('(')
        parameters.forEach {
            appendDescriptor(stringBuilder, it.asType().toString())
        }
        stringBuilder.append(')')
        appendDescriptor(stringBuilder, returnType.toString())
        return stringBuilder.toString()
    }


    private fun appendDescriptor(stringBuilder: StringBuilder, name: String) {
        var temp = name
        while (temp.endsWith("[]")) {
            temp = temp.substring(0, temp.length - 2)
            stringBuilder.append('[')
        }
        when (temp) {
            "int" -> stringBuilder.append('I')
            "void" -> stringBuilder.append('V')
            "boolean" -> stringBuilder.append('Z')
            "byte" -> stringBuilder.append('B')
            "char" -> stringBuilder.append('C')
            "short" -> stringBuilder.append('s')
            "double" -> stringBuilder.append('D')
            "float" -> stringBuilder.append('F')
            "long" -> stringBuilder.append('J')
            else -> {
                stringBuilder.append('L')
                val nameLength = temp.length
                for (i in 0 until nameLength) {
                    val car = temp[i]
                    stringBuilder.append(if (car == '.') '/' else car)
                }
                stringBuilder.append(';')
            }
        }
    }

}
