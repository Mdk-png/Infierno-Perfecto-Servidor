package com.dojan.infiernoperfecto.serverred;

import com.dojan.infiernoperfecto.batalla.ControladorBatallaMultijugador;
import com.dojan.infiernoperfecto.entidades.Enemigo;
import com.dojan.infiernoperfecto.entidades.enemigos.EnemigoLimbo1;
import com.dojan.infiernoperfecto.entidades.enemigos.EnemigoLimbo2;
import com.dojan.infiernoperfecto.entidades.enemigos.MiniBossLimbo;
import com.dojan.infiernoperfecto.entidades.Jugador;
import com.dojan.infiernoperfecto.utiles.Random;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HiloServidor extends Thread {
    private DatagramSocket conexion;
    private boolean fin = false;
    private List<DireccionRed> clientesConectados = new ArrayList<>();
    private static final int MAX_CLIENTES = 2;
    private static final int PUERTO_SERVIDOR = 6666;
    private static final int PUERTO_CLIENTE = 6667;
    private boolean partidaIniciada = false;

    private List<InfoJugador> jugadores = new ArrayList<>();  // Info extendida de jugadores
    private ControladorBatallaMultijugador controladorBatalla;  // Controlador de batalla (Fase 3)

    public HiloServidor() { // Constructor
        this.setDaemon(true);
        try {
            conexion = new DatagramSocket(PUERTO_SERVIDOR); // Puerto fijo para el servidor
            conexion.setBroadcast(true); // Habilitar broadcast
            conexion.setSoTimeout(1000);
            System.out.println("Servidor: Iniciado en puerto " + PUERTO_SERVIDOR);
            System.out.println("Servidor: Esperando conexiones...");
        } catch (SocketException e) {
            e.printStackTrace(); // Error crítico al iniciar el servidor
        }
    }
    
    // Envía un mensaje (que recibe como parámetro) a un cliente específico (que tambien recibe como parámetro)
    public void enviarUnicast(String msg, InetAddress ip, int puerto) {
        try {
            byte[] mensaje = msg.getBytes();
            DatagramPacket dp = new DatagramPacket(mensaje, mensaje.length, ip, puerto);
            conexion.send(dp); // Enviar paquete a cliente específico
            System.out.println("Servidor: Enviado UNICAST '" + msg + "' a " + ip.getHostAddress() + ":" + puerto);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Envía un mensaje (que recibe como parámetro) a todos, en broadcast
    private void enviarBroadcast(String msg) { 
        try {
            byte[] mensaje = msg.getBytes();
            InetAddress broadcast = InetAddress.getByName("255.255.255.255"); // Dirección broadcast
            DatagramPacket dp = new DatagramPacket(mensaje, mensaje.length, broadcast, PUERTO_CLIENTE);
            conexion.send(dp); // Enviar paquete de broadcast a todos los clientes
            System.out.println("Servidor: BROADCAST '" + msg + "' enviado a todos");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Envía un mensaje (que recibe como parámetro) a todos, en unicast
    public void enviarATodos(String msg) { //
        System.out.println("Servidor: Enviando a todos los clientes: '" + msg + "'");
        for (DireccionRed cliente : clientesConectados) {
            enviarUnicast(msg, cliente.getIp(), cliente.getPuerto()); // Enviar a cada cliente individualmente
        }
    }

    // envia a cliente restante (usado en desconexión)
    private void enviarAlOtroCliente(String msg, InetAddress ipExcluir, int puertoExcluir) {
        System.out.println("Servidor: Enviando al otro cliente: '" + msg + "'");
        System.out.println("Servidor: Cliente a EXCLUIR: " + ipExcluir.getHostAddress() + ":" + puertoExcluir);
        System.out.println("Servidor: Total clientes en lista: " + clientesConectados.size());

        for (DireccionRed cliente : clientesConectados) {
            System.out.println("Servidor: Evaluando cliente: " + cliente.getIp().getHostAddress() + ":" + cliente.getPuerto());

            boolean esElClienteQueSeDesconecto = cliente.getIp().equals(ipExcluir) && cliente.getPuerto() == puertoExcluir;

            System.out.println("Servidor: ¿Es el que se desconectó? " + esElClienteQueSeDesconecto);

            if (!esElClienteQueSeDesconecto) {
                enviarUnicast(msg, cliente.getIp(), cliente.getPuerto());
                System.out.println("Servidor: Mensaje enviado al cliente restante");
            } else {
                System.out.println("Servidor: Cliente excluido (el que se desconectó)");
            }
        }
    }

    private long ultimoHeartbeat = System.currentTimeMillis(); // para control de heartbeat
    private static final long INTERVALO_HEARTBEAT = 5000; // originalmente estaba en 3000; lo subo a 5000 para mayor tolerancia

    @Override
    public void run() { // Método principal del hilo
        try {
            do {
                byte[] buffer = new byte[1024]; // Buffer para recibir datos
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length); // Paquete para recibir datos del tamaño del buffer
                try {
                    conexion.receive(dp); // Esperar mensaje entrante
                    procesarMensaje(dp); // Procesar el mensaje recibido
                } catch (SocketTimeoutException e) {
                    // Timeout normal, continuar el loop
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Enviar heartbeat periódico
                // funcion que cada cierto tiempo envia un mensaje HEARTBEAT a todos los clientes conectados
                // si no se recibe respuesta, el cliente puede asumir que el servidor está caído
                if (System.currentTimeMillis() - ultimoHeartbeat > INTERVALO_HEARTBEAT) {
                    if (clientesConectados.size() > 0) {
                        enviarATodos("HEARTBEAT");
                    }
                    ultimoHeartbeat = System.currentTimeMillis();
                }
            } while (!fin);
        } catch (Exception e) {
            System.err.println("Servidor: ERROR CRÍTICO en run()");
            e.printStackTrace();
        } finally {
            System.out.println("Servidor: HiloServidor detenido");
            cerrarConexion(); // Asegurar cierre de conexión al salir del run
        }
    }

    // no olvidar que en esta parte solo se reciben mensajes de los clientes
    private void procesarMensaje(DatagramPacket dp) {
        String msg = new String(dp.getData(), 0, dp.getLength()).trim();
        InetAddress clienteIP = dp.getAddress(); //aca consigue la ip del cliente
        int clientePuerto = dp.getPort(); //aca consigue el puerto del cliente

        System.out.println("Servidor: Recibido '" + msg + "' de " + clienteIP.getHostAddress() + ":" + clientePuerto);

        // CONEXION
        if (msg.equals("Conexion")) {
            manejarConexion(clienteIP, clientePuerto);

        // DESCONEXION
        } else if (msg.equals("Desconexion")) {
            manejarDesconexion(clienteIP, clientePuerto);
            System.out.println("Servidor: Desconexión de " + clienteIP.getHostAddress() + ":" + clientePuerto);
            
        // SELECCIONAR ENEMIGO:id
        } else if (msg.startsWith("SELECCIONAR_ENEMIGO:")) {
            try {
                int indiceEnemigo = Integer.parseInt(msg.split(":")[1])
                registrarSeleccionEnemigo(clienteIP, clientePuerto, indiceEnemigo); 
            } catch (Exception e) {
                System.out.println("Servidor: Error procesando SELECCIONAR_ENEMIGO: " + e.getMessage());
            }

        // SELECCIONAR ATAQUE:id
        } else if (msg.startsWith("SELECCIONAR_ATAQUE:")) {
            try {
                int indiceAtaque = Integer.parseInt(msg.split(":")[1]);
                registrarSeleccionAtaque(clienteIP, clientePuerto, indiceAtaque); // Registrar selección de ataque
            } catch (Exception e) {
                System.out.println("Servidor: Error procesando SELECCIONAR_ATAQUE: " + e.getMessage());
            }

        // LISTO SIGUIENTE NIVEL
        } else if (msg.equals("LISTO_SIGUIENTE_NIVEL")) {
            if (controladorBatalla != null) {
                controladorBatalla.clienteListoParaSiguienteNivel(); // Notificar que el cliente está listo para el siguiente nivel
            }

        // LISTO RESULTADOS
        } else if (msg.equals("LISTO_RESULTADOS")) {
            if (controladorBatalla != null) {
                controladorBatalla.clienteListoParaResultados(); // Notificar que el cliente está listo para ver resultados
            }
        
        // CONFIRMAR LOG
        // esto iria luego de que se ejecutaron todos los turnos y se envio el log
        } else if (msg.equals("CONFIRMAR_LOG")) {
            if (controladorBatalla != null) {
                // Buscar el jugador que envió el mensaje
                InfoJugador jugador = buscarJugador(clienteIP, clientePuerto);
                if (jugador != null) {
                    controladorBatalla.clienteConfirmoLog(jugador); // Notificar que el cliente confirmó el log
                }
            }

        // COMPRAR ITEM
        } else if (msg.startsWith("COMPRAR_ITEM:")) {
            // COMPRAR_ITEM:COSTO  o  COMPRAR_ITEM:COSTO:VIDA:FE
            if (controladorBatalla != null) {
                InfoJugador jugador = buscarJugador(clienteIP, clientePuerto);
                if (jugador != null) {
                    try {
                        String[] partes = msg.split(":");
                        int costo = Integer.parseInt(partes[1]);

                        if (partes.length > 3) { // COSTO:VIDA:FE
                           controladorBatalla.procesarCompraItem(jugador.getNumeroJugador(), costo,
                                                               Integer.parseInt(partes[2]),
                                                               Integer.parseInt(partes[3]));
                        } else {
                           controladorBatalla.procesarCompraItem(jugador.getNumeroJugador(), costo);
                        }
                    } catch(Exception e) {
                        System.out.println("Error procesando compra: " + e.getMessage());
                    }
                }
            }

        // SALIR TIENDA
        } else if (msg.equals("SALIR_TIENDA")) {
            if (controladorBatalla != null) {
                 InfoJugador jugador = buscarJugador(clienteIP, clientePuerto);
                 if (jugador != null) {
                     controladorBatalla.procesarSalidaTienda(jugador.getNumeroJugador());
                 }
            }
        }
    }


    private void manejarConexion(InetAddress clienteIP, int clientePuerto) {
        // Verificar si el cliente ya está conectado
        boolean yaConectado = false;
        for (DireccionRed cliente : clientesConectados) {
            if (cliente.getIp().equals(clienteIP) && cliente.getPuerto() == clientePuerto) {
                yaConectado = true;
                break;
            }
        }

        // Si no está conectado y hay espacio, agregarlo

        if (!yaConectado && clientesConectados.size() < MAX_CLIENTES) {
            clientesConectados.add(new DireccionRed(clienteIP, clientePuerto)); // añade al cliente a la lista de clientes conectados

            //  Crear InfoJugador con número asignado
            int numJugador = clientesConectados.size();
            InfoJugador info = new InfoJugador(clienteIP, clientePuerto, numJugador);
            jugadores.add(info); // añade al jugador a la lista de jugadores, con mas informacion especifica al juego

            // notificar al cliente que se conectó exitosamente
            enviarUnicast("OK", clienteIP, clientePuerto);
            enviarUnicast("ASIGNAR_JUGADOR:" + numJugador, clienteIP, clientePuerto); // Enviar asignación

            // notificar a todos los clientes sobre el cliente conectado
            System.out.println("Servidor: Cliente conectado. Total: " + clientesConectados.size() + "/" + MAX_CLIENTES);
            enviarATodos("ESPERANDO:" + clientesConectados.size());

            // si hay 2 jugadores iniciar partida
            if (clientesConectados.size() == MAX_CLIENTES) {
                System.out.println("Servidor: INICIANDO PARTIDA CON 2 JUGADORES");
                partidaIniciada = true; // Marcar que la partida empezó

                // se envie el mensaje INICIAR varias veces para asegurar recepción
                for (int i = 0; i < 5; i++) {
                    enviarATodos("INICIAR");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // iniciar batalla multijugador
                iniciarBatallaMultijugador();
            }
        } else if (yaConectado) {
            enviarUnicast("OK", clienteIP, clientePuerto);
        } else {
            enviarUnicast("SERVIDOR_LLENO", clienteIP, clientePuerto);
        }
    }

    private void manejarDesconexion(InetAddress clienteIP, int clientePuerto) {
        DireccionRed clienteAEliminar = null;

        // buscar el cliente en la lista de clientes conectados en base a su IP y puerto
        for (DireccionRed cliente : clientesConectados) {
            if (cliente.getIp().equals(clienteIP) && cliente.getPuerto() == clientePuerto) {
                clienteAEliminar = cliente;
                break;
            }
        }

        if (clienteAEliminar != null) {
            // SI LA PARTIDA YA HABÍA INICIADO
            if (partidaIniciada && clientesConectados.size() == 2) {
                System.out.println("Servidor: JUGADOR SE DESCONECTÓ DURANTE LA PARTIDA");
                System.out.println("Servidor: Notificando al otro jugador y cerrando la partida...");

                // Enviar mensaje al otro cliente
                for (int i = 0; i < 3; i++) {
                    enviarAlOtroCliente("COMPANIERO_DESCONECTADO", clienteIP, clientePuerto);
                    try {
                        Thread.sleep(100); // esperar 100 ms para mandar de vuelta el mensaje
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // LIMPIAR COMPLETAMENTE - Desconectar a AMBOS clientes
                System.out.println("Servidor: Limpiando todos los clientes de la partida...");
                clientesConectados.clear(); // Eliminar TODOS los clientes
                jugadores.clear();          // Limpiar lista de InfoJugador
                partidaIniciada = false;
                System.out.println("Servidor: Partida terminada. Total clientes: " + clientesConectados.size() + "/2");

            } else {
                // Si no había iniciado la partida, solo eliminar el cliente normal
                clientesConectados.remove(clienteAEliminar);

                // Remover también de la lista de InfoJugador
                InfoJugador infoToRemove = buscarJugador(clienteIP, clientePuerto);
                if (infoToRemove != null) {
                    jugadores.remove(infoToRemove);
                }

                System.out.println("Servidor: Cliente desconectado. Total: " + clientesConectados.size() + "/" + MAX_CLIENTES);

                // Confirmar desconexión al cliente que se va
                enviarUnicast("DESCONECTADO", clienteIP, clientePuerto);

                // Actualizar contadores para el otro cliente
                if (clientesConectados.size() > 0) {
                    enviarATodos("ESPERANDO:" + clientesConectados.size());
                }
            }
        } else {
            System.out.println("Servidor: Cliente no encontrado para desconectar");
        }
    }

    public int getCantidadClientes() {
        return clientesConectados.size();
    }

    public void detener() {
        System.out.println("Servidor: Enviando notificación de cierre a todos los clientes...");
        for (int i = 0; i < 3; i++) {
            enviarATodos("SERVIDOR_CERRANDO");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        fin = true;
    }

    public void cerrarConexion() {
        System.out.println("Servidor: Cerrando servidor...");
        enviarATodos("SERVIDOR_CERRANDO");
        fin = true;
        if (conexion != null && !conexion.isClosed()) {
            conexion.close();
        }
        System.out.println("Servidor: Socket cerrado");
    }

    private void registrarSeleccionEnemigo(InetAddress ip, int puerto, int indiceEnemigo) {
        InfoJugador jugador = buscarJugador(ip, puerto); // buscar jugador por IP y puerto
        if (jugador != null) {
            jugador.setEnemigoSeleccionado(indiceEnemigo); // Registrar selección de enemigo
            System.out.println("Servidor: Jugador " + jugador.getNumeroJugador() +
                             " seleccionó enemigo " + indiceEnemigo);

            // Verificar si ambos jugadores completaron su selección
            verificarSeleccionesCompletas();
        }
    }

    private void registrarSeleccionAtaque(InetAddress ip, int puerto, int indiceAtaque) {
        InfoJugador jugador = buscarJugador(ip, puerto); // guarda al jugador que eligio el ataque
        if (jugador != null) {
            jugador.setAtaqueSeleccionado(indiceAtaque); // Registrar selección de ataque en InfoJugador, pero solo para un jugador
            System.out.println("Servidor: Jugador " + jugador.getNumeroJugador() +
                             " seleccionó ataque " + indiceAtaque);

            // Si este jugador completó su selección, notificarle que espere
            if (jugador.tieneSeleccionCompleta()) {
                enviarUnicast("ESPERANDO_OTRO_JUGADOR", ip, puerto);
            }

            // Verificar si ambos jugadores completaron su selección
            verificarSeleccionesCompletas();
        }
    }


    private void verificarSeleccionesCompletas() {
        if (jugadores.size() == 2) {
            boolean ambosListos = jugadores.get(0).tieneSeleccionCompleta() &&
                                 jugadores.get(1).tieneSeleccionCompleta();

            if (ambosListos) {
                System.out.println("Servidor: ¡Ambos jugadores listos! Ejecutando turno...");

                if (controladorBatalla != null) {
                    InfoJugador j1 = jugadores.get(0); // crea un InfoJugador para el jugador 1
                    InfoJugador j2 = jugadores.get(1); // crea un InfoJugador para el jugador 2

                    // Ejecutar turno de los jugadores cuando verifica que ambos seleccionaron
                    controladorBatalla.ejecutarTurnoJugadores(
                        j1.getEnemigoSeleccionado(), j1.getAtaqueSeleccionado(),
                        j2.getEnemigoSeleccionado(), j2.getAtaqueSeleccionado() 
                    );
                }
            }
        }
    }

    /**
     * Busca un jugador por IP y puerto
     */
    private InfoJugador buscarJugador(InetAddress ip, int puerto) {
        for (InfoJugador jugador : jugadores) {
            if (jugador.getDireccion().getIp().equals(ip) &&
                jugador.getDireccion().getPuerto() == puerto) {
                return jugador;
            }
        }
        return null;
    }

    /**
     * Getter para la lista de jugadores (usado por ControladorBatallaMultijugador en Fase 3)
     */
    public List<InfoJugador> getJugadores() {
        return jugadores;
    }

    /**
     * Establece el controlador de batalla (Fase 3)
     */
    public void setControladorBatalla(ControladorBatallaMultijugador controlador) {
        this.controladorBatalla = controlador;
    }

    /**
     * Obtiene el controlador de batalla (Fase 3)
     */
    public ControladorBatallaMultijugador getControladorBatalla() {
        return controladorBatalla;
    }
    
    /**
     * Inicializa automáticamente la batalla multijugador cuando se conectan 2 jugadores
     */
    private void iniciarBatallaMultijugador() {
    System.out.println("Servidor: ========================================");
    System.out.println("Servidor: INICIALIZANDO BATALLA MULTIJUGADOR");
    System.out.println("Servidor: ========================================");

    // Crear controlador con piso y nivel inicial
    controladorBatalla = new ControladorBatallaMultijugador(this, 1, 1);

    // Crear jugadores con personajes básicos (Peleador)
    Jugador j1 = new Jugador("Jugador1", new com.dojan.infiernoperfecto.entidades.clases.Peleador());
    Jugador j2 = new Jugador("Jugador2", new com.dojan.infiernoperfecto.entidades.clases.Peleador());

    // Iniciar batalla
    controladorBatalla.iniciarBatalla(j1, j2);

    System.out.println("Servidor: ✓ Batalla iniciada exitosamente");
    }
}
