package com.dojan.infiernoperfecto.batalla;

import java.util.ArrayList;
import java.util.List;

import com.dojan.infiernoperfecto.entidades.Enemigo;
import com.dojan.infiernoperfecto.entidades.Jugador;
import com.dojan.infiernoperfecto.entidades.Personaje;
import com.dojan.infiernoperfecto.entidades.ResultadoAtaque;
import com.dojan.infiernoperfecto.utiles.Random;

public class Batalla {
    private final Personaje jugador;
    private final List<Jugador> jugadores;  // Para modo multijugador
    private final List<Enemigo> enemigos;
    private int turno = 0;
    private String logCombate = "";
    private final List<Integer> enemigosMuertosEsteturno = new ArrayList<>();

    // Constructor existente (1 jugador)
    public Batalla(Personaje jugador, List<Enemigo> enemigos){
        this.jugador = jugador;
        this.jugadores = null;  // null indica modo 1 jugador
        this.enemigos = enemigos;
    }

    // NUEVO: Constructor para 2 jugadores (multijugador)
    public Batalla(List<Jugador> jugadores, List<Enemigo> enemigos){
        this.jugador = null;  // null indica modo multijugador
        this.jugadores = jugadores;
        this.enemigos = enemigos;
    }

    /*
    Procesa un solo turno de la batalla. Retorna true si la batalla sigue, false si terminó.
    El primer turno (turno==0) es del jugador, los siguientes de los enemigos.
     */
    public boolean avanzarTurno(int opcE, int opcA) {
        enemigosMuertosEsteturno.clear();

        if (turno == 0) {
            turnoJugador(opcE, opcA);
        } else {
            // Ajustar el índice porque pueden haber muerto enemigos
            int indiceEnemigo = turno - 1;
            if (indiceEnemigo < enemigos.size()) {
                Enemigo enemigo = enemigos.get(indiceEnemigo);
                if (enemigo.sigueVivo()) {
                    turnoEnemigo(enemigo);
                }
            }
        }

        turno++;
        // Cuando termina la ronda de enemigos, antes de volver al jugador, verifica si la batalla termino
        if (turno > enemigos.size()) {
            turno = 0;
            return !jugador.sigueVivo() || enemigos.isEmpty();
        }
        // Mientras se ejecutan los turnos enemigos, nunca termina la batalla
        return false;
    }

    public boolean batallaTerminada() {
        return !jugador.sigueVivo() || enemigos.isEmpty();
    }

    private void turnoJugador(int opcE, int opcA) {
        Enemigo objetivo = enemigos.get(opcE);

    ResultadoAtaque resultado = jugador.atacar(objetivo, opcA);
        float danioReal = resultado.getDanio();
        logCombate = "Atacaste a " + objetivo.getNombre() + " he hiciste " + danioReal + " de daño.\n";
        if (resultado.getEfectoMensaje() != null && !resultado.getEfectoMensaje().isEmpty()) {
            logCombate += resultado.getEfectoMensaje() + "\n";
        }

        if(!objetivo.sigueVivo()){
            System.out.println("murio el objetivo: "+objetivo.getNombre());
            enemigosMuertosEsteturno.add(opcE); // ← NUEVO: Guardar el índice en lugar de eliminar
            // NO ELIMINAR AQUÍ: enemigos.remove(objetivo);
        }
    }

    private void turnoEnemigo(Enemigo enemigo){
        int ataqueEnemigo = Random.generarEntero(enemigo.getAtaques().size());
        ResultadoAtaque resultado = enemigo.atacar(jugador, ataqueEnemigo);
        float danioReal = resultado.getDanio();
        String nombreAtaque = "";
        if (enemigo.getAtaques() != null && !enemigo.getAtaques().isEmpty() && ataqueEnemigo >= 0 && ataqueEnemigo < enemigo.getAtaques().size()) {
            nombreAtaque = enemigo.getAtaques().get(ataqueEnemigo).getNombre();
        }
        if (!nombreAtaque.isEmpty()) {
            logCombate += enemigo.getNombre() + " usó " + nombreAtaque + " y te hizo " + danioReal + " de daño.\n";
        } else {
            logCombate += enemigo.getNombre() + " te hizo " + danioReal + " de daño.\n";
        }
        // Si el ataque aplicó un efecto con mensaje, agregarlo en la línea siguiente
        if (resultado.getEfectoMensaje() != null && !resultado.getEfectoMensaje().isEmpty()) {
            logCombate += resultado.getEfectoMensaje() + "\n";
        }
    }

    // ============================================================
    // MÉTODOS PARA MODO MULTIJUGADOR
    // ============================================================

    /**
     * Procesa un turno en modo multijugador (2 jugadores).
     * En turno 0: ambos jugadores atacan simultáneamente.
     * En turnos 1+: cada enemigo ataca a un jugador aleatorio.
     *
     * j1EnemigoIdx Índice del enemigo que ataca jugador 1
     * j1AtaqueIdx Índice del ataque que usa jugador 1
     * j2EnemigoIdx Índice del enemigo que ataca jugador 2
     * j2AtaqueIdx Índice del ataque que usa jugador 2
     * return true si la batalla terminó, false si continúa
     */
    public boolean avanzarTurnoMultijugador(int j1EnemigoIdx, int j1AtaqueIdx, int j2EnemigoIdx, int j2AtaqueIdx) {
        enemigosMuertosEsteturno.clear();
        logCombate = "";

        if (turno == 0) {
            // Turno de AMBOS jugadores simultáneamente
            turnoJugadorMulti(jugadores.get(0), j1EnemigoIdx, j1AtaqueIdx, 1);
            turnoJugadorMulti(jugadores.get(1), j2EnemigoIdx, j2AtaqueIdx, 2);
        } else {
            // Turnos de enemigos (igual que modo 1 jugador)
            int indiceEnemigo = turno - 1;
            if (indiceEnemigo < enemigos.size()) {
                Enemigo enemigo = enemigos.get(indiceEnemigo);
                if (enemigo.sigueVivo()) {
                    turnoEnemigoMulti(enemigo);
                }
            }
        }

        turno++;

        // Cuando termina la ronda de enemigos, volver a turno 0
        if (turno > enemigos.size()) {
            turno = 0;
            return batallaTerminadaMulti();
        }

        return false;
    }

    /**
     * Procesa el turno de un jugador en modo multijugador.
     */
    private void turnoJugadorMulti(Jugador jugador, int opcE, int opcA, int numJugador) {
        Enemigo objetivo = enemigos.get(opcE);

        // ejecutar ataque
        ResultadoAtaque resultado = jugador.atacar(objetivo, opcA);
        float danioReal = resultado.getDanio();

        // Registrar en el log
        logCombate += "Jugador " + numJugador + " atacó a " + objetivo.getNombre() +
                      " e hizo " + danioReal + " de daño.\n";

        if (resultado.getEfectoMensaje() != null && !resultado.getEfectoMensaje().isEmpty()) {
            logCombate += resultado.getEfectoMensaje() + "\n";
        }

        // si murio guarda el indice
        if (!objetivo.sigueVivo()) {
            System.out.println("Murió el objetivo: " + objetivo.getNombre());
            enemigosMuertosEsteturno.add(opcE);
        }
    }

    /**
     * Procesa el turno de un enemigo en modo multijugador.
     * El enemigo elige aleatoriamente a qué jugador atacar.
     */
    private void turnoEnemigoMulti(Enemigo enemigo) {
        // Elegir jugador aleatorio para atacar (solo entre los vivos)
        List<Integer> jugadoresVivos = new ArrayList<>();
        for (int i = 0; i < jugadores.size(); i++) {
            if (jugadores.get(i).sigueVivo()) {
                jugadoresVivos.add(i);
            }
        }

        // Si no hasy jugadores vivo, no atacar
        if (jugadoresVivos.isEmpty()) {
            return;
        }

        // Elegir jugador aleatorio de los vivos
        int indiceAleatorio = Random.generarEntero(jugadoresVivos.size());
        int jugadorObjetivo = jugadoresVivos.get(indiceAleatorio);
        System.out.println("DEBUG: Enemigo " + enemigo.getNombre() + " ataca a Jugador " + (jugadorObjetivo+1) + " (De " + jugadoresVivos.size() + " vivos)");
        Jugador jugador = jugadores.get(jugadorObjetivo);

        int ataqueEnemigo = Random.generarEntero(enemigo.getAtaques().size());
        ResultadoAtaque resultado = enemigo.atacar(jugador, ataqueEnemigo);
        float danioReal = resultado.getDanio();

        String nombreAtaque = "";
        if (enemigo.getAtaques() != null && !enemigo.getAtaques().isEmpty() &&
            ataqueEnemigo >= 0 && ataqueEnemigo < enemigo.getAtaques().size()) {
            nombreAtaque = enemigo.getAtaques().get(ataqueEnemigo).getNombre();
        }

        if (!nombreAtaque.isEmpty()) {
            logCombate += enemigo.getNombre() + " usó " + nombreAtaque +
                          " e hizo " + danioReal + " de daño a Jugador " + (jugadorObjetivo + 1) + ".\n";
        } else {
            logCombate += enemigo.getNombre() + " hizo " + danioReal +
                          " de daño a Jugador " + (jugadorObjetivo + 1) + ".\n";
        }

        if (resultado.getEfectoMensaje() != null && !resultado.getEfectoMensaje().isEmpty()) {
            logCombate += resultado.getEfectoMensaje() + "\n";
        }
    }

    /**
     * Verifica si la batalla multijugador ha terminado.
     * Termina si todos los jugadores murieron O si todos los enemigos murieron.
     */
    private boolean batallaTerminadaMulti() {
        boolean algunJugadorVivo = false;
        for (Jugador j : jugadores) {
            if (j.sigueVivo()) {
                algunJugadorVivo = true;
                break;
            }
        }
        return !algunJugadorVivo || enemigos.isEmpty();
    }

    public String getLogCombate() {
        return logCombate;
    }

    public int getTurno() {
        return turno;
    }

    // ← NUEVO: Método para obtener los enemigos que murieron este turno
    public List<Integer> getEnemigosMuertosEsteTurno() {
        return new ArrayList<>(enemigosMuertosEsteturno);
    }

    public Personaje getJugador() {
        return jugador;
    }

    public List<Enemigo> getEnemigos() {
        return enemigos;
    }

    public List<Integer> getEnemigosMuertosEsteturno() {
        return enemigosMuertosEsteturno;
    }

    // NUEVO: Getters para modo multijugador
    public List<Jugador> getJugadores() {
        return jugadores;
    }

    public boolean esMultijugador() {
        return jugadores != null;
    }
}
