package source;

import java.io.Serializable;

public class ClientAuth implements Serializable{
    private String username;
    private String password;
    
    public ClientAuth(String username, String password){
        this.setUsername(username);
        this.setPassword(password);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String toString(){
        return "User: " + username + ", Password: " + password;
    }
}