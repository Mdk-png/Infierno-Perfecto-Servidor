package com.dojan.infiernoperfecto.serverred;

import java.net.InetAddress;

/**
 * Clase que representa la información de un jugador conectado al servidor.
 * Extiende DireccionRed agregando datos específicos del jugador para batalla multijugador.
 */
public class InfoJugador {
    private DireccionRed direccion;
    private int numeroJugador;          // 1 o 2 (orden de conexión)
    private String nombreJugador;
    private int claseId;                // ID de la clase seleccionada
    
    // Selecciones de turno actual
    private Integer enemigoSeleccionado = null;
    private Integer ataqueSeleccionado = null;
    
    /**
     * ip = Dirección IP del cliente
     * puerto = Puerto del cliente
     * numeroJugador = Número asignado (1 o 2)
     */
    public InfoJugador(InetAddress ip, int puerto, int numeroJugador) {
        this.direccion = new DireccionRed(ip, puerto);
        this.numeroJugador = numeroJugador;
    }
    
    /**
     * Verifica si el jugador ha completado su selección de turno
     * return true si seleccionó enemigo Y ataque
     */
    public boolean tieneSeleccionCompleta() {
        return enemigoSeleccionado != null && ataqueSeleccionado != null;
    }
    
    /**
     * Limpia las selecciones del turno actual
     * Se llama después de ejecutar el turno
     */
    public void limpiarSelecciones() {
        enemigoSeleccionado = null;
        ataqueSeleccionado = null;
    }
    
    public DireccionRed getDireccion() {
        return direccion;
    }
    
    public int getNumeroJugador() {
        return numeroJugador;
    }
    
    public void setNumeroJugador(int numeroJugador) {
        this.numeroJugador = numeroJugador;
    }
    
    public String getNombreJugador() {
        return nombreJugador;
    }
    
    public void setNombreJugador(String nombreJugador) {
        this.nombreJugador = nombreJugador;
    }
    
    public int getClaseId() {
        return claseId;
    }
    
    public void setClaseId(int claseId) {
        this.claseId = claseId;
    }
    
    public Integer getEnemigoSeleccionado() {
        return enemigoSeleccionado;
    }
    
    public void setEnemigoSeleccionado(Integer enemigoSeleccionado) {
        this.enemigoSeleccionado = enemigoSeleccionado;
    }
    
    public Integer getAtaqueSeleccionado() {
        return ataqueSeleccionado;
    }
    
    public void setAtaqueSeleccionado(Integer ataqueSeleccionado) {
        this.ataqueSeleccionado = ataqueSeleccionado;
    }
}
