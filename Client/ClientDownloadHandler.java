/*
 *  "ClientDownloadHandler.java"
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
 * ClientDownloadHandler class responsible for handling client downloads.
 */
public class ClientDownloadHandler extends Thread {
    // class atributes
    private String serverIp;
    private int serverPort;
    private DataInputStream dis;
    private String localPath;
    private int bufSize;
    private byte buffer[];

    /**
     * Creates a ClientDownloadHandler thread that will download the file sent by the server.
     * @param serverIp server ip
     * @param serverPort server port
     * @param localPath path to where the file will be downloaded
     */
    public ClientDownloadHandler(String serverIp, int serverPort, String localPath) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.localPath = localPath;
        this.bufSize = 8192; // 8KB
        this.buffer = new byte[bufSize];
        this.start();
    }

    public void run() {
        // open TCP socket
        try (Socket s = new Socket(serverIp, serverPort)) {
            dis = new DataInputStream(s.getInputStream());
            
            System.out.println("\n> Downloading file from server...");

            // re-print the directory where the client is
            if (Client.onServerDirectory)
                System.out.print("(Server) " + Client.serverDirectory + ">");
            else
                System.out.print("(Local) " + Client.localDirectory + ">");

            // receive file name from server
            String fileName = dis.readUTF();

            File file = new File(localPath + "\\" + fileName);
            FileOutputStream fos = new FileOutputStream(file);

            // start receiving file from server
            int n;
            while ((n = dis.read(buffer)) > 0) {
                //System.out.println("Read " + n + "B.");
                fos.write(buffer, 0, n);
            }

            // close socket and streams
            fos.close();
            dis.close();
            s.close();
        } catch (IOException e) {
			System.out.println("<DownloadHandler> IO: " + e.getMessage());
		}

        System.out.println("\n> Download finished...");

        // re-print the directory where the client is
        if (Client.onServerDirectory)
            System.out.print("(Server) " + Client.serverDirectory + ">");
        else
            System.out.print("(Local) " + Client.localDirectory + ">");

        return;
    }


}
