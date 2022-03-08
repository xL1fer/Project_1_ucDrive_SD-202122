package source.UcDrive_Server;
import java.io.File;
import java.io.Serializable;
import source.ClientAuth;

public class User implements Serializable{
    private ClientAuth clientData;
    private String curPath;

    public User(ClientAuth auth){
        clientData = auth;
        curPath = "Users/" + clientData.getUsername();
        createDirectory();
    }

    public boolean compareAuth(ClientAuth auth){
        if(clientData.getUsername().equals(auth.getUsername()) && clientData.getPassword().equals(auth.getPassword()))
            return true;
        return false;
    }

    public void createDirectory(){
        File f = new File("Users/"+ clientData.getUsername());
        if(f.exists() == false){
            f.mkdirs();
        }
    }

    public ClientAuth getClientData(){
        return clientData;
    }

    public String getCurPath(){
        return curPath;
    }

    public void setCurPath(String newPath){
        this.curPath = newPath;
    }

    public String getClientPath(){
        return curPath.substring(6);
    }

    public String toString(){
        return "User: " + clientData.getUsername() + ", Password: " + clientData.getPassword();
    }
}
