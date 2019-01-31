package com.cat.myapplication

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var a = String.javaClass
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.tv_1).setOnClickListener {
            Toast.makeText(this, "4321", Toast.LENGTH_SHORT).show()
        }
    }

}
