package com.dojan.infiernoperfecto.pantallas.batalla;

import com.dojan.infiernoperfecto.batalla.Batalla;
import com.dojan.infiernoperfecto.comandos.ComandoAtacar;
import com.dojan.infiernoperfecto.entidades.Enemigo;
import com.dojan.infiernoperfecto.entidades.Personaje;
import com.dojan.infiernoperfecto.logica.ResultadoCombate;
import com.dojan.infiernoperfecto.logica.ServicioCombate;

public class ControladorBatallaLocal {
    private ServicioCombate servicioCombate;
    private Batalla batalla;

    public ControladorBatallaLocal(Batalla batalla) {
        this.batalla = batalla;
        this.servicioCombate = new ServicioCombate();
    }

    public ResultadoCombate procesarComando(ComandoAtacar comando) {
        Enemigo objetivo = batalla.getEnemigos().get(comando.getEnemigoIndex());
        Personaje jugador = batalla.getJugador();

        return servicioCombate.ejecutarAtaque(
            jugador,
            objetivo,
            comando.getAtaqueIndex()
        );
    }

    public boolean estaTerminada() {
        return servicioCombate.verificarFinBatalla(
            batalla.getJugador(),
            batalla.getEnemigos()
        );
    }
}
