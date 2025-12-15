package com.dojan.infiernoperfecto;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dojan.infiernoperfecto.pantallas.PantallaMenu;
import com.dojan.infiernoperfecto.serverred.HiloServidor;
import com.dojan.infiernoperfecto.utiles.Render;


/** {@link ApplicationListener} implementation shared by all platforms. */
public class InfiernoPerfecto extends Game {
    private HiloServidor hiloServidor;

    @Override
    public void create() {
        Render.app = this;
        Render.batch = new SpriteBatch();

        // iniciará cuando el usuario presione "Iniciar Servidor" en el menú
        System.out.println("=================================");
        System.out.println("   SERVIDOR - INFIERNO PERFECTO  ");
        System.out.println("=================================");
        System.out.println("Esperando que se inicie el servidor...");

        this.setScreen(new PantallaMenu());
    }

    @Override
    public void render() {
        super.render();
    }

    //  Al tocar el boton de iniciar servidor este agarra la app creada en render
    //  (que es una instancia de InfiernoPerfecto) y llama al metodo iniciarServidor()
    //  este crea un Hilo servidor que inicia el servidor mediante start()
    public void iniciarServidor() {
        if (hiloServidor == null || !hiloServidor.isAlive()) {

            // Crea el hilo del servidor
            hiloServidor = new HiloServidor();
            hiloServidor.start(); //start es un metodo nativo de Thread que inicia el hilo

            System.out.println("✓ Servidor iniciado correctamente.");
        } else {
            System.out.println("El servidor ya está en ejecución.");
        }
    }

    // Método para verificar si el servidor está en ejecución
    public boolean isServidorActivo() {
        return hiloServidor != null && hiloServidor.isAlive();
    }

    private void update(){

    }

    @Override
    public void dispose() {
        // Detener servidor de red correctamente
        if (hiloServidor != null) {
            try {
                hiloServidor.detener();
                // Dar tiempo para que el hilo se cierre
                hiloServidor.join(2000);
                System.out.println("Servidor: HiloServidor detenido");
            } catch (InterruptedException e) {
                System.err.println("Servidor: Error al esperar detencion de HiloServidor - " + e.getMessage());
                Thread.currentThread().interrupt();
            }
            hiloServidor = null;
        }

        // dispose global rendering resources
        try{
            if (Render.batch != null) {
                Render.batch.dispose();
                Render.batch = null;
            }
            if (Render.renderer != null) {
                Render.renderer.dispose();
                Render.renderer = null;
            }
        }catch(Exception e){
            // ignore
        }

        // dispose current screen (if set)
        if (getScreen() != null){
            try{ getScreen().dispose(); }catch(Exception e){ }
        }

        // dispose audio control
        com.dojan.infiernoperfecto.utiles.ControlAudio.dispose();
    }

    public HiloServidor getServidor() {
        return hiloServidor;
    }
}
