package com.dojan.infiernoperfecto.entidades;

import java.util.List;

import com.dojan.infiernoperfecto.ataques.Ataque;
import com.dojan.infiernoperfecto.ataques.efectos.EfectoSecundario;
import com.dojan.infiernoperfecto.entidades.clases.Clase;
import com.dojan.infiernoperfecto.entidades.estados.EstadoAlterado;
import com.dojan.infiernoperfecto.entidades.estados.EstadoEstadisticaModificada;
import com.dojan.infiernoperfecto.items.ItemCura;
import com.dojan.infiernoperfecto.utiles.Random;

public abstract class Personaje {

    protected String nombre;
    protected Clase clase;
    protected float vidaBase;
    protected float vidaActual;
    protected int danioBase;
    protected int defensaBase;
    protected List<Ataque> ataques;
    protected int feBase;
    protected int feActual;
    protected int monedasBase;
    protected int monedasActual;
    private EstadoAlterado estadoAlterado;

    // con monedas para los jugadores
    public Personaje(String nombre, int vida, int danio, int defensa, int monedas,List<Ataque> ataques) {
        this.nombre = nombre;
        this.vidaBase = vida;
        this.vidaActual = vida;
        this.danioBase = danio;
        this.defensaBase = defensa;
        this.monedasBase = monedas;
        this.monedasActual = monedas;
        this.ataques = ataques;
        this.feBase = 0;
        this.feActual = 0;
    }

    // sin monedas para los enemigos
    public Personaje(String nombre, int vida, int danio, int defensa, List<Ataque> ataques) {
        this.nombre = nombre;
        this.vidaBase = vida;
        this.vidaActual = vida;
        this.danioBase = danio;
        this.defensaBase = defensa;
        this.ataques = ataques;
        this.feBase = 0;
        this.feActual = 0;
    }

    public ResultadoAtaque atacar(Personaje objetivo, int ataqueElegido){
        int porcentajeReduccionAtaque = 0;
        int porcentajeReduccionDefensa = 0;
        int porcentajeReduccionPrecision = 0;

        if(this.estadoAlterado != null && this.estadoAlterado instanceof EstadoEstadisticaModificada){
            EstadoEstadisticaModificada estado = (EstadoEstadisticaModificada)this.estadoAlterado;
            switch (estado.getTipoEstadistica()) {
                case DANIO:
                    porcentajeReduccionAtaque = estado.getPorcentaje();
                    break;
                case DEFENSA:
                    porcentajeReduccionDefensa = estado.getPorcentaje();
                    break;
                case PRECISION:
                    porcentajeReduccionPrecision = estado.getPorcentaje();
                    break;
            }
            estado.mostrarInformacion();
        }


        int precisionOriginal = ataques.get(ataqueElegido).getProbabilidadAcierto();
        int precisionModificada = precisionOriginal - porcentajeReduccionPrecision;
        precisionModificada = Math.max(0, Math.min(100, precisionModificada));

        boolean acierta = Random.verificarAcierto(precisionModificada);
        if (acierta) {
            System.out.println(this.nombre + " ataca a " + objetivo.getNombre() + " con " + ataques.get(ataqueElegido).getNombre());
        } else {
            System.out.println(this.nombre + " falla el ataque a " + objetivo.getNombre());
        }

        if(ataques.get(ataqueElegido).getCantUsos()<=0){
            System.out.println("Te quedaste sin usos");
        }else{
            // Verificar costo de Fe antes de ejecutar el ataque
            Ataque ataque = ataques.get(ataqueElegido);
            int costoFe = ataque.getCostoFe();
            if (costoFe > 0) {
                if (this.feActual < costoFe) {
                    System.out.println("No tienes suficiente Fe para usar " + ataque.getNombre());
                    return ResultadoAtaque.empty(); // ataque no realizado
                } else {
                    // Restar la Fe correspondiente al atacante
                    this.feActual -= costoFe;
                    if (this.feActual < 0) this.feActual = 0;
                }
            }

            this.ataques.get(ataqueElegido).restarUso();
            if (acierta){
                float danioBaseAtaque = ataques.get(ataqueElegido).getDanio();
                float danioReducido = danioBaseAtaque * (1 - porcentajeReduccionAtaque / 100.0f);
                float danioHecho = this.danioBase + danioReducido;
                float danioFinal = Math.max(0, danioHecho - objetivo.getDefensaBase());
                objetivo.recibirDanio(danioFinal);
                System.out.println("daño final: "+danioFinal);
                System.out.println("El danio a "+objetivo.getNombre()+" es de "+danioFinal);
                // Capturar mensaje de efecto si existe
                EfectoSecundario efecto = ataques.get(ataqueElegido).getEfectoSecundario();
                String mensajeEfecto = null;
                if(efecto!=null){
                    if(Random.verificarAcierto(efecto.getProbabilidad())){
                        mensajeEfecto = efecto.aplicar(objetivo);
                    }
                }
                return new ResultadoAtaque(danioFinal, mensajeEfecto);
            }else{
                System.out.println("Fallaste el ataque");
            }

        }

        if (this.estadoAlterado != null) {
            this.estadoAlterado.reducirTurno();
            if (this.estadoAlterado.getTurnos() == 0) {
                System.out.println(this.nombre + " ha recuperado su estado normal.");
                this.estadoAlterado = null;
            }
        }

    return ResultadoAtaque.empty();
    }

    public void recibirDanio(float cantidad){
        this.vidaActual -= cantidad;
    }


    public void aplicarEstadoAlterado(EstadoAlterado estadoAlterado) {
        this.estadoAlterado = estadoAlterado;
    }


    public boolean sigueVivo(){
        if (this.vidaActual <= 0){
            return false;
        }else {
            return true;
        }
    }

    public boolean comprar(ItemCura itemSeleccionado){
        if(this.monedasActual < itemSeleccionado.getPrecio()){
            System.out.println("No tienes suficiente dinero");
            return false;
        }else if(this.monedasActual >= itemSeleccionado.getPrecio()){
            System.out.println("Compraste "+itemSeleccionado.getNombre());
            this.monedasActual -= itemSeleccionado.getPrecio();
            if (this.monedasActual < 0){
                this.monedasActual = 0;
            }
            this.vidaActual += itemSeleccionado.getCantidadCura();
            if (this.vidaActual > this.vidaBase) {
                this.vidaActual = this.vidaBase;
            }
            return true;
        }
        return false;
    }

    public String getNombre() {
        return nombre;
    }

    public int getDefensaBase() {
        return defensaBase;
    }

    public List<Ataque> getAtaques() {
        return ataques;
    }

    public Clase getClase() {
        return clase;
    }

    public float getVidaBase() {
        return vidaBase;
    }

    public float getVidaActual() {
        return vidaActual;
    }

    public int getFeActual() {
        return feActual;
    }

    public void setFeBase(int feBase) {
        this.feBase = feBase;
        this.feActual = feBase;
    }

    public int getMonedasBase(){
        return monedasBase;
    }

    public int getMonedasActual(){
        return monedasActual;
    }

    public void setMonedasActuales(int monedas) {
        this.monedasActual += monedas;
    }

    public void setMonedasActual(int monedas) {
        this.monedasActual = monedas;
    }

    public void setVidaActual(float vida) {
        this.vidaActual = vida;
    }

    public void setFeActual(int fe) {
        this.feActual = fe;
    }

    public int getFeMax() {
        return feBase; // Asumiendo que feBase es el máximo
    }
}
