package com.dojan.infiernoperfecto.entidades.enemigos;

import com.dojan.infiernoperfecto.ataques.enemigos.*;
import com.dojan.infiernoperfecto.entidades.Enemigo;

public class EnemigoLimbo1 extends Enemigo {
    public EnemigoLimbo1() {
        super("Mini on", 70, 50, 45, AtaquesComunes.ataquesBasicos());
        this.monedasBase = 20;
    }
}
