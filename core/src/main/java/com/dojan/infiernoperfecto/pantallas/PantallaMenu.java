package com.dojan.infiernoperfecto.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.dojan.infiernoperfecto.elementos.Imagen;
import com.dojan.infiernoperfecto.elementos.Musica;
import com.dojan.infiernoperfecto.elementos.Texto;
import com.dojan.infiernoperfecto.utiles.Config;
import com.dojan.infiernoperfecto.utiles.ControlAudio;
import com.dojan.infiernoperfecto.utiles.Recursos;
import static com.dojan.infiernoperfecto.utiles.Render.app; //es una instancia de InfiernoPerfecto
import static com.dojan.infiernoperfecto.utiles.Render.batch;

import io.Entradas;

public class PantallaMenu implements Screen {
    private Musica musicaFondo;
    private Imagen menu;
    final private Texto opciones[] = new Texto[3];
    String textos[] = {
        "Iniciar Servidor",
        "Opciones",
        "Salir"
    };
    private int opc = 1;
    private boolean mouseClick = false;
    private float tiempo;
    private FitViewport fitViewport;

    public PantallaMenu(){
    }

    Entradas entradas = new Entradas();

    @Override
    public void show() {
        fitViewport = new FitViewport(800,600);
        musicaFondo = new Musica(Recursos.MUSICAMENU);
        ControlAudio.setMusicaActual(musicaFondo);

        menu = new Imagen(Recursos.FONDOMENU);
        menu.setSize(Config.ANCHO, Config.ALTO);

        Gdx.input.setInputProcessor(entradas);

        int avanceY = 40;
        int avanceX = 200;

        for(int i = 0; i<opciones.length;i++){
            opciones[i] = new Texto(Recursos.FUENTEMENU,90,Color.WHITE,true);
            opciones[i].setTexto(textos[i]);
            opciones[i].setPosition((int) ((Config.ANCHO/2)-(opciones[i].getAncho()/2))+avanceX, (int) (((Config.ALTO/1.4f)+(opciones[0].getAlto()/2))-(opciones[i].getAlto()+avanceY)*i));
        }

    }

    @Override
    public void render(float delta) {
        fitViewport.apply();
        ControlAudio.reproducirMusica();


        batch.begin();
            menu.dibujar();
            for(int i = 0; i<opciones.length;i++){
                opciones[i].dibujar();
            }
        batch.end();



        tiempo+= delta;

        if(entradas.isAbajo()){
            if(tiempo>0.15f){
                tiempo=0;
                opc++;
                if(opc>3){
                    opc=1;
                }
            }

        }

        if(entradas.isArriba()){
            if(tiempo>0.1f){
                tiempo=0;
                opc--;
                if(opc<1){
                    opc=3;
                }
            }

        }
//CUESTION MOUSE
        int cont = 0;
        for(int i = 0; i<opciones.length;i++){
            if ((entradas.getMouseX()>=opciones[i].getX())&&(entradas.getMouseX()<=(opciones[i].getX()+opciones[i].getAncho()))){
                if((((entradas.getMouseY()>=opciones[i].getY()-opciones[i].getAlto())&&(entradas.getMouseY()<=(opciones[i].getY()))))){
                    opc=i+1;
                    cont++;
                }

            }
        }
        if(cont >0){
            mouseClick = true;
        }else{
            mouseClick = false;
        }


        for(int i=0; i<opciones.length;i++){
            if(i==(opc-1)){
                opciones[i].setColor(Color.GOLDENROD);
            }else{
                opciones[i].setColor(Color.WHITE);
            }
        }

        if(entradas.isEnter() || entradas.isClick()){
            if(((opc==1)&&(entradas.isEnter())) || ((opc==1)&&(entradas.isClick())&&(mouseClick))){
                // Iniciar servidor en lugar de ir a otra pantalla
                app.iniciarServidor();
                ControlAudio.pararMusica();
                app.setScreen(new PantallaServidorActivo());
            }else if(((opc==2)&&(entradas.isEnter())) || ((opc==2)&&(entradas.isClick())&&(mouseClick))){
                app.setScreen(new PantallaOpciones());
            }else if(((opc==3)&&(entradas.isEnter())) || ((opc==3)&&(entradas.isClick())&&(mouseClick))){
                Gdx.app.exit();
            }
        }




    }

    @Override
    public void resize(int width, int height) {
        fitViewport.update(width, height);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        // dispose resources created in show()
        if (musicaFondo != null) {
            try{ musicaFondo.dispose(); }catch(Exception e){}
            musicaFondo = null;
        }

        if (menu != null) {
            try{ menu.dispose(); }catch(Exception e){}
            menu = null;
        }

        if (opciones != null) {
            for(int i=0;i<opciones.length;i++){
                if (opciones[i] != null){
                    try{ opciones[i].dispose(); }catch(Exception e){}
                    opciones[i] = null;
                }
            }
        }
    }

    public void setTiempo(float tiempo) {
        this.tiempo = tiempo;
    }
//
//    public Musica getMusicaFondo() {
//        return musicaFondo;
//    }
}
