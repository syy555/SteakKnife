package com.cat.plugin

import java.util.*

data class HookItem(var methodName: String, var desc: String, var scope: Scope, var originalOwners: MutableList<String> = mutableListOf(), var targetOwner: String = "", var isStatic: Boolean = true) {


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
        if ((isStatic && s.contains(desc) || (!isStatic && s.contains(getUnStaticMethodDesc())))) {
            return true
        }
        return false
    }


    fun transformMethod(opcode: Int, owner: String, name: String, methodDesc: String): Array<String>? {
        if (!originalOwners.contains(owner)) return null
        if (methodName == name && ((isStatic && desc == methodDesc) || (!isStatic && methodDesc == getUnStaticMethodDesc()))) {
            return arrayOf(targetOwner, methodName, desc)
        }
        return null
    }

    fun getUnStaticMethodDesc(): String {
        return if (desc.contains(";L")) {
            "(" + desc.substring(desc.indexOf(";L") + 1, desc.length)
        } else {
            "(" + desc.substring(desc.lastIndexOf(";)") + 1, desc.length)
        }
    }
//


    class Scope {
        var include: MutableList<String> = ArrayList()
        var exclude: MutableList<String> = ArrayList()

        fun checkInScope(name: String): Boolean {
            var ret = false
            if (include.isEmpty()) {
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