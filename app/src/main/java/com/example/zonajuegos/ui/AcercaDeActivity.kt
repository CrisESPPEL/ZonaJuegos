package com.example.zonajuegos.ui

import com.example.zonajuegos.R
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Pantalla informativa, con atribución de FreeToGame y retorno a la pantalla anterior. */
class AcercaDeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acerca_de)

        // FreeToGame exige atribución con un enlace activo a su página.
        findViewById<TextView>(R.id.tvAtribucionFreeToGame).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.freetogame.com/api-doc")))
        }
        // finish() devuelve a la pantalla desde la que se abrió esta sección.
        findViewById<Button>(R.id.btnVolverAcercaDe).setOnClickListener { finish() }
    }
}
