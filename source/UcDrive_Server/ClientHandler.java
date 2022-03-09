/*
 *  "ClientHandler.java"
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

package source.UcDrive_Server;
import java.net.*;

import javax.xml.namespace.QName;

import java.io.*;
import source.ClientAuth;

/**
 * Client handler server-sided class
 */
public class ClientHandler extends Thread{
    private Socket clientSocket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private boolean isAuthenticated;
    private User user;

    public ClientHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
        this.isAuthenticated = false;

        try {
            this.oos = new ObjectOutputStream(clientSocket.getOutputStream());
            oos.flush();
            this.ois = new ObjectInputStream(clientSocket.getInputStream());
            this.start();
        } catch (IOException e) {
            System.out.println("Connection:" + e.getMessage());
        }
    }

    public void run(){
        String answer;
        
        // authentication loop
        outer:
        while (true) {
            try {
                ClientAuth auth = (ClientAuth) ois.readObject();
                for(User u : UcDrive_Server.users){
                    //System.out.println(u);
                    if(u.compareAuth(auth)){
                        System.out.println("> Client " + auth.getUsername() + " authenticated.");
                        this.user = u;
                        this.isAuthenticated = true;
                        oos.writeBoolean(true);
                        oos.flush();
                        break outer;
                    }
                    
                }

                oos.writeBoolean(false);
                oos.flush();

            } catch (ClassNotFoundException e) {
                System.out.println("ClassNotFound: " + e.getMessage());
                return;
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
                return;
            }
        }

        String opt[];
        // commands loop
        while (true) {
            try{
                //send current directory to client
                oos.writeUTF(user.getClientPath());
                oos.flush();
                
                opt = ois.readUTF().split(" ");

                switch(opt[0]){
                    // list directory
                    case "ls":
                        oos.writeUTF(getFileList());
                        oos.flush();
                        break;
                    // change directory
                    case "cd":
                        oos.writeUTF(changeDirectory(opt[1]));
                        oos.flush();
                        break;
                    // change password
                    case "pw":
                        user.getClientData().setPassword(opt[1]);
                        oos.writeUTF("> Password changed.");
                        oos.flush();
                        System.out.println("> Client " + user.getClientData().getUsername() + " left.");
                        return;
                    // exit
                    case "exit":
                        System.out.println("> Client " + user.getClientData().getUsername() + " left.");
                        return;
                    default:
                        break;
                }

            } catch (EOFException e) {
                System.out.println("EOF:" + e);
                return;
            } catch (IOException e) {
                System.out.println("IO:" + e);
                return;
            }
        }
    }

    private String getFileList(){
        File f = new File(user.getCurPath());
        String files[] = f.list();
        String ls = "";
        for(String s : files){
            ls += s;
            ls += "  ";
        }
        return ls;
    }

    private String changeDirectory(String path){

        if(path.equals("..")){
            String directoryList[] = user.getCurPath().split("/");

            //restrict to home path
            if(directoryList.length < 3){
                return "> Cannot leave home folder.";
            }

            String newPath = "";
            for(int i = 0; i < directoryList.length - 1; i++){
                newPath += directoryList[i];
                if(i != directoryList.length - 2)
                    newPath += "/";
            }
            user.setCurPath(newPath);
            return "";
        }

        File f = new File(user.getCurPath());
        File files[] = f.listFiles();

        //check if directory exists
        for(File file : files){
            //file.tostring will return a path like "Users\alex\asd", we want only "asd"
            String fileNames[] = file.toString().split("\\\\");
            String fileName = fileNames[fileNames.length-1];

            //checks if the path is directory and if the name equals the desired path
            if(fileName.equals(path)){
                if(file.isDirectory()){
                    user.setCurPath(user.getCurPath() + "/" + path);
                    return "";
                }
                //found file with that name
                else
                    return "> Specified path is not a directory.";
            }
        }

        return "> Invalid path.";
    }

}