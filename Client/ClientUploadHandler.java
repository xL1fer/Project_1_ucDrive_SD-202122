/*
 *  "ClientUploadHandler.java"
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
 * ClientUploadHandler class responsible for handling client uploads.
 */
public class ClientUploadHandler extends Thread{
    // class atributes
    private String serverIp;
    private int serverPort;
    private DataOutputStream dos;
    private String localPath;
    private int bufSize;
    private byte buffer[];

    /**
     * Creates a ClientUploadHandler thread that will upload the file to the server.
     * @param serverIp server ip
     * @param serverPort server port
     * @param localPath path of the file to be uploaded
     */
    public ClientUploadHandler(String serverIp, int serverPort, String localPath) {
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
            dos = new DataOutputStream(s.getOutputStream());
            dos.flush();

            File file = new File(localPath);
            FileInputStream fis = new FileInputStream(file);
            String fileName = file.getName();
            
            System.out.println("\n> Uploading file to server...");

            // re-print the directory where the client is
            if (Client.onServerDirectory)
                System.out.print("(Server) " + Client.serverDirectory + ">");
            else
                System.out.print("(Local) " + Client.localDirectory + ">");

            // send file name to server
            dos.writeUTF(fileName);
            
            // start sending file to server
            int n;
            while ((n = fis.read(buffer)) > 0) {
                //System.out.println("Sent " + n + "B.");
                dos.write(buffer, 0, n);
            }
            dos.flush();

            // close socket and streams
            dos.close();
            fis.close();
            s.close();
        } catch (IOException e) {
			System.out.println("<UploadHandler> IO: " + e.getMessage());
		}

        System.out.println("\n> Upload finished...");

        // re-print the directory where the client is
        if (Client.onServerDirectory)
            System.out.print("(Server) " + Client.serverDirectory + ">");
        else
            System.out.print("(Local) " + Client.localDirectory + ">");

        return;
    }


}
