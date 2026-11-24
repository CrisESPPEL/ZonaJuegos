package com.example.zonajuegos.data.local

import android.content.Context
import com.example.zonajuegos.data.entity.Partida
import com.example.zonajuegos.data.entity.JuegoCatalogo
import com.example.zonajuegos.data.entity.Videojuego
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Base Room central: una única instancia compartida durante la vida del proceso. */
@Database(entities = [Videojuego::class, Partida::class, JuegoCatalogo::class], version = 5, exportSchema = false)
abstract class VideojuegoDatabase : RoomDatabase() {

    /** DAO que ofrece lecturas reactivas y escrituras suspendidas. */
    abstract fun videojuegoDao(): VideojuegoDao

    companion object {
        /** Volatile hace visible la instancia correctamente entre hilos. */
        @Volatile
        private var INSTANCE: VideojuegoDatabase? = null

        /**
         * Migra la tabla existente sin borrar los juegos que ya se guardaron en
         * la versión 1. Las columnas nuevas reciben valores seguros para filas
         * antiguas; los campos opcionales empiezan como NULL.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE videojuegos ADD COLUMN imagenUrl TEXT")
                // Campos heredados de una versión anterior; se conservan para actualizar Room sin perder datos.
                db.execSQL("ALTER TABLE videojuegos ADD COLUMN rawgId INTEGER")
                db.execSQL("ALTER TABLE videojuegos ADD COLUMN trailerUrl TEXT")
                db.execSQL("ALTER TABLE videojuegos ADD COLUMN tiempoCompletar TEXT")
                db.execSQL("ALTER TABLE videojuegos ADD COLUMN tieneLogros INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE videojuegos ADD COLUMN numeroLogros INTEGER")
                db.execSQL("ALTER TABLE videojuegos ADD COLUMN creadoEn INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE videojuegos ADD COLUMN vecesJugado INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Añade el historial local de sesiones sin perder los datos de cada juego. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS partidas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        videojuegoId INTEGER NOT NULL,
                        latitud REAL NOT NULL,
                        longitud REAL NOT NULL,
                        fecha INTEGER NOT NULL,
                        FOREIGN KEY(videojuegoId) REFERENCES videojuegos(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_partidas_videojuegoId ON partidas(videojuegoId)")
            }
        }

        /** Añade el estado de progreso y asigna «No jugado» a juegos anteriores. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE videojuegos ADD COLUMN estado TEXT NOT NULL DEFAULT 'No jugado'"
                )
            }
        }

        /**
         * Añade el enlace del catálogo a la biblioteca existente y crea una
         * tabla separada para cachear las respuestas de FreeToGame.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE videojuegos ADD COLUMN urlJuego TEXT")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS catalogo_juegos (
                        id INTEGER NOT NULL PRIMARY KEY,
                        titulo TEXT NOT NULL,
                        resumen TEXT NOT NULL,
                        descripcion TEXT NOT NULL,
                        genero TEXT NOT NULL,
                        plataforma TEXT NOT NULL,
                        miniaturaUrl TEXT,
                        imagenesJson TEXT NOT NULL,
                        desarrollador TEXT NOT NULL,
                        editor TEXT NOT NULL,
                        fechaLanzamiento TEXT NOT NULL,
                        urlJuego TEXT NOT NULL,
                        requisitosMinimos TEXT NOT NULL,
                        actualizadoEn INTEGER NOT NULL
                    )""".trimIndent()
                )
            }
        }

        /**
         * Devuelve la base compartida; synchronized evita abrir dos bases a la
         * vez si dos pantallas solicitan el DAO al mismo tiempo.
         */
        fun getDatabase(context: Context): VideojuegoDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                VideojuegoDatabase::class.java,
                "zonajuegos_database"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                .also { INSTANCE = it }
        }
    }
}
