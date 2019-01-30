package com.cat.myapplication

import fm.qingting.router.annotations.HookParams
import fm.qingting.router.annotations.Policy

object Testclass {
    @HookParams(targetClass = [String::class], policy = Policy.EXCLUDE, impactPackage = ["www.qingting.fm", "com.cat.fm"])
    fun valueOf(obj: Any?): String {
        return obj?.toString() ?: "null"
    }
}
