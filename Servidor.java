import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Servidor {
    private static String ip = "192.168.1.38";
    private static int puerto = 28350;
    private static int maxThreads = 100;
    private static int aforoDefecto = 5;
    private static int aforoMaximo = 10;
    private static String passwordAdmin = "admin123";

    private static Map<String, Sala> salas = new ConcurrentHashMap<>();
    private static Map<String, ManejadorCliente> clientes = new ConcurrentHashMap<>();
    private static ExecutorService poolHilos;

    public static void main(String[] args) {
        cargarConfiguracion();
        crearSalasIniciales();

        poolHilos = Executors.newFixedThreadPool(maxThreads);
        try (ServerSocket servidor = new ServerSocket(puerto)) {
            System.out.println("Servidor iniciado en " + ip + ":" + puerto);
            while (true) {
                Socket socketCliente = servidor.accept();
                System.out.println("Nuevo cliente conectado: " + socketCliente.getInetAddress());
                poolHilos.execute(new ManejadorCliente(socketCliente));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void cargarConfiguracion() {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("chat.properties"));
            ip = props.getProperty("servidor.ip", "127.0.0.1");
            puerto = Integer.parseInt(props.getProperty("servidor.puerto", "12345"));
            maxThreads = Integer.parseInt(props.getProperty("servidor.numero.maximo.threads", "100"));
            aforoDefecto = Integer.parseInt(props.getProperty("sala.aforo.defecto", "5"));
            aforoMaximo = Integer.parseInt(props.getProperty("sala.aforo.maximo", "10"));
            passwordAdmin = props.getProperty("admin.password", "admin123");
        } catch (IOException e) {
            System.err.println("Error cargando chat.properties, usando valores por defecto");
        }
    }

    private static void crearSalasIniciales() {
        salas.put("recepcion", new Sala("recepcion", aforoDefecto));
        salas.put("jiuston", new Sala("jiuston", aforoDefecto));
        System.out.println("Salas creadas: recepcion y jiuston");
    }