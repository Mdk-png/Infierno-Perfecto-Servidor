package com.dojan.infiernoperfecto.entidades.enemigos;

import com.dojan.infiernoperfecto.ataques.enemigos.AtaquesComunes;
import com.dojan.infiernoperfecto.entidades.Enemigo;

public class EnemigoLimbo2 extends Enemigo {
    public EnemigoLimbo2() {
        super("Esbirro", 200, 30, 40, AtaquesComunes.ataquesBasicos());
        this.monedasBase = 10;
    }
}
