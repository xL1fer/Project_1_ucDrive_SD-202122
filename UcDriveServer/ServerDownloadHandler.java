/*
 *  "ServerDownloadHandler.java"
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
 * Download server-sided class.
 */
public class ServerDownloadHandler extends Thread {
    private DataInputStream dis;            // receive files
    private String filePath;                // file path
    private ServerSocket listenSocket;      // socket to receive packets
    private int bufSize;                    // packet buffer size
    private byte buffer[];                  // packet buffer
    
    /**
     * Creates a new ServerDownloadHandler to download from the client.
     * @param filePath path where the file is going to be located
     */
    public ServerDownloadHandler(String filePath) {
        this.bufSize = 8192;
        this.buffer = new byte[bufSize];
        this.filePath = filePath;

        // port = 0 will force operating system to search for unused ports
        try {
            this.listenSocket = new ServerSocket(0);
            this.start();
        } catch (IOException e) {
            System.out.println("<ServerDownloadHandler> IO: " + e.getMessage());
        }
    }

    public void run() {
        String fileName;
        try {
            System.out.println("\n:: Upload Socket listening on port " + getPort() + " ::");
            System.out.println("UPLOAD LISTEN SOCKET=" + listenSocket);
            Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
            System.out.println("CLIENT UPLOAD SOCKET (created at accept())="+clientSocket);
            
            dis = new DataInputStream(clientSocket.getInputStream());

            fileName = dis.readUTF();

            File newFile = new File(filePath + "\\" + fileName);
            FileOutputStream fos = new FileOutputStream(newFile);

            int n;
            while ((n = dis.read(buffer)) > 0) {
                //System.out.println("Read " + n + "B.");
                fos.write(buffer, 0, n);
            }
            
            System.out.println("CLIENT UPLOAD SOCKET CLOSING");

            fos.close();
            clientSocket.close();
            listenSocket.close();
        } catch (IOException e) {
            System.out.println("<ServerDownloadHandler> IO: " + e.getMessage());
            return;
        }

        //send files to secondary server
        UDPPortManager.addFileTransfer(1, filePath, fileName);
        new UDPPortManager(UcDriveServer.otherServerIp, UcDriveServer.portManager, true);

        return;
    }

    /**
     * Returns current port.
     * @return int with the port
     */
    public int getPort() {
        return this.listenSocket.getLocalPort();
    }

}
