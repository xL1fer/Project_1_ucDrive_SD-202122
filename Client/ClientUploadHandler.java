import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class ClientUploadHandler extends Thread{
    private String serverIp;
    private int serverPort;
    private ObjectOutputStream oos;
    private String localPath;

    public ClientUploadHandler(String serverIp, int serverPort, String localPath){
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.localPath = localPath;
        this.start();
    }

    public void run(){
        try (Socket s = new Socket(serverIp, serverPort)) {
            oos = new ObjectOutputStream(s.getOutputStream());
            oos.flush();
            
            File file = new File(localPath);
            String fileName = file.getName();
            //get byte array with data
            byte fileData[] = Files.readAllBytes(file.toPath());

            oos.writeUTF(fileName);
            oos.writeObject(fileData);
            oos.flush();

            s.close();
        } catch (IOException e) {
			System.out.println("Listen:" + e.getMessage());
		}

        return;
    }


}
