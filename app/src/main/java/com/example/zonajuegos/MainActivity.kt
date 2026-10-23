package com.example.zonajuegos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zonajuegos.ui.theme.ZonaJuegosTheme


class MainActivity : AppCompatActivity() {

    // Componentes principales de la aplicación
    // Campos de texto
    private lateinit var etTitulo: EditText
    private lateinit var etGenero: EditText
    private lateinit var etPlataforma: EditText

    // Botones
    private lateinit var btnAgregar: Button
    private lateinit var btnAcercaDe: Button

    // RecyclerView
    private lateinit var rvVideojuegos: RecyclerView

    // Lista donde se guardarán los videojuegos
    private val listaVideojuegos = mutableListOf<Videojuego>()

    private lateinit var adaptador: VideojuegoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {

        // Preparamos la pantalla cuando se crea
        super.onCreate(savedInstanceState)

        // Definimos la interfaz de MainActivity.kt en activity_main.xml
        setContentView(R.layout.activity_main)

        // Enlazamos los componentes al XML correspondiente
        etTitulo = findViewById(R.id.etTitulo)
        etGenero = findViewById(R.id.etGenero)
        etPlataforma = findViewById(R.id.etPlataforma)

        btnAgregar = findViewById(R.id.btnAgregar)
        btnAcercaDe = findViewById(R.id.btnAcercaDe)

        rvVideojuegos = findViewById(R.id.rvVideojuegos)

        // Configuración del RecyclerView
        rvVideojuegos.layoutManager = LinearLayoutManager(this)
        adaptador = VideojuegoAdapter(listaVideojuegos) {
            videojuego -> abrirDetalle(videojuego)
        }

        rvVideojuegos.adapter = adaptador

        // Función del botón para añadir un videojuego
        btnAgregar.setOnClickListener {agregarVideojuego()}

    }

    // Función para agregar un videojuego a la lista
    private fun agregarVideojuego() {

        // Obtenemos los datos del videojuego
        val titulo = etTitulo.text.toString().trim()
        val genero = etGenero.text.toString().trim()
        val plataforma = etPlataforma.text.toString().trim()

        // Validación de los campos del videojuego, muestra mensaje si los campos no han sido rellenados.
        if (titulo.isEmpty() || genero.isEmpty() || plataforma.isEmpty()) {

            Toast.makeText(
                this,
                "Rellena los campos necesarios.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val nuevoVideojuego = Videojuego(titulo, genero, plataforma)

        listaVideojuegos.add(nuevoVideojuego)

        // Aviso para el adaptador sobre la inserción de un nuevo videojuego
        adaptador.notifyItemInserted(listaVideojuegos.lastIndex)

        // Limpiamos el formulario para el próximo videojuego
        etTitulo.text.clear()
        etGenero.text.clear()
        etPlataforma.text.clear()

        // Mostramos el mensaje de confirmación
        Toast.makeText(
            this,
            "Videojuego añadido correctamente a la lista",
            Toast.LENGTH_SHORT
        ).show()

    }

    // Función para abrir los detalles de un videojuego
    private fun abrirDetalle(videojuego: Videojuego) {

        // Enviamos los valores a DetalleVideojuegoActivity.kt
        val intent = Intent(
            this,
            DetalleVideojuegoActivity::class.java
        )

        intent.putExtra(
            DetalleVideojuegoActivity.EXTRA_TITULO,
            videojuego.titulo
        )

        intent.putExtra(
            DetalleVideojuegoActivity.EXTRA_GENERO,
            videojuego.genero
        )

        intent.putExtra(
            DetalleVideojuegoActivity.EXTRA_PLATAFORMA,
            videojuego.plataforma
        )

        startActivity(intent)
    }
}