package com.example.zonajuegos

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DetalleVideojuegoActivity : AppCompatActivity {

    companion object {
        const val EXTRA_TITULO = "extra_titulo"
        const val EXTRA_GENERO = "extra_genero"
        const val EXTRA_PLATAFORMA = "extra_plataforma"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_detalle_videojuego)

        // Recuperamos los datos enviados desde MainActivity.kt
        val titulo = intent.getStringExtra(EXTRA_TITULO) ?: "Sin título"
        val genero = intent.getStringExtra(EXTRA_GENERO) ?: "Sin género"
        val plataforma = intent.getStringExtra(EXTRA_PLATAFORMA) ?: "Sin plataforma"

        // Buscamos los componentes de la interfaz
        val tvTitulo = findViewById<TextView>(R.id.tvDetalleTitulo)
        val tvGenero = findViewById<TextView>(R.id.tvDetalleGenero)
        val tvPlataforma = findViewById<TextView>(R.id.tvDetallePlataforma)
        val ivPortada = findViewById<ImageView>(R.id.ivPortada)
        val btnVolver = findViewById<Button>(R.id.btnVolverDetalle)

        // Mostramos los datos
        tvTitulo.text = titulo
        tvGenero.text = "Género: $genero"
        tvPlataforma.text = "Plataforma: $plataforma"

        // Imagen provisional usando un recurso de Android
        ivPortada.setImageResource(
            android.R.drawable.ic_media_play
        )

        // Regresamos a la pantalla anterior
        btnVolver.setOnClickListener { finish() }

    }
}