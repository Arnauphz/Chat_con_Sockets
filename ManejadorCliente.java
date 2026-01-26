import java.io.*;
import java.net.*;

public class ManejadorCliente implements Runnable {
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String nombre;
    private boolean esAdmin;
    private String salaActual;

    public ManejadorCliente(Socket socket) {
        this.socket = socket;
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

        salida.println("¿Eres administrador? (si/no):");
        String respuesta = entrada.readLine();
        if ("si".equalsIgnoreCase(respuesta)) {
            salida.println("Contraseña:");
            String pass = entrada.readLine();
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
        String[] partes = comando.split(" ", 2);
        String cmd = partes[0];
        
        switch (cmd) {
            case "/ayuda":
                mostrarAyuda();
                break;
            case "/quien_soy":
                salida.println("Nombre: " + nombre + ", Tipo: " + (esAdmin ? "Admin" : "Cliente") + 
                             ", Sala: " + salaActual);
                break;
            case "/salas":
                listarSalas();
                break;
            case "/usuarios":
                if (partes.length > 1) {
                    listarUsuarios(partes[1]);
                } else {
                    salida.println("Uso: /usuarios <sala>");
                }
                break;
            case "/unirse":
                if (!esAdmin && partes.length > 1) {
                    cambiarSala(partes[1]);
                } else {
                    salida.println("Los admins no pueden cambiar de sala");
                }
                break;
            case "/abandona":
                salida.println("¡Hasta luego " + nombre + "!");
                desconectar();
                break;
            default:
                salida.println("Comando desconocido. Usa /ayuda");
        }
    }