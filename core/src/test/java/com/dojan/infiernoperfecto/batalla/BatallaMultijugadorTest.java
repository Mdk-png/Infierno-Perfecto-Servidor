package com.dojan.infiernoperfecto.batalla;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.Before;

import com.dojan.infiernoperfecto.batalla.Batalla;
import com.dojan.infiernoperfecto.entidades.Jugador;
import com.dojan.infiernoperfecto.entidades.Personaje;
import com.dojan.infiernoperfecto.entidades.Enemigo;  // Corregido
import com.dojan.infiernoperfecto.entidades.clases.Peleador;
import com.dojan.infiernoperfecto.entidades.clases.Magico;

/**
 * Test Unitario para Fase 1: Batalla Multijugador
 * 
 * Fase 1.1: Constructor multijugador
 * Fase 1.2: Métodos de turno multijugador
 */
public class BatallaMultijugadorTest {

    private List<Jugador> jugadores;
    private Jugador jugadorUnico;
    private Enemigo enemigo;
    
    @Before
    public void setUp() {
        // Crear jugadores para tests
        jugadores = new ArrayList<>();
        jugadores.add(new Jugador("Jugador1", new Peleador()));
        jugadores.add(new Jugador("Jugador2", new Magico()));
        
        jugadorUnico = new Jugador("Jugador", new Peleador());
    }

    /**
     * Helper: Crear lista de enemigos para tests
     */
    private List<Enemigo> crearEnemigos() {
        List<Enemigo> enemigos = new ArrayList<>();
        enemigos.add(new EnemigoTest("Enemigo1", 100, 10, 15));
        enemigos.add(new EnemigoTest("Enemigo2", 80, 8, 12));
        enemigos.add(new EnemigoTest("Enemigo3", 60, 5, 10));
        return enemigos;
    }

    // ============================================================
    // FASE 1.1: TESTS DE CONSTRUCTOR
    // ============================================================

    @Test
    public void testConstructorMultijugador() {
        List<Enemigo> enemigos = crearEnemigos();
        
        Batalla batalla = new Batalla(jugadores, enemigos);
        
        assertTrue("Batalla debe ser multijugador", batalla.esMultijugador());
        assertNotNull("Lista de jugadores no debe ser null", batalla.getJugadores());
        assertEquals("Debe haber 2 jugadores", 2, batalla.getJugadores().size());
        assertNull("Jugador único debe ser null en modo multijugador", batalla.getJugador());
    }

    @Test
    public void testConstructorUnJugadorNoSeRompe() {
        List<Enemigo> enemigos = crearEnemigos();
        
        Batalla batalla = new Batalla(jugadorUnico, enemigos);
        
        assertFalse("Batalla NO debe ser multijugador", batalla.esMultijugador());
        assertNotNull("Jugador único no debe ser null", batalla.getJugador());
        assertNull("Lista de jugadores debe ser null en modo 1 jugador", batalla.getJugadores());
    }

    @Test
    public void testEsMultijugadorRetornaTrue() {
        Batalla batalla = new Batalla(jugadores, crearEnemigos());
        assertTrue("esMultijugador() debe retornar true", batalla.esMultijugador());
    }

    @Test
    public void testEsMultijugadorRetornaFalse() {
        Batalla batalla = new Batalla(jugadorUnico, crearEnemigos());
        assertFalse("esMultijugador() debe retornar false", batalla.esMultijugador());
    }

    @Test
    public void testGettersMultijugador() {
        List<Enemigo> enemigos = crearEnemigos();
        Batalla batalla = new Batalla(jugadores, enemigos);
        
        assertEquals("getJugadores() debe retornar la lista correcta", 
                     jugadores, batalla.getJugadores());
        assertEquals("getEnemigos() debe retornar la lista correcta", 
                     enemigos, batalla.getEnemigos());
    }

    @Test
    public void testTurnoInicialEsCero() {
        Batalla batalla = new Batalla(jugadores, crearEnemigos());
        assertEquals("Turno inicial debe ser 0", 0, batalla.getTurno());
    }

    // ============================================================
    // FASE 1.2: TESTS DE MÉTODOS DE TURNO MULTIJUGADOR
    // ============================================================

    @Test
    public void testAvanzarTurnoMultijugadorExiste() {
        Batalla batalla = new Batalla(jugadores, crearEnemigos());
        
        // Verificar que el método existe y puede ser llamado
        // (con enemigos vacíos no hará mucho, pero verifica la firma)
        boolean terminada = batalla.avanzarTurnoMultijugador(0, 0, 0, 0);
        
        // Con lista vacía de enemigos, debería terminar inmediatamente
        assertTrue("Batalla sin enemigos debe terminar", terminada);
    }

    @Test
    public void testLogCombateSeActualiza() {
        Batalla batalla = new Batalla(jugadores, crearEnemigos());
        
        batalla.avanzarTurnoMultijugador(0, 0, 0, 0);
        
        String log = batalla.getLogCombate();
        assertNotNull("Log no debe ser null", log);
    }

    @Test
    public void testEnemigosMuertosListaExiste() {
        Batalla batalla = new Batalla(jugadores, crearEnemigos());
        
        List<Integer> muertos = batalla.getEnemigosMuertosEsteTurno();
        assertNotNull("Lista de enemigos muertos no debe ser null", muertos);
    }
}
