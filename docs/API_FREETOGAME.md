# Ficha técnica de API: FreeToGame

## Propósito

FreeToGame aporta un catálogo remoto de videojuegos free-to-play. La app lo usa para mostrar títulos, géneros, plataformas, descripciones, fechas, desarrolladores y miniaturas; quien quiera puede guardar una ficha en su colección local.

## Acceso y atribución

- URL base: `https://www.freetogame.com/api/`.
- Formato: JSON sobre HTTPS.
- Autenticación: no requiere cuenta ni clave.
- Uso: la documentación del servicio declara uso gratuito y requiere atribución mediante enlace activo a FreeToGame.
- Límites: recomienda no hacer más de 10 peticiones por segundo. La aplicación consulta al pulsar Buscar y al abrir un detalle; no hace sondeo periódico.
- Alcance: el servicio cubre su catálogo de juegos free-to-play; los resultados no abarcan todos los juegos comerciales o físicos.

Fuente: [documentación oficial de FreeToGame](https://www.freetogame.com/api-doc). La pantalla «Acerca de» contiene un enlace activo a esa documentación y aclara que los juegos/imagenes pertenecen a sus titulares.

## Endpoints usados

| Petición | Uso en ZonaJuegos | Respuesta relevante |
| --- | --- | --- |
| `GET /games` | Consultar el listado y sus miniaturas | `id`, `title`, `short_description`, `genre`, `platform`, `thumbnail` y URL de juego |
| `GET /games?platform=windows` | Filtrar el catálogo para Windows/PC | El mismo modelo de lista |
| `GET /games?platform=browser` | Filtrar juegos de navegador | El mismo modelo de lista |
| `GET /games?category={categoria}` | Filtrar por género/categoría del API | El mismo modelo de lista |
| `GET /game?id={id}` | Abrir la ficha ampliada | Descripción, desarrollador, editor, fecha, capturas y requisitos cuando existen |

FreeToGame publica sus categorías como etiquetas, por ejemplo `shooter` o `mmorpg`. El campo libre de la pantalla se envía tal como se escribe, en minúsculas; para coincidencias sin conexión también se aplica el filtro localmente a las fichas cacheadas.

## Conversión de datos

`FreeToGameDto` describe el JSON remoto y `JuegoCatalogo` es la entidad Room que representa una ficha cacheada. `CatalogoRepository` convierte DTO → entidad, omite registros inválidos y serializa capturas/requisitos opcionales a JSON. Al guardar desde el catálogo, convierte los datos necesarios a `Videojuego`, la entidad de la colección personal.

Las imágenes se cargan con Glide desde `thumbnail`. La URL se conserva en Room y Glide administra su caché en disco/memoria; las imágenes descargadas previamente pueden continuar visibles sin una nueva petición.

## Manejo de errores y modo sin conexión

- HTTP distinto de éxito se convierte en un error presentado por la pantalla.
- La lista se observa desde Room mientras la consulta remota se ejecuta.
- Si la lista remota falla, se conservan las filas previamente almacenadas y se presenta un aviso.
- El detalle intenta actualizarse desde Internet; si falla, abre la copia de Room.
- Un juego solo se puede guardar desde el catálogo cuando su ficha está en Room, de modo que una selección offline siga siendo coherente.
- No hay caché HTTP explícita de OkHttp: la caché de datos de dominio es Room y la de imágenes es Glide.

## Datos que el API no proporciona para esta app

El catálogo no es una fuente para el número de logros, el tiempo para completar ni los tráilers. La app no debe mostrar valores inventados: esos datos se indican manualmente si se conocen; de lo contrario permanecen vacíos. La URL de detalle es una ficha del juego, no un enlace garantizado a vídeo.
