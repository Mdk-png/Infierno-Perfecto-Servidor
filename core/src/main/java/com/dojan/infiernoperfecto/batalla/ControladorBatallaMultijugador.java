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

    // crea la primer batalla, luego se utiliza iniciarNivelStandard para los siguientes niveles
    public void iniciarBatalla(Jugador j1, Jugador j2) {
        System.out.println("ControladorBatalla: Iniciando batalla multijugador...");
        System.out.println("ControladorBatalla: Piso " + pisoActual + ", Nivel " + nivelActual);

        this.jugador1 = j1;
        this.jugador2 = j2;

        // generar enemigos según aleatorios segun piso
        regenerarEnemigos();

        // crear objeto batalla
        List<Jugador> jugadores = Arrays.asList(jugador1, jugador2);
        batalla = new Batalla(jugadores, enemigos);

        //envia los datos de la batalla a los clientes
        enviarDatosIniciales();

        // dar turno a ambos jugadores
        enviarATodos("TU_TURNO");

        System.out.println("ControladorBatalla: Batalla iniciada con 2 jugadores y " + enemigos.size() + " enemigos");
    }

    private void regenerarEnemigos() {
        enemigos.clear();

        if (pisoActual == 5) {
            // PISO 5: TRAICIÓN (SOLO BOSS)
            enemigos.add(new BossFinal());
            System.out.println("¡BOSS FINAL (PISO 5) HA APARECIDO!");
            return;
        }

        if (nivelActual == NIVEL_BOSS) {
            // MINIBOSS (Pisos 1-4)
            Enemigo miniboss = null;
            switch(pisoActual) {
                case 1: miniboss = new MiniBossLimbo(); break;
                case 2: miniboss = new MiniBossFraude(); break;
                case 3: miniboss = new MiniBossCodicia(); break;
                case 4: miniboss = new MiniBossLujuria(); break;
                default: miniboss = new MiniBossLimbo(); break;
            }
            if (miniboss != null) enemigos.add(miniboss);
            System.out.println("¡MINIBOSS DEL PISO " + pisoActual + " HA APARECIDO!");
        } else {
            // Niveles 1-3: Enemigos aleatorios (1-3 enemigos)
            int cantEnemigos = Random.generarEntero(3) + 1;

            for (int i = 0; i < cantEnemigos; i++) {
                int tipoEnemigo = Random.generarEntero(2);

                Enemigo enemigo = null;
                switch(pisoActual) {
                    case 1:
                        enemigo = (tipoEnemigo == 1) ? new EnemigoLimbo1() : new EnemigoLimbo2();
                        break;
                    case 2:
                        enemigo = (tipoEnemigo == 1) ? new EnemigoFraude1() : new EnemigoFraude2();
                        break;
                    case 3:
                        enemigo = (tipoEnemigo == 1) ? new EnemigoCodicia1() : new EnemigoCodicia2();
                        break;
                    case 4:
                        enemigo = (tipoEnemigo == 1) ? new EnemigoLujuria1() : new EnemigoLujuria2();
                        break;
                    case 5:
                        // Boss Final - Solo uno
                        if (i == 0) enemigo = new BossFinal();
                        else enemigo = null; // Evitar generar más de uno si por error se pidieron más
                        break;
                    default:
                         enemigo = (tipoEnemigo == 1) ? new EnemigoLimbo1() : new EnemigoLimbo2();
                         break;
                }

                if (enemigo != null) enemigos.add(enemigo);
            }

            System.out.println("Generados " + cantEnemigos + " enemigos aleatorios para nivel " + nivelActual + " Piso " + pisoActual);
        }
    }

    private void enviarDatosIniciales() {
        // Formato: DATOS_BATALLA:PISO:NIVEL:Enemigo1,Vida1...
        StringBuilder datos = new StringBuilder("DATOS_BATALLA:" + pisoActual + ":" + nivelActual + ",");

        // enviar datos de los enemigos que encuentra en la ArrayList enemigos
        for (int i = 0; i < enemigos.size(); i++) {
            Enemigo e = enemigos.get(i);
            datos.append(e.getNombre()).append(",").append((int)e.getVidaActual());
            if (i < enemigos.size() - 1) {
                datos.append(",");
            }
        }

        // Enviar múltiples veces por redundancia UDP (evitar pérdida de paquete crítico)
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
        enviarInfoJugadores();
    }

    private void enviarInfoJugadores() {
        // INFO_JUGADORES:1:Clase:MaxVida:MaxFe:2:Clase:MaxVida:MaxFe
        StringBuilder sb = new StringBuilder("INFO_JUGADORES");

        sb.append(":1:").append(jugador1.getClase().getNombre())
          .append(":").append((int)jugador1.getVidaBase())
          .append(":").append(jugador1.getFeMax());

        sb.append(":2:").append(jugador2.getClase().getNombre())
          .append(":").append((int)jugador2.getVidaBase())
          .append(":").append(jugador2.getFeMax());

        enviarATodos(sb.toString());
    }

    public void ejecutarTurnoJugadores(int enemigoJ1, int ataqueJ1, int enemigoJ2, int ataqueJ2) {
        System.out.println("ControladorBatalla: Ejecutando turno de jugadores...");

        // ejecutar turno en batalla
        batalla.avanzarTurnoMultijugador(enemigoJ1, ataqueJ1, enemigoJ2, ataqueJ2);
        // enviar log de combate
        enviarLogCombate(batalla.getLogCombate());
        // sincronizar estado (vida,enemigos muertos,etc)
        sincronizarEstado();
        // ejecutar  todos los turnos enemigos
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

    private boolean verificarDerrota() {
        // DERROTA si AMBOS jugadores están muertos (vida <= 0)
        // Ojo: Si permitimos revivir, esta lógica debería ser más compleja.
        // Por ahora: Game Over si mueren todos.
        if (jugador1.getVidaActual() <= 0 && jugador2.getVidaActual() <= 0) {
            System.out.println("ControladorBatalla: ¡Ambos jugadores muertos! DERROTA");
            return true;
        }
        return false;
    }

    private void limpiarEnemigosMuertos() {
        int antesSize = enemigos.size();
        enemigos.removeIf(e -> !e.sigueVivo());
        int despuesSize = enemigos.size();

        System.out.println("ControladorBatalla: Limpieza de enemigos: " + antesSize + " -> " + despuesSize);
    }

    //viene aca desde HiloServidor cuando le llega que el cliente esta listo para el siguiente nivel
    public void clienteListoParaSiguienteNivel() {
        clientesListosParaSiguienteNivel++; // Incrementar contador de clientes listos para siguiente nivel
        System.out.println("Cliente listo para siguiente nivel (" + clientesListosParaSiguienteNivel + "/2)");

        if (clientesListosParaSiguienteNivel >= 2) {
            avanzarNivel(); // Ambos clientes listos, avanzar nivel
        }
    }

    public void clienteListoParaResultados() {
        clientesListosResultados++; // Incrementar contador de clientes listos para resultados
        // Si ambos clientes están listos, continuar con resultados
        System.out.println("Cliente listo para continuar resultados (" + clientesListosResultados + "/2)");

    }

    // una vez que se ejecutaron ambos turnos y los de los enemigos, se envia el log
    // y si ambos jugadores confirman el log, se verifica la victoria y se avanza al siguiente nivel
    public void clienteConfirmoLog(InfoJugador jugador) {
        clientesConfirmaronLog++;
        System.out.println("ControladorBatalla: Cliente confirmó log (" + clientesConfirmaronLog + "/2)");

        if (clientesConfirmaronLog >= 2) {
            System.out.println("ControladorBatalla: Ambos clientes confirmaron log");
            clientesConfirmaronLog = 0;  // Resetear para próximo turno

            // Verificar si hay victoria
            if (verificarVictoria()) {
                System.out.println("ControladorBatalla: Victoria detectada, enviando FIN_BATALLA");
                servidor.enviarATodos("FIN_BATALLA:VICTORIA"); // Avisar victoria
                clientesListosParaSiguienteNivel = 0;
                // cuando reciben esto los clientes envian clienteListoParaSiguienteNivel()
            } else if (verificarDerrota()) {
                System.out.println("ControladorBatalla: Derrota detectada, enviando FIN_BATALLA:DERROTA");
                servidor.enviarATodos("FIN_BATALLA:DERROTA"); // Avisar derrota
                clientesListosParaSiguienteNivel = 0;
            } else { //si no hay victoria ni derrota, se avanza al siguiente turno
                // Limpiar selecciones para el próximo turno
                for (InfoJugador j : servidor.getJugadores()) {
                    j.limpiarSelecciones();
                }

                // Continuar con nuevo turno
                System.out.println("ControladorBatalla: Batalla continúa, enviando TU_TURNO");
                servidor.enviarATodos("TU_TURNO"); // enviar mensaje a todos los clientes
                System.out.println("ControladorBatalla: Nuevo turno de jugadores");
            }
        }
    }

    private void avanzarNivel() {
        // Limpiar enemigos muertos ANTES de avanzar
        limpiarEnemigosMuertos();

        // Si estamos en piso 5 (Boss Final) y terminamos el nivel, ES VICTORIA.
        // El Boss Final es nivel unico (aunque internamente sea 1).
        if (pisoActual >= 5) {
             System.out.println("ControladorBatalla: ¡VICTORIA FINAL! Completaron el Piso 5 (Boss Final)");
             enviarATodos("VICTORIA_FINAL");
             return;
        }

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

            // RESETEAR JUGADORES (Fe, Estados) AL CAMBIAR DE PISO
            resetearJugadoresCambioPiso(jugador1);
            resetearJugadoresCambioPiso(jugador2);

            System.out.println("ControladorBatalla: Completado piso " + (pisoActual-1) + ", avanzando a piso " + pisoActual);
        }

        if (nivelActual == NIVEL_BOSS) {
            System.out.println("========================================");
            System.out.println("NIVEL 4 ALCANZADO (TIENDA/MINIBOSS)");
            System.out.println("Enviando jugadores a la Tienda...");
            System.out.println("========================================");

            enviarATodos("IR_A_TIENDA");

            // Resetear contadores para esperar salida de tienda
            clientesListosParaSiguienteNivel = 0;
        } else {
            // Regenerar enemigos para niveles 1-3
            iniciarNivelStandard();
        }

        System.out.println("ControladorBatalla: ===== FIN AVANCE NIVEL =====");
    }

    // Este es utilizado para iniciar un nivel standard (1-3) que no sea el primero
    private void iniciarNivelStandard() {
        regenerarEnemigos();

        List<Jugador> jugadores = Arrays.asList(jugador1, jugador2); //actualizar jugadores
        batalla = new Batalla(jugadores, enemigos); //actualizar batalla

        enviarDatosIniciales(); //enviar datos iniciales
        enviarATodos("TU_TURNO"); //enviar turno

        for (InfoJugador j : servidor.getJugadores()) {
            j.limpiarSelecciones(); //limpiar selecciones
        }
    }

    public void procesarCompraItem(int numJugador, int costo) {
        Jugador jugador = (numJugador == 1) ? jugador1 : jugador2;

        System.out.println("ControladorBatalla: Jugador " + numJugador + " compra item de valor " + costo);

        if (jugador.getMonedasActual() >= costo) {
            jugador.setMonedasActual(jugador.getMonedasActual() - costo);

            // Validar update
            sincronizarEstadoJugadores();
        }
    }

    // Sobrecarga para soportar stats
    public void procesarCompraItem(int numJugador, int costo, int vidaExtra, int feExtra) {
        Jugador jugador = (numJugador == 1) ? jugador1 : jugador2;
        System.out.println("ControladorBatalla: Jugador " + numJugador + " compra item (" + costo + ", +" + vidaExtra + "HP)");

        if (jugador.getMonedasActual() >= costo) {
            // resetear monedas
            jugador.setMonedasActual(jugador.getMonedasActual() - costo);

            // sumar vida hasta el maximo disponible
            float nuevaVida = jugador.getVidaActual() + vidaExtra;
            if (nuevaVida > jugador.getVidaBase()) nuevaVida = jugador.getVidaBase();
            jugador.setVidaActual(nuevaVida);

            // Fe (si hubiera item de fe, por ahora asumimos solo vida o logica mixta)
            // ItemCura en cliente da vida y fe.
            if (feExtra > 0) {
                 int nuevaFe = jugador.getFeActual() + feExtra;
                 if (nuevaFe > jugador.getFeMax()) nuevaFe = jugador.getFeMax();
                 jugador.setFeActual(nuevaFe);
            }

            // validar update
            sincronizarEstadoJugadores();
        }
    }

    public void procesarSalidaTienda(int numJugador) {
        System.out.println("ControladorBatalla: Jugador " + numJugador + " quiere salir de la tienda.");
        // Reutilizamos el contador de listos
        clientesListosParaSiguienteNivel++; // 1 o 2.

        // Si no están todos, avisar al que ya salió que espere
        if (clientesListosParaSiguienteNivel < 2) {
             // Ya el cliente se pone en modo espera solo.
        } else {
            // Ambos listos -> Iniciar Nivel 4 (MiniBoss)
            System.out.println("ControladorBatalla: Ambos jugadores salieron de tienda. Iniciando MiniBoss.");
            clientesListosParaSiguienteNivel = 0; // Reset para próximo nivel
            iniciarNivelStandard(); // continua el juego
        }
    }

    private void sincronizarEstado() {
        sincronizarEstadoEnemigos();
        sincronizarEstadoJugadores();
        enviarEnemigosMuertos();
    }

    // envia estado de enemigos a los clientes (vida actual)
    private void sincronizarEstadoEnemigos() {
        for (int i = 0; i < enemigos.size(); i++) {
            Enemigo e = enemigos.get(i);
            if (e.sigueVivo()) {
                String msg = String.format(java.util.Locale.US, "ACTUALIZAR_ENEMIGO:%d:%.1f,0", i, e.getVidaActual());
                enviarATodos(msg);
            }
        }
    }

    // envia estado de jugadores a los clientes (vida, fe, monedas)
    private void sincronizarEstadoJugadores() {
        String msg1 = String.format(java.util.Locale.US, "ACTUALIZAR_JUGADOR:1:%.1f:%d:%d",
                                   jugador1.getVidaActual(), jugador1.getFeActual(), jugador1.getMonedasActual());
        String msg2 = String.format(java.util.Locale.US, "ACTUALIZAR_JUGADOR:2:%.1f:%d:%d",
                                   jugador2.getVidaActual(), jugador2.getFeActual(), jugador2.getMonedasActual());
        enviarATodos(msg1);
        enviarATodos(msg2);
    }

    // envia indices de enemigos muertos a los clientes
    private void enviarEnemigosMuertos() {
        List<Integer> muertos = batalla.getEnemigosMuertosEsteTurno();

        // Calcular recompensas por enemigos muertos AHORA
        if (!muertos.isEmpty()) {
            calcularRecompensas(muertos);
        }

        for (Integer indice : muertos) {
            String msg = "ENEMIGO_MUERTO:" + indice;
            enviarATodos(msg);
        }
    }

    private void calcularRecompensas(List<Integer> muertosIndices) {
        int monedasGanadas = 0;
        for (Integer i : muertosIndices) {
            if (i >= 0 && i < enemigos.size()) {
                monedasGanadas += enemigos.get(i).getMonedasBase();
            }
        }

        if (monedasGanadas > 0) {
            jugador1.setMonedasActuales(monedasGanadas);
            jugador2.setMonedasActuales(monedasGanadas);
            System.out.println("ControladorBatalla: Jugadores ganaron " + monedasGanadas + " monedas.");
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

    // envia mensaje a todos los clientes usando el metodo enviarUnicast de HiloServidor 
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

    private void resetearJugadoresCambioPiso(Jugador j) {
        if (j == null) return;
        // Restaurar Fe (Energia) al maximo (base)
        j.setFeActual(j.getFeMax());
        // Limpiar estados alterados (veneno, buff, etc)
        j.aplicarEstadoAlterado(null);
        System.out.println("ControladorBatalla: Reseteados valores de piso para " + j.getNombre() + " (Fe: " + j.getFeActual() + ")");
    }
}
