package com.dojan.infiernoperfecto.entidades.enemigos;

import com.dojan.infiernoperfecto.ataques.enemigos.AtaquesMiniBoss;
import com.dojan.infiernoperfecto.entidades.Enemigo;

public class MiniBossLimbo extends Enemigo {
    public MiniBossLimbo() {
        super("Sabueso", 300, 60, 65, AtaquesMiniBoss.ataquesBasicos());
        this.monedasBase = 200;
    }
}
