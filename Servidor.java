import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class Servidor {
    private static String ip = "192.168.1.38";
    private static int puerto = 28350;
    private static int maxThreads = 100;
    private static int aforoDefecto = 5;
    private static int aforoMaximo = 10;
    private static String passwordAdmin = "admin123";
    private static Map<String, Integer> aforos = new ConcurrentHashMap<>();
    private static Map<String, Set<String>> salasUsers = new ConcurrentHashMap<>();
    private static Map<String, ManejadorCliente> clientes = new ConcurrentHashMap<>();
    private static Map<String, String> userSala = new ConcurrentHashMap<>();
    private static ExecutorService poolHilos;
    private static PrintWriter log;

    public static void main(String[] args) {
        cargarConfiguracion();
        crearSalasIniciales();
        iniciarLogging();
        poolHilos = Executors.newFixedThreadPool(maxThreads);
        try (ServerSocket servidor = new ServerSocket(puerto)) {
            System.out.println("Servidor iniciado en " + ip + ":" + puerto);
            while (true) {
                Socket socketCliente = servidor.accept();
                System.out.println("Nuevo cliente: " + socketCliente.getInetAddress());
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
            ip = props.getProperty("servidor.ip", "127.0.0.1").trim();
            puerto = Integer.parseInt(props.getProperty("servidor.puerto", "12345").trim());
            maxThreads = Integer.parseInt(props.getProperty("servidor.numero.maximo.threads", "100").trim());
            aforoDefecto = Integer.parseInt(props.getProperty("sala.aforo.defecto", "5").trim());
            aforoMaximo = Integer.parseInt(props.getProperty("sala.aforo.maximo", "10").trim());
            passwordAdmin = props.getProperty("admin.password", "admin123").trim();
        } catch (IOException e) {
            System.err.println("Error chat.properties, usando defaults");
        }
        
        System.out.println("DEBUG - Password admin: '" + passwordAdmin + "'");
    }

    private static void crearSalasIniciales() {
        aforos.put("recepcion", aforoDefecto);
        salasUsers.put("recepcion", ConcurrentHashMap.newKeySet());
        aforos.put("jiuston", aforoDefecto);
        salasUsers.put("jiuston", ConcurrentHashMap.newKeySet());
        System.out.println("Salas creadas: recepcion y jiuston");
    }

    private static void iniciarLogging() {
        try {
            log = new PrintWriter(new FileWriter("chat.log", true), true);
        } catch (IOException e) {
            System.err.println("Error iniciando log");
        }
    }

    public static synchronized void logEvent(String evento) {
        if (log != null) {
            log.println(LocalDateTime.now() + " - " + evento);
            log.flush();
        }
    }

    public static synchronized boolean registrarCliente(String nombre, ManejadorCliente manejador) {
        if (clientes.containsKey(nombre)) return false;
        clientes.put(nombre, manejador);
        logEvent("Conexión: " + nombre + " (" + (manejador.esAdmin() ? "admin" : "básico") + ")");
        return true;
    }

    public static synchronized void desconectarCliente(String nombre) {
        ManejadorCliente mc = clientes.remove(nombre);
        if (mc != null) {
            String sala = mc.getSalaActual();
            if (sala != null) {
                Set<String> users = salasUsers.get(sala);
                if (users != null) users.remove(nombre);
            }
            userSala.remove(nombre);
            logEvent("Desconexión: " + nombre);
        }
    }

    // GETTERS
    public static Map<String, Integer> getAforos() { return aforos; }
    public static Map<String, Set<String>> getSalasUsers() { return salasUsers; }
    public static Map<String, ManejadorCliente> getClientes() { return clientes; }
    public static String getPasswordAdmin() { return passwordAdmin; }
    public static int getAforoMaximo() { return aforoMaximo; }
    public static int getAforoDefecto() { return aforoDefecto; }
    public static Map<String, String> getUserSala() { return userSala; }

    // SALAS
    public static synchronized boolean agregarUserSala(String user, String sala) {
        if (!aforos.containsKey(sala)) return false;
        Set<String> users = salasUsers.get(sala);
        if (users.size() >= aforos.get(sala)) return false;
        users.add(user);
        userSala.put(user, sala);
        return true;
    }

    public static synchronized boolean quitarUserSala(String user) {
        String sala = userSala.get(user);
        if (sala == null) return false;
        Set<String> users = salasUsers.get(sala);
        if (users != null) users.remove(user);
        userSala.remove(user);
        return true;
    }

    public static synchronized boolean crearSala(String nombre, int aforo) {
        if (aforos.containsKey(nombre) || "recepcion".equals(nombre) || "jiuston".equals(nombre)) 
            return false;
        aforos.put(nombre, Math.min(aforo, aforoMaximo));
        salasUsers.put(nombre, ConcurrentHashMap.newKeySet());
        logEvent("Sala creada: " + nombre);
        return true;
    }

    public static synchronized boolean eliminarSala(String nombre) {
        if (!aforos.containsKey(nombre) || salasUsers.get(nombre).size() > 0 || 
            "recepcion".equals(nombre) || "jiuston".equals(nombre)) return false;
        aforos.remove(nombre);
        salasUsers.remove(nombre);
        logEvent("Sala eliminada: " + nombre);
        return true;
    }

    public static synchronized boolean eliminarSalaForzada(String nombre) {
        if (!aforos.containsKey(nombre) || "recepcion".equals(nombre) || "jiuston".equals(nombre)) 
            return false;
        Set<String> users = salasUsers.get(nombre);
        Set<String> recepcionUsers = salasUsers.get("recepcion");
        if (recepcionUsers.size() + users.size() > aforos.get("recepcion")) return false;
        
        for (String user : users) {
            recepcionUsers.add(user);
            userSala.put(user, "recepcion");
        }
        aforos.remove(nombre);
        salasUsers.remove(nombre);
        logEvent("Sala eliminada forzada: " + nombre);
        return true;
    }

    public static synchronized boolean cambiarAforo(String nombre, int nuevoAforo) {
        if (!aforos.containsKey(nombre) || nuevoAforo <= 0 || nuevoAforo > aforoMaximo || 
            nuevoAforo < salasUsers.get(nombre).size()) return false;
        aforos.put(nombre, nuevoAforo);
        logEvent("Aforo cambiado " + nombre + ": " + nuevoAforo);
        return true;
    }

    public static synchronized void expulsarUsuario(String usuario, String motivo) {
        ManejadorCliente mc = clientes.get(usuario);
        if (mc != null && !mc.esAdmin()) {
            mc.enviarMensaje("Expulsado: " + (motivo != null ? motivo : "Sin motivo"));
            mc.desconectar();
            logEvent("Usuario expulsado: " + usuario + (motivo != null ? " - " + motivo : ""));
        }
    }

    public static synchronized void apagarServidor() {
        logEvent("Servidor apagado por admin");
        for (ManejadorCliente mc : clientes.values()) {
            mc.enviarMensaje("Servidor apagado por administrador.");
        }
        poolHilos.shutdownNow();
        System.exit(0);
    }
}