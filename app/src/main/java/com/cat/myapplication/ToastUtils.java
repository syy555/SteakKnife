package com.cat.myapplication;

import android.content.Context;
import android.widget.Toast;

public class ToastUtils {
    public static Toast makeText(Context context, CharSequence text, int duration){
        return Toast.makeText(context, "abcd", duration);
    }
}
