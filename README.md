ZonaJuegos

Aplicación Android para crear y organizar una colección personal de videojuegos. Está desarrollada con Kotlin, layouts XML y Room. Este documento resume cómo evolucionó desde la versión inicial y cómo está organizada.

Evolución del proyecto

La primera versión era una colección local sencilla: permitía registrar juegos y mostrarlos, con una persistencia de un único juego. Durante el desarrollo se añadieron una pantalla independiente para la biblioteca, fichas detalladas, filtros por género y plataforma, el último juego añadido, estados de progreso, carátulas con cámara o galería, sonidos por plataforma e historial de partidas. He tomado en cuenta las correcciones de la versión anterior del proyecto, para tratar de mejorarlo.

También se planteó consultar portadas y datos de juegos mediante RAWG. Se cambió esa API por FreeToGame porque RAWG requería una clave de acceso y sus condiciones de uso gratuito no encajaban con el objetivo de usar una API sin clave y completamente gratuita. FreeToGame ofrece acceso público sin autenticación, aunque exige atribuir la fuente; la pantalla «Acerca de» incluye un enlace activo a su [documentación](https://www.freetogame.com/api-doc).

Con FreeToGame se incorporó un catálogo de juegos free-to-play con filtros, imágenes y fichas. Retrofit realiza las peticiones, Gson transforma el JSON y Room conserva una copia local para poder consultar los datos ya descargados sin conexión. Se añadió Glide para cargar y cachear las imágenes.

Funcionamiento

- Alta manual: permite guardar título, género, plataforma, tiempo estimado y si se sabe que tiene logros. La carátula puede fotografiarse o elegirse de la galería.
  
  <img width="780" height="1321" alt="Captura de pantalla 2026-11-24 200543" src="https://github.com/user-attachments/assets/2bc7ea88-2ede-48d1-920f-48d3bda1afb1" />


  Al hacer una foto se guarda como portada del juego después de incluirlo a la biblioteca
  
  <img width="779" height="1316" alt="Captura de pantalla 2026-11-24 200601" src="https://github.com/user-attachments/assets/b81a2ae1-f3b4-4675-87d3-962630bd2dbf" />
  <img width="771" height="1308" alt="Captura de pantalla 2026-11-24 200643" src="https://github.com/user-attachments/assets/fa8ed4b2-8cb8-48df-bb59-a966a861b97f" />


- Catálogo online: obtiene juegos desde `GET /games`, permite filtrar por plataforma/categoría y abrir el detalle con `GET /game?id=…`. Desde allí se añade el juego a la colección personal.
  
  <img width="773" height="1311" alt="Captura de pantalla 2026-11-24 200734" src="https://github.com/user-attachments/assets/8801c349-6346-432d-a809-c0e2b42e779f" />


- Colección: muestra los juegos guardados en Room, permite filtrar y abrir la ficha para cambiar su estado o registrar una partida.
  
  <img width="781" height="1319" alt="Captura de pantalla 2026-11-24 200752" src="https://github.com/user-attachments/assets/401db5b6-7260-4d08-bca9-b56c2393d0b9" />


- Recomendación por zona: al solicitarla, pide permiso de ubicación aproximada, intenta obtener una ubicación reciente o una nueva y muestra las coordenadas aproximadas. Si no hay ubicación o partidas cercanas, recomienda al azar un juego de la colección. La ubicación y las partidas se quedan en el dispositivo.
- Modo sin conexión: la biblioteca siempre es local. El catálogo y sus detalles se pueden consultar offline cuando ya se habían guardado en Room; Glide mantiene su propia caché de imágenes.

FreeToGame solo cubre su catálogo de títulos free-to-play; no es una base universal. No proporciona tiempo para completar, número de logros ni tráileres. Esos valores no se inventan: el tiempo y la presencia de logros pueden anotarse manualmente, y el enlace del catálogo abre la ficha del juego, no un tráiler.

Arquitectura y estructura

El flujo principal separa presentación, lógica y datos: Activity → ViewModel → Repository → Room o API. Las Activities gestionan las vistas y la navegación; los ViewModels mantienen el estado de pantalla; los repositorios coordinan las fuentes; el DAO ejecuta las consultas Room y Retrofit accede al servicio remoto. Las operaciones suspendidas evitan bloquear la interfaz.

El código Kotlin vive en `app/src/main/java/com/example/zonajuegos/`:

- `data/entity/`: entidades Room `Videojuego`, `Partida` y `JuegoCatalogo`.
- `data/local/`: `VideojuegoDAO` y `VideojuegoDatabase`, con migraciones hasta la versión 5.
- `data/remote/`: DTO, contrato Retrofit y cliente de FreeToGame.
- `data/repository/`: acceso local/remoto, conversión de modelos y política de caché.
- `ui/`: pantallas de alta, colección, catálogo, detalles, recomendación y acerca de; ViewModels asociados.
- `ui/adapter/`: filas reutilizables para la colección y el catálogo.
- `util/`: carga de imágenes con Glide.
- `app/src/main/res/layout/`: interfaces y tarjetas XML.

Ejecutar

Abre el proyecto en Android Studio, sincroniza Gradle y ejecuta la configuración `app` en un emulador o dispositivo. El catálogo necesita Internet para la primera descarga.

