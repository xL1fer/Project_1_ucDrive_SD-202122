import java.io.*;
import java.net.*;

public class DownloadHandler extends Thread{
    private String serverIp;
    private int serverPort;
    private ObjectInputStream ois;
    private String localPath;

    public DownloadHandler(String serverIp, int serverPort, String localPath){
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.localPath = localPath;
        this.start();
    }

    public void run(){
        try (Socket s = new Socket(serverIp, serverPort)) {
            ois = new ObjectInputStream(s.getInputStream());
            
            //System.out.println("\n> Starting download.");

            String fileName = ois.readUTF();
            byte fileData[] = (byte[]) ois.readObject();

            File file = new File(localPath + "\\" + fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(fileData);

            //System.out.println("\n> Download finished.");
            fos.close();
            s.close();
            return;
        } catch (IOException e) {
			System.out.println("Listen:" + e.getMessage());
            return;
		} catch (ClassNotFoundException e) {
            System.out.println("ClassNotFound:" + e.getMessage());
            return;
        }

    }


}
