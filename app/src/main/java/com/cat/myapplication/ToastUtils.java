package com.cat.myapplication;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import fm.qingting.router.annotations.HookParams;
import fm.qingting.router.annotations.Policy;

public class ToastUtils {
    @HookParams(targetClass = Toast.class, policy = Policy.INCLUDE, scope = {MainActivity.class})
    public static Toast makeText(Context context, CharSequence text, int duration) {
        return Toast.makeText(context, "Toast hook:"+text, duration);
    }

    public static Application fakeApplication() {
        return new Application();
    }

    @HookParams(targetClass = MainActivity.class, isStatic = false)
    public static String test(MainActivity activity, String text) {
        Toast.makeText(activity, "non-static method hook:"+text, Toast.LENGTH_SHORT).show();
        return "4321";
    }

}
