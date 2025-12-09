package com.dojan.infiernoperfecto.entidades.enemigos;

import com.dojan.infiernoperfecto.ataques.enemigos.AtaquesMiniBoss;
import com.dojan.infiernoperfecto.entidades.Enemigo;

public class MiniBossCodicia extends Enemigo {
    public MiniBossCodicia() {
        super("Hormigagula", 340, 60, 65, AtaquesMiniBoss.ataquesBasicos());
    }
}
