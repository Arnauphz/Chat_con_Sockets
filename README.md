# Diario de Desarrollo - Chat Cliente-Servidor
**Proyecto**: Sistema de chat multiusuario con arquitectura cliente-servidor  
**Asignatura**: Programación de Servicios y Procesos  

---

## Viernes 

### Análisis inicial
- ✅ Lectura detallada del enunciado del PDF
- ✅ Análisis de requisitos funcionales:
  - Sistema de salas (recepcion/jiuston)
  - Comandos diferenciados para cliente básico y administrador
  - Sistema de aforo configurable
  - Autenticación con contraseña para admins
  
### Implementación Cliente.java (Primera parte)
- ✅ Comencé por el archivo más simple: `Cliente.java`
- ✅ Implementada conexión básica mediante Socket:
  - IP: 192.168.1.38
  - Puerto: 28350 (cambiado del inicial 12345)
- ✅ Sistema de reintentos automáticos:
  - Máximo 5 intentos
  - Espera de 10 segundos entre intentos
  - Implementado con bucle `while` y `Thread.sleep()`
  
**Estado al final del día**: Cliente conecta al servidor pero sin comunicación bidireccional aún.

---

## Sábado

### Cliente.java (Completado)
- Implementado **hilo lector** para recibir mensajes del servidor:
  - Uso de `Thread` para lectura asíncrona
  - `BufferedReader` para entrada desde servidor
  - Bucle infinito que imprime mensajes recibidos
  
- Implementado **bucle principal** para enviar comandos:
  - Lectura desde consola (`System.in`)
  - Envío mediante `PrintWriter` al servidor
  
- Comando `/abandona`:
  - Detección con `equalsIgnoreCase()`
  - Cierre limpio del socket
  
**Estado al final**: Cliente completamente funcional para conectar, enviar y recibir mensajes.


### Inicio del servidor
- Creación de `Servidor.java`:
  - Basado en ejemplos de clase sobre sockets multi-cliente
  - `ExecutorService` con pool de hilos fijos (configurable)
  - `ServerSocket` escuchando en puerto 28350
  
### Sistema de configuración
- Archivo `chat.properties` creado con:
  ```properties
  servidor.ip=192.168.1.38
  servidor.puerto=28350
  servidor.numero.maximo.threads=30
  sala.aforo.defecto=5
  sala.aforo.maximo=30
  admin.password=admin123
  ```
  
- Carga de configuración con `Properties.load()`:
  - Método `cargarConfiguracion()` en Servidor.java
  - Valores por defecto si falla la carga
  
### Gestión de salas inicial
- Creación de salas por defecto en `crearSalasIniciales()`:
  - Sala "recepcion" para usuarios básicos
  - Sala "jiuston" para administradores
  
- Estructuras de datos:
  - `ConcurrentHashMap<String, Integer> aforos` - Capacidad de cada sala
  - `ConcurrentHashMap<String, Set<String>> salasUsers` - Usuarios por sala
  - `ConcurrentHashMap<String, ManejadorCliente> clientes` - Clientes conectados
  
### ManejadorCliente.java (Primera parte)
- Clase implementa `Runnable` para ejecución en hilos
- Constructor recibe `Socket` del cliente
- Estructura básica del método `run()`

**Estado al final**: Servidor acepta conexiones y las delega a ManejadorCliente.

---

## Domingo

### Sistema de autenticación
- Método `autenticar()` en ManejadorCliente:
  - Solicitud de nombre de usuario
  - Validación de nombre único (no duplicados)
  - Pregunta sobre rol (básico/admin)
  - Si es admin: solicitud y validación de contraseña
  - Registro en `Servidor.clientes`
  
### Comandos básicos implementados
- `/ayuda` - Muestra comandos disponibles según rol
- `/quiensoy` - Información del usuario actual
- `/salas` - Lista salas disponibles
- `/usuarios <sala>` - Lista usuarios en una sala específica

### Gestión de salas
- Unión automática según rol:
  - Usuarios básicos → sala "recepcion"
  - Administradores → sala "jiuston"
  
- Comando `/unirse <sala>` para usuarios básicos:
  - Validación de existencia de sala
  - Validación de aforo disponible
  - Actualización de mapas de usuarios
  
### Comandos de administrador (parcial)
- `/crea <sala> [aforo]` - Crear nuevas salas
- `/elimina <sala>` - Eliminar salas vacías
- `/cambiaaforo <sala> <aforo>` - Modificar capacidad
- `/infosala <sala>` - Información de sala
- `/infousuario <usuario>` - Información de usuario

### Sistema de logging
- Archivo `chat.log` para registro de eventos:
  - Conexiones/desconexiones
  - Creación/eliminación de salas
  - Expulsiones
- Método `logEvent()` con timestamp automático

### Problemas encontrados y soluciones pendientes

#### Problema 1: ConcurrentHashMap y race conditions
**Descripción**: Cuando varios clientes cambian de sala simultáneamente, aparecen usuarios duplicados o desaparecen.

**Causa identificada**: Aunque uso `ConcurrentHashMap`, las operaciones de agregar/quitar no son atómicas cuando involucran múltiples mapas.

**Solución aplicada**: 
- Uso de `synchronized` en métodos críticos:
  - `agregarUserSala()`
  - `quitarUserSala()`
  - `registrarCliente()`
  - `desconectarCliente()`

#### ⚠️ Problema 2: Comandos avanzados pendientes
**Pendiente de implementar**:
- `/eliminaforzado <sala>` - Eliminar sala moviendo usuarios a recepcion
- `/expulsa <usuario> [motivo]` - Expulsar usuarios
- `/mensajetodos <msg>` - Broadcast global (solo admin)
- `/apaga` - Apagar servidor

**Estado**: Lógica parcialmente implementada, falta testing exhaustivo.

---

## 📅 Lunes - Correcciones finales

### Bug crítico: Contraseña de admin no funciona

**Problema descubierto**: Al intentar autenticar como admin, siempre devuelve "Contraseña incorrecta" aunque se introduzca la correcta.

**Investigación**:
1. Añadido `System.out.println()` para debug de la contraseña
2. Descubierto que la contraseña tiene espacios invisibles
3. Problema: `Properties.getProperty()` no elimina espacios en blanco

**Solución implementada**:
```java
// En Servidor.java - cargarConfiguracion()
passwordAdmin = props.getProperty("admin.password", "admin123").trim();
ip = props.getProperty("servidor.ip", "127.0.0.1").trim();
// ... aplicado a todas las propiedades String

// En ManejadorCliente.java - autenticar()
String respuesta = entrada.readLine();
if (respuesta != null) respuesta = respuesta.trim();

String pass = entrada.readLine();
if (pass != null) pass = pass.trim();
```

**Resultado**: Autenticación funcionando correctamente

### Comandos avanzados completados
- `/eliminaforzado <sala>` - Con validación de aforo en recepcion
- `/expulsa <usuario> [motivo]` - Con protección para admins
- `/mensajetodos <msg>` - Broadcast global
- `/apaga` - Cierre ordenado del servidor

**Estado al final del día**: Funcionalidad core completada, pendiente testing exhaustivo.

---

## Miércoles (Día 6) - Testing y refinamiento

### Pruebas de funcionalidad básica
- **Pruebas de conexión**:
  - Verificado sistema de reintentos (5 intentos, 10 segundos)
  - Confirmado que cliente se reconecta automáticamente
  - Validado mensaje de fallo tras agotar intentos

- **Pruebas de autenticación**:
  - Login usuario básico → OK (entra a "recepcion")
  - Login admin con password correcta → OK (entra a "jiuston")
  - Login admin con password incorrecta → OK (rechazado)
  - Nombres duplicados → OK (segundo cliente rechazado)
  - Nombres vacíos/espacios → OK (validación funciona)

### Pruebas de comandos de usuario
- **Comandos informativos**:
  - `/ayuda` muestra correctamente comandos según rol
  - `/quiensoy` devuelve info completa con tiempo conectado
  - `/salas` lista solo salas visibles (jiuston oculta para básicos)
  - `/usuarios <sala>` lista correctamente usuarios

- **Navegación entre salas**:
  - `/unirse <sala>` funciona correctamente
  - Validación de aforo → sala llena rechaza entrada
  - Usuario permanece en sala original si falla el cambio
  - Admins NO pueden cambiar de sala (validado)

- **Sistema de mensajería**:
  - Mensajes de sala llegan solo a usuarios en misma sala
  - `/mensaje <texto> <usuario>` envía mensaje privado OK
  - `/ping <usuario>` detecta correctamente si está en línea
  - Usuario no recibe sus propios mensajes

### Bugs encontrados durante testing

#### Bug menor 1: Formato de tiempo en /quiensoy
**Problema**: El tiempo conectado mostraba formato raro (PT1H30M45S)

**Solución**: Mejorado el formateo del Duration:
```java
String tiempo = Duration.between(tiempoConexion, LocalDateTime.now()).toString()
    .substring(2).replaceAll("([0-9][0-9]):", "H $1 M ").replace("M ", "M ");
```

#### Bug menor 2: Comando /mensaje con sintaxis incorrecta
**Problema**: No validaba correctamente cuando faltaban parámetros

**Solución**: Añadida validación:
```java
if (partes.length < 2) {
    salida.println("Falta mensaje");
    return;
}
```

### Pruebas de comandos admin
- `/crea <sala>` crea salas correctamente
- `/crea <sala> <aforo>` respeta aforo especificado
- Aforo máximo (30) validado correctamente
- `/elimina <sala>` solo elimina salas vacías
- Protección de salas recepcion/jiuston funcionando
- `/cambiaaforo <sala> <aforo>` validaciones OK
- `/infosala` y `/infousuario` muestran info correcta

**Estado al final del día**: Mayoría de funcionalidades validadas, algunos bugs menores corregidos.

---

## Jueves - Testing avanzado y documentación

### Pruebas de concurrencia
- **Múltiples usuarios simultáneos**:
  - Conectados 5 clientes simultáneamente
  - Todos envían mensajes a la vez
  - No hay pérdida de mensajes ✓
  - No hay duplicados ✓
  - Los `synchronized` funcionan correctamente

- **Race conditions en salas**:
  - 3 usuarios intentan unirse simultáneamente a sala con aforo 2
  - Solo 2 entran, el 3º recibe mensaje de sala llena ✓
  - No hay usuarios fantasma ni duplicados ✓
  - El `ConcurrentHashMap` + `synchronized` resuelve el problema inicial

- **Registro simultáneo mismo nombre**:
  - 2 clientes intentan conectarse como "Duplicado"
  - Solo uno tiene éxito ✓
  - El segundo recibe "nombre ya en uso" ✓

### Pruebas de comandos admin avanzados
- **Expulsión de usuarios**:
  - `/expulsa <usuario>` desconecta correctamente
  - `/expulsa <usuario> <motivo>` envía motivo al expulsado
  - No se puede expulsar a administradores (protección OK)
  - Evento registrado en chat.log

- **Eliminación forzada de salas**:
  - `/eliminaforzado <sala>` mueve usuarios a recepcion
  - Validación de espacio en recepcion funciona
  - Si no caben, la sala NO se elimina (protección OK)
  - Usuarios movidos reciben notificación

- **Broadcast global**:
  - `/mensajetodos <msg>` llega a TODOS los usuarios
  - Funciona incluso entre diferentes salas
  - Formato "ADMIN <nombre>: <mensaje>" correcto

- **Apagado de servidor**:
  - `/apaga` cierra correctamente todos los clientes
  - Mensaje de aviso enviado a todos
  - Servidor termina limpiamente con System.exit(0)
  - Evento registrado en chat.log

### Sistema de logging verificado
- Archivo `chat.log` se crea automáticamente
- Eventos registrados:
  ```
  2025-01-31T14:23:15... - Conexión: Carlos (básico)
  2025-01-31T14:23:45... - Sala creada: gaming
  2025-01-31T14:24:12... - Aforo cambiado gaming: 8
  2025-01-31T14:25:30... - Usuario expulsado: Molesto - Spam
  2025-01-31T14:26:05... - Desconexión: Carlos
  ```
- Timestamps correctos con LocalDateTime
- Append mode (true) mantiene historial

### Pruebas de límites y robustez
- **Comandos malformados**:
  - `/usuarios` (sin sala) → mensaje de error apropiado
  - `/unirse` (sin sala) → error con sintaxis correcta
  - `/ping` (sin usuario) → uso correcto mostrado

- **Valores inválidos**:
  - Aforo 0 o negativo → rechazado
  - Aforo mayor que máximo (30) → ajustado a 30
  - Cambiar aforo bajo número actual usuarios → rechazado

- **Desconexiones abruptas**:
  - Cliente cerrado con Ctrl+C → servidor maneja OK
  - Socket cerrado sin /abandona → limpieza correcta
  - Usuario eliminado de sala automáticamente
  - No quedan usuarios "zombie"

### Documentación final
- **Plan de pruebas creado**:
  - 70+ casos de prueba documentados
  - Organizados en 9 categorías
  - Pasos detallados y resultados esperados
  - Checklist de verificación incluido

- **Diario de desarrollo**:
  - Documentado proceso día a día
  - Problemas encontrados y soluciones
  - Aprendizajes clave identificados
  - Estadísticas del proyecto

- ✅ **Código comentado**:
  - Métodos críticos con JavaDoc
  - Explicaciones en secciones complejas
  - Validaciones documentadas

### Ajustes finales de código
- ✅ Eliminados `System.out.println()` de debug
- ✅ Mensajes de usuario mejorados (más claros)
- ✅ Validación de entrada más robusta
- ✅ Manejo de excepciones refinado
- ✅ Código formateado y limpio

### Testing final exhaustivo
- ✅ Ejecutado plan de pruebas completo
- ✅ Todas las pruebas pasadas satisfactoriamente
- ✅ No quedan bugs conocidos