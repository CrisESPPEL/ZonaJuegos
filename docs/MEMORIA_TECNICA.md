# Memoria técnica: catálogo, persistencia y modo offline

## Objetivo

Integrar una fuente externa útil y pública en ZonaJuegos, conservar los datos para consultarlos cuando no haya conexión y mantener la interfaz separada de la lógica de datos.

## Flujo de consulta

1. `CatalogoActivity` lee los filtros y muestra el estado del `CatalogoViewModel`.
2. `CatalogoViewModel` activa la consulta del repositorio y observa Room a la vez.
3. `CatalogoRepository` llama a `FreeToGameService` mediante Retrofit; las funciones `suspend` no bloquean el hilo de interfaz.
4. Gson convierte el JSON a `FreeToGameDto`.
5. El repositorio convierte los DTO válidos a `JuegoCatalogo` y los inserta/actualiza con Room.
6. El flujo de Room emite la caché; el ViewModel mantiene los filtros y la Activity redibuja el adaptador.
7. Al seleccionar un juego, se consulta su detalle. Si la red no está disponible, el repositorio devuelve la ficha local cacheada.

## Capas

- **UI (`ui/`)**: captura acciones, observa estados de pantalla y navega entre Activities. No contiene SQL ni configura Retrofit.
- **ViewModel (`CatalogoViewModel`, `BibliotecaViewModel`)**: conserva el estado mientras vive la pantalla, coordina tareas suspendidas y expone flujos de Room.
- **Repository (`data/repository/`)**: punto de coordinación entre fuentes de datos. El repositorio de catálogo decide entre red y caché; el de biblioteca concentra las operaciones locales.
- **Local (`data/local/`)**: `VideojuegoDao` implementa las consultas Room y `VideojuegoDatabase` configura las entidades y migraciones.
- **Remote (`data/remote/`)**: contrato Retrofit, DTO del API y cliente Gson.
- **Imágenes**: Glide carga URL HTTPS y URI local con placeholder y caché propia.

Esta solución usa factories sencillas de ViewModel en lugar de añadir una librería de inyección de dependencias. Es adecuada para el tamaño y objetivo didáctico del proyecto.

## Persistencia y migración

Room contiene dos conceptos separados:

- `videojuegos`: colección personal; conserva estados, carátulas propias y partidas.
- `catalogo_juegos`: caché de las fichas remotas, identificadas por el ID asignado por FreeToGame.

La versión 5 agrega el enlace online a la entidad de biblioteca y crea la tabla de caché. La migración 4→5 no elimina filas anteriores. Las migraciones 1→2→3→4 se mantienen para instalaciones antiguas. Si se cambia una entidad en el futuro, hay que incrementar la versión y añadir la migración correspondiente; no se debe habilitar migración destructiva para datos de colección.

## Estados visibles

El catálogo distingue entre carga en curso, catálogo cargado, catálogo vacío y fallo de red. Si existe caché, un error de red no la oculta. En el detalle también se informa del error, pero se deja disponible el contenido local. La colección propia se puede consultar sin conexión.

## Alcance, limitaciones y privacidad

- FreeToGame solo cubre su catálogo de juegos gratuitos; no se pretende que sustituya a una base universal de juegos.
- La base no da logros, duración de la campaña ni trailers. Se muestran únicamente datos proporcionados por el servicio o introducidos por la persona.
- El botón de una ficha online abre la página del juego; no se presenta como trailer.
- Las coordenadas de partidas son locales. No se envían con las peticiones del catálogo.
- La ubicación se solicita de forma aproximada y solo para una acción iniciada por la persona. Si falta permiso, ubicación reciente o partida cercana, se escoge un juego aleatorio de la colección.

## Demostración recomendada para la entrega

1. Con Internet, abrir «Catálogo online», consultar la lista, filtrar por Windows o navegador y por una categoría válida.
2. Abrir una ficha, comprobar imagen/descripción y añadirla a la biblioteca.
3. Abrir la biblioteca y el detalle local; cambiar el estado y comprobar que se conserva al regresar.
4. Desconectar Internet y reabrir el catálogo/detalle previamente consultados para enseñar la caché Room y de imágenes.
5. Volver a Inicio, abrir recomendación y probar permiso concedido y denegado. Con datos geográficos se muestra el título con más partidas cercanas; en caso contrario se elige uno al azar.
6. Abrir «Acerca de» y mostrar el enlace de atribución al API.

## Hitos de trabajo

- **Hito 1 — API y listado:** catálogo Retrofit/Gson, filtros y estados de carga/error.
- **Hito 2 — detalle y caché:** ficha ampliada, portada y persistencia Room con acceso offline.
- **Hito 3 — arquitectura:** repositorios/ViewModels y flujo de datos desacoplado de las Activities.
- **Hito 4 — mejora funcional:** guardar del catálogo a la biblioteca, ubicación voluntaria y recomendación local.

Los hitos describen la secuencia funcional de la implementación. El proyecto actualizado está en la rama local `branchDatabase`; no se ha creado un commit ni se han enviado cambios al remoto. Si la entrega exige evidencias Git por hito, registrar los cambios en commits separados antes de publicarlos.
