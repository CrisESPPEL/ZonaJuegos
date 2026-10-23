package com.example.zonajuegos

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AcercaDeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_acerca_de)

        val btnVolver = findViewById<Button>(R.id.btnVolverAcercaDe)

        btnVolver.setOnClickListener { finish() }
    }

}