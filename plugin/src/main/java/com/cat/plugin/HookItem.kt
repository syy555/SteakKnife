package com.cat.plugin

import jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC
import java.util.*

data class HookItem(var methodName: String, var desc: String, var scope: Scope, var originalOwners: MutableList<String> = mutableListOf(), var targetOwner: String = "") {


    fun checkInjection(className: String, s: String): Boolean {
        if (!scope.checkInScope(className)) {
            return false
        }
        var containsOwnerName = false
        for (owner in originalOwners) {
            if (s.contains(owner)) {
                containsOwnerName = true
                break
            }
        }
        if (!containsOwnerName) {
            return false
        }
        if (s.contains(desc) && s.contains(methodName)) {
            return true
        }
        return false
    }

    fun transformMethod(opcode: Int, owner: String, name: String, methodDesc: String): Array<String>? {
        if (!originalOwners.contains(owner)) return null
        if (methodName == name && desc == methodDesc) {
            val result = arrayOfNulls<String>(3)
            var actualDesc = if (opcode == INVOKESTATIC) {
                desc
            } else {
                "(L" + originalOwners[0] + ";" + desc.substring(1)
            }
            return arrayOf(targetOwner, methodName, actualDesc)
        }
        return null
    }

    class Scope {
        var include: MutableList<String> = ArrayList()
        var exclude: MutableList<String> = ArrayList()

        fun checkInScope(name: String): Boolean {
            var ret = false
            if(include.isEmpty()){
                ret = true
            }
            for (s in include) {
                if (name.startsWith(s)) {
                    println("Hook job name:$name : $s end")
                    ret = true
                    break
                }
            }
            if (ret) {
                for (s in exclude) {
                    if (name.startsWith(s)) {
                        return false
                    }
                }
            }
            return ret
        }
    }
}