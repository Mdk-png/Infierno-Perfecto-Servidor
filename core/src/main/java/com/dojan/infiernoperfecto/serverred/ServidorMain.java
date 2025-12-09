package com.dojan.infiernoperfecto.serverred;

/**
 * Clase principal para ejecutar el servidor de forma independiente.
 * Puedes ejecutar esto desde otra terminal o en otra máquina.
 */
public class ServidorMain {

    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("   SERVIDOR - INFIERNO PERFECTO  ");
        System.out.println("=================================");
        System.out.println("Iniciando servidor en puerto 6666...");

        HiloServidor servidor = new HiloServidor(); // crea el hilo
        servidor.start(); // inicia el hilo

        System.out.println("Servidor iniciado correctamente.");
        System.out.println("Esperando conexiones de clientes...");
        System.out.println("Presiona CTRL+C para detener el servidor.");

        // Mantener el programa corriendo
        try {
            servidor.join(); // Espera a que el hilo termine
        } catch (InterruptedException e) {
            System.out.println("Servidor interrumpido.");
            servidor.detener(); // Detener el servidor si se interrumpe
        } finally {
            // ✅ Garantizar que el servidor se detiene correctamente
            System.out.println("Deteniendo servidor...");
            servidor.detener(); // Detener el servidor
            try {
                servidor.join(2000);
                System.out.println("Servidor detenido correctamente."); // Confirmación de detención
            } catch (InterruptedException e) {
                System.err.println("Forzando cierre del servidor...");
                Thread.currentThread().interrupt(); // Restaurar el estado de interrupción
            }
        }
    }
}
