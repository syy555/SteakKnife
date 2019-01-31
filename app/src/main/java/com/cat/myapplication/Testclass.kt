package com.cat.myapplication

import fm.qingting.router.annotations.HookParams
import fm.qingting.router.annotations.Policy

object Testclass {
    @JvmStatic
    @HookParams(targetClass = [String::class],scope = [ToastUtils::class])
    fun valueOf(obj: Int): String {
        return "null"
    }
}
