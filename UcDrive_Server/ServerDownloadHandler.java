import java.io.*;
import java.net.*;

public class ServerDownloadHandler extends Thread{
    private ObjectInputStream ois;
    private String filePath;
    int port;
    
    public ServerDownloadHandler(String filePath, int port){
        this.filePath = filePath;
        this.port = port;
        this.start();
    }

    public void run(){

        try (ServerSocket listenSocket = new ServerSocket(port)) {
            System.out.println("\n:: Upload Socket listening on port " + port + " ::");
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
        UcDrive_Server.ports.remove(Integer.valueOf(port));
        return;

    }

}
