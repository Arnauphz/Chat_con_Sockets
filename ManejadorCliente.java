import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.time.Duration;

public class ManejadorCliente implements Runnable {
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String nombre;
    private boolean esAdmin;
    private String salaActual;
    private LocalDateTime tiempoConexion;

    public ManejadorCliente(Socket socket) {
        this.socket = socket;
        this.tiempoConexion = LocalDateTime.now();
    }

    @Override
    public void run() {
        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            if (!autenticar()) {
                socket.close();
                return;
            }

            // Unir a sala inicial
            if (esAdmin) {
                unirseASala("jiuston");
            } else {
                unirseASala("recepcion");
            }

            salida.println("¡Bienvenido " + nombre + "! Escribe /ayuda para comandos");

            // Bucle de comandos
            String linea;
            while ((linea = entrada.readLine()) != null) {
                procesarComando(linea);
            }
        } catch (IOException e) {
            System.out.println("Error con cliente: " + e.getMessage());
        } finally {
            desconectar();
        }
    }

    private boolean autenticar() throws IOException {
        salida.println("Nombre de usuario:");
        nombre = entrada.readLine();
        if (nombre == null || nombre.trim().isEmpty()) {
            salida.println("Nombre inválido");
            return false;
        }
        nombre = nombre.trim();

        salida.println("¿Eres administrador? (si/no):");
        String respuesta = entrada.readLine();
        if (respuesta != null) respuesta = respuesta.trim();
        if ("si".equalsIgnoreCase(respuesta)) {
            salida.println("Contraseña:");
            String pass = entrada.readLine();
            if (pass != null) pass = pass.trim();
            if (!Servidor.getPasswordAdmin().equals(pass)) {
                salida.println("Contraseña incorrecta");
                return false;
            }
            esAdmin = true;
        }

        // Registrar en servidor
        if (!Servidor.registrarCliente(nombre, this)) {
            salida.println("Ese nombre ya está en uso");
            return false;
        }

        return true;
    }

    private void procesarComando(String comando) {
    if (comando.startsWith("/")) {
        String[] partes = comando.substring(1).split(" ", 3);
        String cmd = partes[0].toLowerCase();

        switch (cmd) {
            case "ayuda": mostrarAyuda(); break;
            case "quiensoy": mostrarInfoUsuario(); break;
            case "salas": listarSalas(); break;
            case "usuarios":
                if (partes.length > 1) listarUsuarios(partes[1]);
                else salida.println("Uso: /usuarios <sala>");
                break;
            case "unirse":
                if (!esAdmin && partes.length > 1) cambiarSala(partes[1]);
                else salida.println("Los admins no pueden cambiar de sala");
                break;
            case "ping":
                if (partes.length > 1) pingUsuario(partes[1]);
                else salida.println("Uso: /ping <usuario>");
                break;
            case "mensaje": enviarMensaje(partes); break;
            case "abandona": 
                salida.println("¡Hasta luego " + nombre + "!");
                desconectar(); 
                break;
            // ADMIN
            case "crea":
                if (esAdmin && partes.length > 1) {
                    int aforo = partes.length > 2 ? Integer.parseInt(partes[2]) : Servidor.getAforoDefecto();
                    if (Servidor.crearSala(partes[1], aforo))
                        salida.println("Sala creada: " + partes[1]);
                    else salida.println("No se pudo crear sala");
                } else salida.println("Solo admins");
                break;
            case "elimina":
                if (esAdmin && partes.length > 1) {
                    if (Servidor.eliminarSala(partes[1]))
                        salida.println("Sala eliminada");
                    else salida.println("No se puede eliminar");
                }
                break;
            case "eliminaforzado":
                if (esAdmin && partes.length > 1) {
                    if (Servidor.eliminarSalaForzada(partes[1]))
                        salida.println("Sala eliminada forzada");
                    else salida.println("No se puede");
                }
                break;
            case "cambiaaforo":
                if (esAdmin && partes.length > 2) {
                    int aforo = Integer.parseInt(partes[2]);
                    if (Servidor.cambiarAforo(partes[1], aforo))
                        salida.println("Aforo cambiado");
                    else salida.println("Error aforo");
                } else salida.println("Uso: /cambiaaforo <sala> <aforo>");
                break;
            case "expulsa":
                if (esAdmin && partes.length > 1) {
                    Servidor.expulsarUsuario(partes[1], partes.length > 2 ? partes[2] : null);
                    salida.println("Usuario expulsado");
                } else salida.println("Uso: /expulsa <user> [motivo]");
                break;
            case "infosala":
                if (partes.length > 1) infoSala(partes[1]);
                else salida.println("Uso: /infosala <sala>");
                break;
            case "infousuario":
                if (partes.length > 1) infoUsuario(partes[1]);
                else salida.println("Uso: /infousuario <usuario>");
                break;
            case "mensajetodos":
                if (esAdmin && partes.length > 1) {
                    for (ManejadorCliente mc : Servidor.getClientes().values())
                        mc.enviarMensaje("ADMIN " + nombre + ": " + partes[1]);
                    salida.println("Enviado a todos");
                } else salida.println("Solo admins");
                break;
            case "apaga":
                if (esAdmin) Servidor.apagarServidor();
                break;
            default: salida.println("Comando desconocido. /ayuda");
        }
    } else {
        enviarMensajeSala(comando);
    }
}



    private void mostrarAyuda() {
        if (esAdmin) {
            salida.println("Comandos admin: /ayuda /quiensoy /salas /usuarios <sala> /crea <sala> [aforo] /elimina <sala> /eliminaforzado <sala> /cambiaaforo <sala> <aforo> /expulsa <user> [motivo] /infosala <sala> /infousuario <user> /mensajetodos <msg> /apaga");
        } else {
            salida.println("Comandos: /ayuda /quiensoy /salas /usuarios <sala> /unirse <sala> /ping <user> /abandona");
        }
    }

    private void mostrarInfoUsuario() {
        String tiempo = Duration.between(tiempoConexion, LocalDateTime.now()).toString()
                .substring(2).replaceAll("([0-9][0-9]):", "H $1 M ").replace("M ", "M ");
        salida.println("Nombre: " + nombre + ", Tipo: " + (esAdmin ? "Admin" : "Cliente") +
                      ", Sala: " + salaActual + ", Tiempo: " + tiempo);
    }

    private void infoUsuario(String nombreUser) {
    ManejadorCliente mc = Servidor.getClientes().get(nombreUser);
        if (mc == null) {
            salida.println("Usuario no existe");
            return;
        }
        salida.println(nombreUser + " - Sala: " + mc.getSalaActual());
    }

    private void listarSalas() {
        for (String sala : Servidor.getAforos().keySet()) {
            if (!esAdmin && "jiuston".equals(sala)) continue;
            java.util.Set<String> users = Servidor.getSalasUsers().get(sala);
            int numUsers = users != null ? users.size() : 0;
            salida.println(sala + " (" + numUsers + "/" + Servidor.getAforos().get(sala) + ")");
        }
    }

    private void infoSala(String nombreSala) {
        if (!Servidor.getAforos().containsKey(nombreSala)) {
            salida.println("Sala no existe");
            return;
        }
            java.util.Set<String> users = Servidor.getSalasUsers().get(nombreSala);
            int numUsers = users != null ? users.size() : 0;
            int aforo = Servidor.getAforos().get(nombreSala);
            salida.println(nombreSala + " - " + numUsers + "/" + aforo + " usuarios");
    }

    private void listarUsuarios(String sala) {
        Set<String> users = Servidor.getSalasUsers().get(sala);
        if (users == null) {
            salida.println("Sala no existe");
            return;
        }
        salida.println("Usuarios en " + sala + ":");
        for (String u : users) {
            salida.println(" - " + u);
        }
    }

    private void cambiarSala(String nuevaSala) {
        Servidor.quitarUserSala(nombre);
        if (Servidor.agregarUserSala(nombre, nuevaSala)) {
            salaActual = nuevaSala;
            salida.println("Cambiado a sala: " + nuevaSala);
        } else {
            Servidor.agregarUserSala(nombre, salaActual); // Revertir
            salida.println("No se puede unir (sala llena o no existe)");
        }
    }

    private void unirseASala(String sala) {
        Servidor.agregarUserSala(nombre, sala);
        salaActual = sala;
    }

    private void pingUsuario(String usuario) {
        String salaUser = Servidor.getUserSala().get(usuario);
        if (salaUser == null) {
            salida.println("Usuario no existe");
        } else if (!salaUser.equals(salaActual)) {
            salida.println(usuario + " no está en tu sala");
        } else {
            salida.println(usuario + " está en línea");
        }
    }

    private void enviarMensaje(String[] partes) {
        if (partes.length < 2) {
            salida.println("Falta mensaje");
            return;
        }
        String mensaje = partes[1];
        if (partes.length > 2) {
            String destino = partes[2];
            ManejadorCliente destMc = Servidor.getClientes().get(destino);
            if (destMc != null) {
                if (esAdmin || Servidor.getUserSala().get(destino).equals(salaActual)) {
                    destMc.enviarMensaje(nombre + ": " + mensaje);
                    salida.println("Enviado a " + destino);
                } else {
                    salida.println("Usuario no en tu sala");
                }
            } else {
                salida.println("Usuario no existe");
            }
        } else {
            enviarMensajeSala(mensaje);
        }
    }

    private void enviarMensajeSala(String mensaje) {
        java.util.Set<String> users = Servidor.getSalasUsers().get(salaActual);
        if (users != null) {
            for (String user : users) {
                if (!user.equals(nombre)) {
                    Servidor.getClientes().get(user).enviarMensaje(nombre + ": " + mensaje);
                }
            }
        }
    }

    public void enviarMensaje(String msg) {
        salida.println(msg);
    }

    public void desconectar() {
        Servidor.desconectarCliente(nombre);
        try {
            socket.close();
        } catch (IOException e) {
            // Ignorar
        }
    }

    // GETTERS para Servidor
    public boolean esAdmin() { return esAdmin; }
    public String getSalaActual() { return salaActual; }
}