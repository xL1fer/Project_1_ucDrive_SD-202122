import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class ServerUploadHandler extends Thread{
    private DataOutputStream dos;
    private String filePath;
    ServerSocket listenSocket;
    private int bufSize;
    private byte buffer[];
    
    public ServerUploadHandler(String filePath){
        this.bufSize = 8192; //8KB
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
            System.out.println("IO:" + e.getMessage());
        }

        //System.out.println("Dl removing unused port");
        //UcDrive_Server.ports.remove(Integer.valueOf(port));
        return;
    }

    public int getPort() {
        return this.listenSocket.getLocalPort();
    }

}
