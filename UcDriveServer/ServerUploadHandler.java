/*
 *  "ServerUploadHandler.java"
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
 * Upload server-sided class
 */
public class ServerUploadHandler extends Thread {
    private DataOutputStream dos;           // send files
    private String filePath;                // file path
    private ServerSocket listenSocket;      // socket to send packets
    private int bufSize = 8192;             // packet buffer size
    private byte buffer[];                  // packet buffer
    
    public ServerUploadHandler(String filePath) {
        this.buffer = new byte[bufSize];
        this.filePath = filePath;
        
        // port = 0 will force operating system to search for unused ports
        try {
            this.listenSocket = new ServerSocket(0);
            this.start();
        } catch (IOException e) {
            System.out.println("<ServerUploadHandler> IO: " + e.getMessage());
        }
    }

    public void run() {
        try {
            System.out.println("\n:: Download Socket listening on port " + getPort() + " ::");
            System.out.println("DOWNLOAD LISTEN SOCKET=" + listenSocket);
            Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
            System.out.println("CLIENT DOWNLOAD SOCKET (created at accept())="+clientSocket);
            
            dos = new DataOutputStream(clientSocket.getOutputStream());
            dos.flush();

            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            String fileName = file.getName();

            dos.writeUTF(fileName);

            int n;
            while((n = fis.read(buffer)) > 0){
                //System.out.println("Sent " + n + "B.");
                dos.write(buffer, 0, n);
            }
            dos.flush();

            System.out.println("CLIENT DOWNLOAD SOCKET CLOSING");

            fis.close();
            clientSocket.close();
            listenSocket.close();
        } catch (IOException e) {
            System.out.println("<ServerUploadHandler> IO: " + e.getMessage());
        }

        return;
    }

    // get port created by OS
    public int getPort() {
        return this.listenSocket.getLocalPort();
    }

}
