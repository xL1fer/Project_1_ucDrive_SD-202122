import java.io.*;
import java.net.*;

public class ServerDownloadHandler extends Thread{
    private DataInputStream dis;
    private String filePath;
    ServerSocket listenSocket;
    private int bufSize;
    private byte buffer[];
    
    public ServerDownloadHandler(String filePath){
        this.bufSize = 8192;
        this.buffer = new byte[bufSize];
        this.filePath = filePath;

        // port = 0 will force operating system to search for unused ports
        try {
            this.listenSocket = new ServerSocket(0);
            this.start();
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
        }
    }

    public void run(){
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
            while((n = dis.read(buffer)) > 0){
                //System.out.println("Read " + n + "B.");
                fos.write(buffer, 0, n);
            }
            
            System.out.println("CLIENT UPLOAD SOCKET CLOSING");

            fos.close();
            clientSocket.close();
            listenSocket.close();
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
            return;
        }

        //System.out.println("Up removing unused port");
        //UcDrive_Server.ports.remove(Integer.valueOf(port));

        //send files to secondary server
        UDPPortManager udpPortSender = new UDPPortManager(UcDrive_Server.otherServerIp, UcDrive_Server.portManager, filePath, fileName);

        return;

    }

    public int getPort() {
        return this.listenSocket.getLocalPort();
    }

}
