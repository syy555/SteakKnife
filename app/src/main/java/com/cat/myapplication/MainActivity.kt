package com.cat.myapplication

import android.app.Application
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var a = String.javaClass
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.tv_1).setOnClickListener {
            Toast.makeText(this, test("123"), Toast.LENGTH_LONG).show()
        }

        var c = Application()

    }


    fun test(text: String): String {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        return "321"
    }
}
