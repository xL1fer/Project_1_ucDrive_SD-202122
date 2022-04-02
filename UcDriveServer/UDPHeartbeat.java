/*
 *  "UDPHeartbeat.java"
 * 
 *  ====================================
 *
 *  Universidade de Coimbra
 *  Faculdade de Ciências e Tecnologia
 *  Departamento de Engenharia Informatica
 * 
 *  Alexandre Gameiro Leopoldo - 2019219929
 *  Luís Miguel Gomes Batista  - 2019214869
 * 
 *  ====================================
 * 
 *  "ucDrive Project"
 */

import java.io.*;
import java.net.*;

/**
 * Heartbeat class (for primary server).
 */
public class UDPHeartbeat extends Thread {
    //private String serverIp;
    private int port;
    private DatagramSocket aSocket;
    private int heartbeatDelay;

    /**
     * Creates a new UDPHeartbeat thread to listen for heartbeats from the secondary server.
     * @param serverIp secondary server IP
     * @param port port to listen to
     * @param heartbeatDelay delay between heartbeats
     */
    public UDPHeartbeat(String serverIp, int port, int heartbeatDelay) {
        //this.serverIp = serverIp;
        this.port = port;
        this.heartbeatDelay = heartbeatDelay - 100;
        try {
			this.aSocket = new DatagramSocket(port);
            this.aSocket.setSoTimeout(heartbeatDelay);
			this.start();
		} catch (SocketException e) {
			System.out.println("<UDPHeartbeat> Socket: " + e.getMessage());
		}
    }

    public void run() {
        System.out.println("\n:: UDP Socket listening on port " + port + " ::");
        while (true) {
            try {
                byte buffer[] = new byte[1];
                buffer[0] = (byte)0xAA;	
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);

                if (UcDriveServer.otherServerUp == false) {
                    //UcDrive_Server.replicateFiles()
                    System.out.println("<UcDriveServer> Secondary server up, replicating files.");
                    UcDriveServer.otherServerUp = true;
                    UcDriveServer.replicateFiles(null);
                    new UDPPortManager(UcDriveServer.otherServerIp, UcDriveServer.portManager, true);
                }

                UcDriveServer.otherServerUp = true;

                DatagramPacket reply = new DatagramPacket(buffer, 
                buffer.length, request.getAddress(), request.getPort());
                aSocket.send(reply);
                Thread.sleep(heartbeatDelay);
            } catch(IOException e) {
                //System.out.println("UDPHeartbeat - IOException: " + e.getMessage());
                UcDriveServer.otherServerUp = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
