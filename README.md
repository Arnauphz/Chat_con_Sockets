# Bitácora de desarrollo - Chat cliente-servidor

## Viernes tarde
- Lectura detallada del enunciado del PDF y análisis de los requisitos funcionales (salas recepcion/jiuston, comandos cliente/admin, aforo).
- Comienzo del archivo más ligero: `Cliente.java`. Implementé hasta la mitad - conexión básica con socket al servidor usando la IP 192.168.1.38 y puerto 12345.
- Configuré el sistema de reintentos (5 intentos, 10 segundos espera) con bucles while y Thread.sleep para reconexión automática.

## Sábado mañana
- Terminé `Cliente.java` añadiendo el hilo lector para recibir mensajes del servidor y el bucle principal para enviar comandos desde consola.
- Implementé detección de `/abandona` para cierre limpio del socket. El cliente ya conecta, envía/recibe mensajes básicos y se reconecta si falla.

## Sábado tarde
- Empecé `Servidor.java` y `ManejadorCliente.java` basándome en los ejercicios de clase de sockets multi-cliente con ExecutorService.
- En `Servidor.java` cargué configuración desde `chat.properties` usando `Properties.load()` y creé las salas iniciales recepcion/jiuston.
- Configuré ServerSocket con pool de hilos fijos y la clase interna `Sala` básica (HashSet usuarios, aforo, métodos agregar/quitar).

## Domingo
- Avancé en `ManejadorCliente.java` con autenticación (nombre único, admin/password) y comandos básicos como `/ayuda`, `/quien_soy`, `/salas`.
- Implementé gestión de salas: unión automática (clientes a recepcion, admins a jiuston) y cambio de sala para clientes básicos con validación de aforo.
- **Problema**: Me está complicando el **ConcurrentHashMap** para las salas y clientes compartidos entre hilos. Tengo race conditions cuando varios clientes cambian de sala simultáneamente - unos usuarios aparecen duplicados o desaparecen.
- **Pendiente**: Comandos avanzados admin (`/crea`, `/elimina_forzado`, `/expulsa`), logging de eventos, `/mensaje_todos`, y **sincronización thread-safe** del mapa de salas/clientes con locks o atomic operations. El concepto de concurrencia con mapas compartidos se me atraganta mucho.
