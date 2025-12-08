package com.dojan.infiernoperfecto.batalla;

import com.dojan.infiernoperfecto.entidades.Enemigo;
import com.dojan.infiernoperfecto.entidades.Jugador;
import com.dojan.infiernoperfecto.entidades.clases.*;
import com.dojan.infiernoperfecto.entidades.enemigos.*;
import com.dojan.infiernoperfecto.serverred.HiloServidor;
import com.dojan.infiernoperfecto.serverred.InfoJugador;
import com.dojan.infiernoperfecto.utiles.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ControladorBatallaMultijugador {
    
    private HiloServidor servidor;
    private Batalla batalla;
    private Jugador jugador1;
    private Jugador jugador2;
    private List<Enemigo> enemigos;
    
    private boolean jugador1Listo = false;
    private boolean jugador2Listo = false;
    
    private int nivelActual;
    private int pisoActual;
    private int clientesListosParaSiguienteNivel = 0;
    private int clientesListosResultados = 0;
    private int clientesConfirmaronLog = 0;  // Contador de confirmaciones de log
    
    private static final int NIVEL_TIENDA = 3;
    private static final int NIVEL_BOSS = 4;
    private static final int NIVEL_MAX = 4;
    private static final int PISO_MAX = 5;
    
    public ControladorBatallaMultijugador(HiloServidor servidor, int piso, int nivel) {
        this.servidor = servidor;
        this.pisoActual = piso;
        this.nivelActual = nivel;
        this.enemigos = new ArrayList<>();
    }
    
    public void iniciarBatalla(Jugador j1, Jugador j2) {
        System.out.println("ControladorBatalla: Iniciando batalla multijugador...");
        System.out.println("ControladorBatalla: Piso " + pisoActual + ", Nivel " + nivelActual);
        
        this.jugador1 = j1;
        this.jugador2 = j2;
        
        regenerarEnemigos();
        
        List<Jugador> jugadores = Arrays.asList(jugador1, jugador2);
        batalla = new Batalla(jugadores, enemigos);
        
        enviarDatosIniciales();
        enviarATodos("TU_TURNO");
        
        System.out.println("ControladorBatalla: Batalla iniciada con 2 jugadores y " + enemigos.size() + " enemigos");
    }
    
    private void regenerarEnemigos() {
        enemigos.clear();
        
        if (nivelActual == NIVEL_BOSS) {
            // MINIBOSS (igual que modo un jugador)
            Enemigo miniboss = new MiniBossLimbo();
            enemigos.add(miniboss);
            System.out.println("¡MINIBOSS DEL PISO " + pisoActual + " HA APARECIDO!");
        } else {
            // Niveles 1-3: Enemigos aleatorios (1-3 enemigos)
            int cantEnemigos = Random.generarEntero(3) + 1;
            
            for (int i = 0; i < cantEnemigos; i++) {
                int tipoEnemigo = Random.generarEntero(2);
                
                Enemigo enemigo;
                if (tipoEnemigo == 1) {
                    enemigo = new EnemigoLimbo1();
                } else {
                    enemigo = new EnemigoLimbo2();
                }
                enemigos.add(enemigo);
            }
            
            System.out.println("Generados " + cantEnemigos + " enemigos aleatorios para nivel " + nivelActual);
        }
    }
    
    private void enviarDatosIniciales() {
        StringBuilder datos = new StringBuilder("DATOS_BATALLA:" + nivelActual + ",");
        
        for (int i = 0; i < enemigos.size(); i++) {
            Enemigo e = enemigos.get(i);
            datos.append(e.getNombre()).append(",").append((int)e.getVidaActual());
            if (i < enemigos.size() - 1) {
                datos.append(",");
            }
        }
        
        // FASE 5: Enviar múltiples veces por redundancia UDP (evitar pérdida de paquete crítico)
        final String mensajeDatos = datos.toString();
        new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                enviarATodos(mensajeDatos);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        sincronizarEstadoJugadores();
    }
    
    public void ejecutarTurnoJugadores(int enemigoJ1, int ataqueJ1, int enemigoJ2, int ataqueJ2) {
        System.out.println("ControladorBatalla: Ejecutando turno de jugadores...");
        
        batalla.avanzarTurnoMultijugador(enemigoJ1, ataqueJ1, enemigoJ2, ataqueJ2);
        enviarLogCombate(batalla.getLogCombate());
        sincronizarEstado();
        ejecutarTurnosEnemigos();
    }
    
    private void ejecutarTurnosEnemigos() {
        System.out.println("ControladorBatalla: Ejecutando turnos de enemigos...");
        
        while (batalla.getTurno() != 0) {
            batalla.avanzarTurnoMultijugador(0, 0, 0, 0);
            
            if (!batalla.getLogCombate().isEmpty()) {
                enviarLogCombate(batalla.getLogCombate());
            }
            
            sincronizarEstado();
        }
        
        // NO enviar TU_TURNO ni FIN_BATALLA aquí
        // Esperar a que ambos jugadores confirmen el log
        System.out.println("ControladorBatalla: Esperando confirmación de log de ambos jugadores...");
    }
    
    private boolean verificarVictoria() {
        // IMPORTANTE: NO eliminar enemigos aquí, solo verificar
        // Los índices deben mantenerse estables durante el turno
        for (Enemigo e : enemigos) {
            if (e.sigueVivo()) {
                return false;
            }
        }
        
        System.out.println("ControladorBatalla: ¡Todos los enemigos muertos! VICTORIA");
        return true;
    }
    
    private void limpiarEnemigosMuertos() {
        int antesSize = enemigos.size();
        enemigos.removeIf(e -> !e.sigueVivo());
        int despuesSize = enemigos.size();
        
        System.out.println("ControladorBatalla: Limpieza de enemigos: " + antesSize + " -> " + despuesSize);
    }
    
    public void clienteListoParaSiguienteNivel() {
        clientesListosParaSiguienteNivel++;
        System.out.println("Cliente listo para siguiente nivel (" + clientesListosParaSiguienteNivel + "/2)");
        
        if (clientesListosParaSiguienteNivel >= 2) {
            avanzarNivel();
        }
    }

    public void clienteListoParaResultados() {
        clientesListosResultados++;
        System.out.println("Cliente listo para continuar resultados (" + clientesListosResultados + "/2)");
        
    }

    public void clienteConfirmoLog(InfoJugador jugador) {
        clientesConfirmaronLog++;
        System.out.println("ControladorBatalla: Cliente confirmó log (" + clientesConfirmaronLog + "/2)");
        
        if (clientesConfirmaronLog >= 2) {
            System.out.println("ControladorBatalla: Ambos clientes confirmaron log");
            clientesConfirmaronLog = 0;  // Resetear para próximo turno
            
            // Verificar si hay victoria
            if (verificarVictoria()) {
                System.out.println("ControladorBatalla: Victoria detectada, enviando FIN_BATALLA");
                servidor.enviarATodos("FIN_BATALLA:VICTORIA");
                clientesListosParaSiguienteNivel = 0;
            } else {
                // Limpiar selecciones para el próximo turno
                for (InfoJugador j : servidor.getJugadores()) {
                    j.limpiarSelecciones();
                }
                
                // Continuar con nuevo turno
                System.out.println("ControladorBatalla: Batalla continúa, enviando TU_TURNO");
                servidor.enviarATodos("TU_TURNO");
                System.out.println("ControladorBatalla: Nuevo turno de jugadores");
            }
        }
    }
    
    private void avanzarNivel() {
        // Limpiar enemigos muertos ANTES de avanzar
        limpiarEnemigosMuertos();
        
        nivelActual++;
        System.out.println("ControladorBatalla: ===== AVANZANDO NIVEL =====");
        System.out.println("ControladorBatalla: Nivel anterior: " + (nivelActual - 1));
        System.out.println("ControladorBatalla: Nivel nuevo: " + nivelActual);
        
        if (nivelActual > NIVEL_MAX) {
            pisoActual++;
            nivelActual = 1;
            
            if (pisoActual > PISO_MAX) {
                System.out.println("ControladorBatalla: ¡VICTORIA FINAL! Completaron todos los pisos");
                enviarATodos("VICTORIA_FINAL");
                return;
            }
            
            System.out.println("ControladorBatalla: Completado piso " + (pisoActual-1) + ", avanzando a piso " + pisoActual);
        }
        
        // TEMPORAL: Cerrar juego cuando llegue a la tienda (nivel 4)
        // TODO: Implementar sincronización de tienda en el futuro
        if (nivelActual == NIVEL_BOSS) {
            System.out.println("========================================");
            System.out.println("NIVEL 4 ALCANZADO (TIENDA/MINIBOSS)");
            System.out.println("========================================");
            System.out.println("CERRANDO JUEGO - La tienda no está implementada aún");
            System.out.println("Por favor, reinicien el juego para probar los niveles 1-3 nuevamente");
            System.out.println("========================================");
            
            enviarATodos("MENSAJE:¡Nivel 4 alcanzado! Cerrando juego (tienda no implementada)");
            
            // Esperar un momento para que el mensaje llegue
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // Cerrar el servidor
            System.exit(0);
        } else {
            // Regenerar enemigos para niveles 1-3
            regenerarEnemigos();
            
            List<Jugador> jugadores = Arrays.asList(jugador1, jugador2);
            batalla = new Batalla(jugadores, enemigos);
            
            enviarDatosIniciales();
            enviarATodos("TU_TURNO");
            
            for (InfoJugador j : servidor.getJugadores()) {
                j.limpiarSelecciones();
            }
        }
        
        System.out.println("ControladorBatalla: ===== FIN AVANCE NIVEL =====");
    }
    
    private void sincronizarEstado() {
        sincronizarEstadoEnemigos();
        sincronizarEstadoJugadores();
        enviarEnemigosMuertos();
    }
    
    private void sincronizarEstadoEnemigos() {
        for (int i = 0; i < enemigos.size(); i++) {
            Enemigo e = enemigos.get(i);
            if (e.sigueVivo()) {
                String msg = String.format(java.util.Locale.US, "ACTUALIZAR_ENEMIGO:%d:%.1f,0", i, e.getVidaActual());
                enviarATodos(msg);
            }
        }
    }
    
    private void sincronizarEstadoJugadores() {
        String msg1 = String.format(java.util.Locale.US, "ACTUALIZAR_JUGADOR:1:%.1f:%d", 
                                   jugador1.getVidaActual(), jugador1.getFeActual());
        String msg2 = String.format(java.util.Locale.US, "ACTUALIZAR_JUGADOR:2:%.1f:%d", 
                                   jugador2.getVidaActual(), jugador2.getFeActual());
        enviarATodos(msg1);
        enviarATodos(msg2);
    }
    
    private void enviarEnemigosMuertos() {
        List<Integer> muertos = batalla.getEnemigosMuertosEsteTurno();
        for (Integer indice : muertos) {
            String msg = "ENEMIGO_MUERTO:" + indice;
            enviarATodos(msg);
        }
    }
    
    private void enviarLogCombate(String log) {
        if (log != null && !log.isEmpty()) {
            String[] lineas = log.split("\\n");
            for (String linea : lineas) {
                if (!linea.trim().isEmpty()) {
                    enviarATodos("LOG_BATALLA:" + linea.trim());
                }
            }
        }
    }
    
    private void enviarATodos(String mensaje) {
        for (InfoJugador jugador : servidor.getJugadores()) {
            servidor.enviarUnicast(mensaje, 
                jugador.getDireccion().getIp(), 
                jugador.getDireccion().getPuerto());
        }
    }
    
    public Jugador getJugador1() { return jugador1; }
    public Jugador getJugador2() { return jugador2; }
    public int getNivelActual() { return nivelActual; }
    public int getPisoActual() { return pisoActual; }
}