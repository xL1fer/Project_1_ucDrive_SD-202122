import java.io.*;
import java.net.*;

public class ServerDownloadHandler extends Thread{
    private ObjectInputStream ois;
    private String filePath;
    ServerSocket listenSocket;
    
    public ServerDownloadHandler(String filePath){
        // port = 0 will force operating system to search for unused ports
        try {
            this.listenSocket = new ServerSocket(0);
            this.start();
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
        }

        this.filePath = filePath;
    }

    public void run(){

        try {
            System.out.println("\n:: Upload Socket listening on port " + getPort() + " ::");
            System.out.println("UOLOAD LISTEN SOCKET=" + listenSocket);
            Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
            System.out.println("CLIENT UPLOAD SOCKET (created at accept())="+clientSocket);
            
            ois = new ObjectInputStream(clientSocket.getInputStream());

            String fileName = ois.readUTF();
            byte fileData[] = (byte[]) ois.readObject();

            File file = new File(filePath + "\\" + fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(fileData);
            
            System.out.println("CLIENT UPLOAD SOCKET CLOSING");
            clientSocket.close();
            listenSocket.close();
            fos.close();
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFound:" + e.getMessage());
        }

        //System.out.println("Up removing unused port");
        //UcDrive_Server.ports.remove(Integer.valueOf(port));
        return;

    }

    public int getPort() {
        return this.listenSocket.getLocalPort();
    }

}
