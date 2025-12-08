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
    
    // ============================================================
    // ATRIBUTOS PARA MODO MULTIJUGADOR - FASE 2.3 y 3
    // ============================================================
    private List<InfoJugador> jugadores = new ArrayList<>();  // Info extendida de jugadores
    private ControladorBatallaMultijugador controladorBatalla;  // Controlador de batalla (Fase 3)

    public HiloServidor() {
        this.setDaemon(true);
        try {
            conexion = new DatagramSocket(PUERTO_SERVIDOR);
            conexion.setBroadcast(true);
            conexion.setSoTimeout(1000);
            System.out.println("Servidor: Iniciado en puerto " + PUERTO_SERVIDOR);
            System.out.println("Servidor: Esperando conexiones...");
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void enviarUnicast(String msg, InetAddress ip, int puerto) {
        try {
            byte[] mensaje = msg.getBytes();
            DatagramPacket dp = new DatagramPacket(mensaje, mensaje.length, ip, puerto);
            conexion.send(dp);
            System.out.println("Servidor: Enviado UNICAST '" + msg + "' a " + ip.getHostAddress() + ":" + puerto);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enviarBroadcast(String msg) {
        try {
            byte[] mensaje = msg.getBytes();
            InetAddress broadcast = InetAddress.getByName("255.255.255.255");
            DatagramPacket dp = new DatagramPacket(mensaje, mensaje.length, broadcast, PUERTO_CLIENTE);
            conexion.send(dp);
            System.out.println("Servidor: 📡 BROADCAST '" + msg + "' enviado a todos");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void enviarATodos(String msg) {
        System.out.println("Servidor: Enviando a todos los clientes: '" + msg + "'");
        enviarBroadcast(msg);
        for (DireccionRed cliente : clientesConectados) {
            enviarUnicast(msg, cliente.getIp(), cliente.getPuerto());
        }
    }


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
                System.out.println("Servidor: ✅ Mensaje enviado al cliente restante");
            } else {
                System.out.println("Servidor: ❌ Cliente excluido (el que se desconectó)");
            }
        }
    }

    private long ultimoHeartbeat = System.currentTimeMillis();
    private static final long INTERVALO_HEARTBEAT = 5000; // originalmente estaba en 3000; lo subo a 5000 para mayor tolerancia

    @Override
    public void run() {
        try {
            do {
                byte[] buffer = new byte[1024];
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                try {
                    conexion.receive(dp);
                    procesarMensaje(dp);
                } catch (SocketTimeoutException e) {
                    // Timeout normal, continuar el loop
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Enviar heartbeat periódico
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
            cerrarConexion();
        }
    }

    private void procesarMensaje(DatagramPacket dp) {
        String msg = new String(dp.getData(), 0, dp.getLength()).trim();
        InetAddress clienteIP = dp.getAddress();
        int clientePuerto = dp.getPort();

        System.out.println("Servidor: Recibido '" + msg + "' de " + clienteIP.getHostAddress() + ":" + clientePuerto);

        if (msg.equals("Conexion")) {
            manejarConexion(clienteIP, clientePuerto);

        } else if (msg.equals("Desconexion")) {
            manejarDesconexion(clienteIP, clientePuerto);
        
        // ============================================================
        // MENSAJES DE BATALLA MULTIJUGADOR - FASE 2.3
        // ============================================================
        } else if (msg.startsWith("SELECCIONAR_ENEMIGO:")) {
            try {
                int indiceEnemigo = Integer.parseInt(msg.split(":")[1]);
                registrarSeleccionEnemigo(clienteIP, clientePuerto, indiceEnemigo);
            } catch (Exception e) {
                System.out.println("Servidor: Error procesando SELECCIONAR_ENEMIGO: " + e.getMessage());
            }
        
        } else if (msg.startsWith("SELECCIONAR_ATAQUE:")) {
            try {
                int indiceAtaque = Integer.parseInt(msg.split(":")[1]);
                registrarSeleccionAtaque(clienteIP, clientePuerto, indiceAtaque);
            } catch (Exception e) {
                System.out.println("Servidor: Error procesando SELECCIONAR_ATAQUE: " + e.getMessage());
            }
        } else if (msg.equals("LISTO_SIGUIENTE_NIVEL")) {
            if (controladorBatalla != null) {
                controladorBatalla.clienteListoParaSiguienteNivel();
            }
        } else if (msg.equals("LISTO_RESULTADOS")) {
            if (controladorBatalla != null) {
                controladorBatalla.clienteListoParaResultados();
            }
        } else if (msg.equals("CONFIRMAR_LOG")) {
            if (controladorBatalla != null) {
                // Buscar el jugador que envió el mensaje
                InfoJugador jugador = buscarJugador(clienteIP, clientePuerto);
                if (jugador != null) {
                    controladorBatalla.clienteConfirmoLog(jugador);
                }
            }
        }
    }

    private void manejarConexion(InetAddress clienteIP, int clientePuerto) {
        boolean yaConectado = false;
        for (DireccionRed cliente : clientesConectados) {
            if (cliente.getIp().equals(clienteIP) && cliente.getPuerto() == clientePuerto) {
                yaConectado = true;
                break;
            }
        }

        if (!yaConectado && clientesConectados.size() < MAX_CLIENTES) {
            clientesConectados.add(new DireccionRed(clienteIP, clientePuerto));
            
            // FASE 2.3: Crear InfoJugador y asignar número
            int numJugador = clientesConectados.size();
            InfoJugador info = new InfoJugador(clienteIP, clientePuerto, numJugador);
            jugadores.add(info);
            
            enviarUnicast("OK", clienteIP, clientePuerto);
            enviarUnicast("ASIGNAR_JUGADOR:" + numJugador, clienteIP, clientePuerto); // Enviar asignación
            
            System.out.println("Servidor: ✅ Cliente conectado. Total: " + clientesConectados.size() + "/" + MAX_CLIENTES);
            enviarATodos("ESPERANDO:" + clientesConectados.size());

            if (clientesConectados.size() == MAX_CLIENTES) {
                System.out.println("Servidor: ========================================");
                System.out.println("Servidor: ¡¡¡INICIANDO PARTIDA CON 2 JUGADORES!!!");
                System.out.println("Servidor: ========================================");
                partidaIniciada = true; // ✅ Marcar que la partida empezó

                for (int i = 0; i < 5; i++) {
                    enviarATodos("INICIAR");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                
                // FASE 5.1: Auto-iniciar batalla multijugador
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

        for (DireccionRed cliente : clientesConectados) {
            if (cliente.getIp().equals(clienteIP) && cliente.getPuerto() == clientePuerto) {
                clienteAEliminar = cliente;
                break;
            }
        }

        if (clienteAEliminar != null) {
            // ✅ SI LA PARTIDA YA HABÍA INICIADO
            if (partidaIniciada && clientesConectados.size() == 2) {
                System.out.println("Servidor: 🚨 JUGADOR SE DESCONECTÓ DURANTE LA PARTIDA");
                System.out.println("Servidor: Notificando al otro jugador y cerrando la partida...");

                // Enviar mensaje al otro cliente
                for (int i = 0; i < 3; i++) {
                    enviarAlOtroCliente("COMPANIERO_DESCONECTADO", clienteIP, clientePuerto);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // ✅ LIMPIAR COMPLETAMENTE - Desconectar a AMBOS clientes
                System.out.println("Servidor: Limpiando todos los clientes de la partida...");
                clientesConectados.clear(); // Eliminar TODOS los clientes
                jugadores.clear();          // Limpiar lista de InfoJugador
                partidaIniciada = false;
                System.out.println("Servidor: ✅ Partida terminada. Total clientes: " + clientesConectados.size() + "/2");

            } else {
                // Si no había iniciado la partida, solo eliminar el cliente normal
                clientesConectados.remove(clienteAEliminar);
                
                // Remover también de la lista de InfoJugador
                InfoJugador infoToRemove = buscarJugador(clienteIP, clientePuerto);
                if (infoToRemove != null) {
                    jugadores.remove(infoToRemove);
                }
                
                System.out.println("Servidor: ❌ Cliente desconectado. Total: " + clientesConectados.size() + "/" + MAX_CLIENTES);

                // Confirmar desconexión al cliente que se va
                enviarUnicast("DESCONECTADO", clienteIP, clientePuerto);

                // Actualizar contadores para el otro cliente
                if (clientesConectados.size() > 0) {
                    enviarATodos("ESPERANDO:" + clientesConectados.size());
                }
            }
        } else {
            System.out.println("Servidor: ⚠️ Cliente no encontrado para desconectar");
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
    
    // ============================================================
    // MÉTODOS PARA MODO MULTIJUGADOR - FASE 2.3
    // ============================================================
    
    /**
     * Registra la selección de enemigo de un jugador
     */
    private void registrarSeleccionEnemigo(InetAddress ip, int puerto, int indiceEnemigo) {
        InfoJugador jugador = buscarJugador(ip, puerto);
        if (jugador != null) {
            jugador.setEnemigoSeleccionado(indiceEnemigo);
            System.out.println("Servidor: Jugador " + jugador.getNumeroJugador() + 
                             " seleccionó enemigo " + indiceEnemigo);
            
            // Verificar si ambos jugadores completaron su selección
            verificarSeleccionesCompletas();
        }
    }
    
    /**
     * Registra la selección de ataque de un jugador
     */
    private void registrarSeleccionAtaque(InetAddress ip, int puerto, int indiceAtaque) {
        InfoJugador jugador = buscarJugador(ip, puerto);
        if (jugador != null) {
            jugador.setAtaqueSeleccionado(indiceAtaque);
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
    
    /**
     * Verifica si ambos jugadores completaron sus selecciones
     * Si sí, aquí se ejecutaría el turno (Fase 3)
     */
    private void verificarSeleccionesCompletas() {
        if (jugadores.size() == 2) {
            boolean ambosListos = jugadores.get(0).tieneSeleccionCompleta() && 
                                 jugadores.get(1).tieneSeleccionCompleta();
            
            if (ambosListos) {
                System.out.println("Servidor: ¡Ambos jugadores listos! Ejecutando turno...");
                
                if (controladorBatalla != null) {
                    InfoJugador j1 = jugadores.get(0);
                    InfoJugador j2 = jugadores.get(1);
                    
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
    
    // ============================================================
    // INICIALIZACIÓN DE BATALLA MULTIJUGADOR - FASE 5.1
    // ============================================================
    
    /**
     * Inicializa automáticamente la batalla multijugador cuando se conectan 2 jugadores
     */
    private void iniciarBatallaMultijugador() {
    System.out.println("Servidor: ========================================");
    System.out.println("Servidor: INICIALIZANDO BATALLA MULTIJUGADOR");
    System.out.println("Servidor: ========================================");
    
    // Crear controlador con piso y nivel inicial
    controladorBatalla = new ControladorBatallaMultijugador(this, 1, 1);
    
    // Crear jugadores
    Jugador j1 = new Jugador("Jugador1", new com.dojan.infiernoperfecto.entidades.clases.Peleador());
    Jugador j2 = new Jugador("Jugador2", new com.dojan.infiernoperfecto.entidades.clases.Peleador());
    
    // Iniciar batalla
    controladorBatalla.iniciarBatalla(j1, j2);
    
    System.out.println("Servidor: ✓ Batalla iniciada exitosamente");
}
}
