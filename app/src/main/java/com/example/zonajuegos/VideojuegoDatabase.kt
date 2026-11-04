
package com.example.zonajuegos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Videojuego::class],
    version = 1,
    exportSchema = false
)
abstract class VideojuegoDatabase : RoomDatabase() {

    abstract fun videojuegoDao(): VideojuegoDao

    companion object {

        @Volatile
        private var INSTANCE: VideojuegoDatabase? = null

        fun getDatabase(context: Context): VideojuegoDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VideojuegoDatabase::class.java,
                    "zonajuegos_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}
