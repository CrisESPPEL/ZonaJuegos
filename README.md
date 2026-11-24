# ZonaJuegos

Aplicación Android educativa para organizar una biblioteca personal de videojuegos. Está desarrollada en Kotlin, usa interfaces XML y conserva sus datos con Room.

## Evolución del proyecto

La versión inicial permitía guardar juegos localmente con información básica. Durante el desarrollo se amplió para incluir carátulas, una biblioteca separada del formulario de alta, fichas de detalle, estados de progreso, filtros, historial de partidas y una recomendación basada en la ubicación aproximada. También se reorganizó el código por responsabilidades.

Al principio se planteó usar RAWG para obtener portadas y datos. Se descartó porque no encajaba con el requisito de una API completamente gratuita y accesible sin gestionar una clave: RAWG requiere una API key y su uso gratuito está sujeto a condiciones. Se eligió [FreeToGame](https://www.freetogame.com/api-doc), cuya API pública no necesita clave y ofrece filtros y fichas de juegos free-to-play. El uso requiere atribuir la fuente, por eso «Acerca de» incluye un enlace activo.

## Funciones principales

- Añadir juegos manualmente con género, plataforma, tiempo estimado y si tienen logros; adjuntar una foto con la cámara o elegir una imagen de la galería.
- Explorar el catálogo online, filtrar por plataforma o categoría, ver miniaturas y detalles, y añadir juegos a la colección.
- Consultar la biblioteca completa, filtrar por género/plataforma, ver el último juego añadido y actualizar el estado: no jugado, jugando, completado, entre otros.
- Registrar partidas. Con permiso de ubicación aproximada, recomendar el juego con más partidas registradas en un radio cercano; si no hay ubicación o partidas próximas, escoger uno al azar de la biblioteca.
- Conservar la colección y las fichas de catálogo ya consultadas para poder acceder a ellas sin conexión.

**Límites del catálogo:** FreeToGame cubre solo su selección de juegos free-to-play, no todos los videojuegos. No proporciona duración para completar, logros ni tráileres. La app no inventa esos datos: el tiempo y la presencia de logros pueden indicarse manualmente; un enlace del catálogo abre la ficha del juego, no un tráiler.

## Cómo funciona

La pantalla de catálogo solicita a FreeToGame el listado (`GET /games`) y los detalles (`GET /game?id=…`) mediante Retrofit. Gson convierte el JSON en modelos de datos. El repositorio guarda las respuestas válidas en Room y el ViewModel publica los cambios a la interfaz. Si la petición falla, se muestra un aviso y se utiliza la copia local cuando existe. Glide carga las miniaturas desde Internet y gestiona su caché de imágenes.

La colección personal y la caché del catálogo se guardan en tablas Room distintas. Las migraciones de la base llegan a la versión 5 y preservan las filas anteriores. La ubicación y el historial de partidas permanecen en el dispositivo; no se envían a FreeToGame ni se realiza seguimiento continuo.

## Estructura del código

Todo el código de la app está en `app/src/main/java/com/example/zonajuegos/`:

- `data/entity/`: modelos persistentes `Videojuego`, `Partida` y `JuegoCatalogo`.
- `data/local/`: `VideojuegoDAO` contiene las consultas; `VideojuegoDatabase` configura Room y sus migraciones.
- `data/remote/`: DTO, endpoints Retrofit y cliente de FreeToGame.
- `data/repository/`: coordina Room y la API, convierte modelos y aplica la estrategia de caché.
- `ui/`: Activities de alta, colección, catálogo, detalles, recomendación y acerca de; ViewModels y estados de pantalla.
- `ui/adapter/`: adaptadores de las listas de colección y catálogo.
- `util/`: carga de imágenes con Glide.
- `app/src/main/res/layout/`: pantallas y tarjetas definidas en XML.
- `docs/`: ficha técnica de FreeToGame y memoria de arquitectura/demostración.

La interfaz se ocupa de mostrar datos y recibir acciones; ViewModels mantienen el estado de pantalla; los repositorios separan el origen de datos; DAO y Retrofit realizan el acceso local y remoto. Las operaciones de red y Room no bloquean el hilo de interfaz.
