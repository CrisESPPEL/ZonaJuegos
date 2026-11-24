package com.example.zonajuegos.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/** Campos de la lista o ficha de juego que devuelve el JSON de FreeToGame. */
data class FreeToGameDto(
    val id: Int,
    val title: String,
    @SerializedName("short_description") val resumen: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val platform: String? = null,
    val thumbnail: String? = null,
    @SerializedName("game_url") val urlJuego: String? = null,
    @SerializedName("freetogame_profile_url") val urlPerfil: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    @SerializedName("release_date") val fechaLanzamiento: String? = null,
    val screenshots: List<FreeToGameScreenshotDto>? = null,
    @SerializedName("minimum_system_requirements") val requisitos: FreeToGameRequirementsDto? = null
)

/** Una captura que la API enlaza desde la ficha ampliada del juego. */
data class FreeToGameScreenshotDto(
    val id: Int? = null,
    val image: String? = null
)

/** Requisitos mínimos opcionales que aparecen en algunas fichas. */
data class FreeToGameRequirementsDto(
    val os: String? = null,
    val processor: String? = null,
    val memory: String? = null,
    val graphics: String? = null,
    val storage: String? = null
)

/**
 * Contrato REST. FreeToGame devuelve una lista JSON en `/games` y un único
 * objeto JSON en `/game?id=...`; no requiere API key.
 */
interface FreeToGameService {
    @GET("games")
    suspend fun listarJuegos(
        @Query("platform") plataforma: String? = null,
        @Query("category") categoria: String? = null
    ): Response<List<FreeToGameDto>>

    @GET("game")
    suspend fun obtenerJuego(@Query("id") id: Int): Response<FreeToGameDto>
}

/** Crea una única instancia Retrofit reutilizable durante la ejecución de la app. */
object FreeToGameClient {
    private const val BASE_URL = "https://www.freetogame.com/api/"

    val service: FreeToGameService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FreeToGameService::class.java)
    }
}
