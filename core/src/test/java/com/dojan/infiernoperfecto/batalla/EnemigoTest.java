package com.dojan.infiernoperfecto.batalla;

import com.dojan.infiernoperfecto.ataques.Ataque;
import com.dojan.infiernoperfecto.entidades.Enemigo;

import java.util.ArrayList;
import java.util.List;

/**
 * Enemigo simple para testing.
 * Tiene stats bajos para facilitar las pruebas.
 */
public class EnemigoTest extends Enemigo {
    
    public EnemigoTest(String nombre, int vida, int defensa, int danio) {
        super(nombre, vida, defensa, danio, crearAtaques());
    }
    
    /**
     * Constructor por defecto con stats predefinidos
     */
    public EnemigoTest() {
        this("Enemigo Test", 50, 5, 10);
    }
    
    private static List<Ataque> crearAtaques() {
        List<Ataque> ataques = new ArrayList<>();
        // Ataque simple de prueba
        ataques.add(new Ataque("Golpe Test", 10, 0, 99));
        return ataques;
    }
}
