import java.io.*;
import java.net.*;
import java.util.*;

public class Cliente {
    private static String host = "192.168.1.38";
    private static int puerto = 12345;
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
                System.out.println("¡Conectado!");
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
            System.out.println("No se pudo conectar después de " + intentos + " intentos");
            return;
        }