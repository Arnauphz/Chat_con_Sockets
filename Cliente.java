import java.io.*;
import java.net.*;
import java.util.*;

public class Cliente {

    private static String host = "192.168.1.38";
    private static int puerto = 28350;  
    private static int intentos = 5;
    private static int espera = 10;

    public static void main(String[] args) {

        Socket socket = null;
        int intento = 0;

        // Intentar conectar
        while (intento < intentos) {
            try {
                System.out.println("Conectando... (intento " + (intento + 1) + ")");
                socket = new Socket(host, puerto);
                System.out.println("Conectado!");
                break;
            } catch (IOException e) {
                intento++;
                if (intento < intentos) {
                    System.out.println("Reintentando en " + espera + " segundos...");
                    try {
                        Thread.sleep(espera * 1000);
                    } catch (InterruptedException ie) {
                        return;
                    }
                }
            }
        }

        if (socket == null) {
            System.out.println("No se pudo conectar");
            return;
        }

        try {
            BufferedReader entradaUsuario = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter salidaServidor = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader entradaServidor = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Hilo para leer mensajes del servidor
            Thread lectorServidor = new Thread(() -> {
                String mensaje;
                try {
                    while ((mensaje = entradaServidor.readLine()) != null) {
                        System.out.println(mensaje);
                    }
                } catch (IOException e) {
                    System.out.println("Conexión cerrada por el servidor.");
                }
            });

            lectorServidor.start();

            // Enviar mensajes al servidor
            String mensajeUsuario;
            while ((mensajeUsuario = entradaUsuario.readLine()) != null) {
                salidaServidor.println(mensajeUsuario);
                if (mensajeUsuario.equalsIgnoreCase("/abandona")) {
                    break;
                }
            }

            socket.close();
        } catch (IOException e) {
            System.out.println("Error en la comunicación con el servidor.");
        }
    }
}
