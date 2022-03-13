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

import java.net.*;
import java.nio.file.Files;
import java.io.*;

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
        File file;
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
                        oos.writeUTF(changeDirectory(joinString(opt)));
                        oos.flush();
                        break;
                    // make directory
                    case "mkdir":
                        oos.writeUTF(createDirectory(joinString(opt)));
                        oos.flush();
                        break;
                    // remove directory
                    case "rm":
                        file = new File(user.getCurPath() + "\\" + joinString(opt));
                        // directory not found
                        if(file.exists() == false){
                            oos.writeUTF("> Directory not found.");
                            oos.flush();
                            break;
                        }
                        
                        deleteDir(file);
                        oos.writeUTF("> Directory \"" + joinString(opt) + "\" deleted.");
                        oos.flush();
                        break;
                    // change password
                    case "pw":
                        // TODO: when changing password we need to save it on the file
                        user.getClientData().setPassword(opt[1]);
                        oos.writeUTF("> Password changed.");
                        oos.flush();
                        System.out.println("> Client " + user.getClientData().getUsername() + " left.");
                        return;
                    // download files from server
                    case "dw":
                        file = new File(user.getCurPath() + "\\" + joinString(opt));
                        // directory not found
                        if(file.exists() == false){
                            oos.writeUTF("> Directory not found.");
                            oos.flush();
                            break;
                        }
                        // directory is not a file
                        if(!file.isFile()){
                            oos.writeUTF("> Directory is not a file.");
                            oos.flush();
                            break;
                        }
                        // start download message
                        oos.writeUTF("dw start");
                        oos.flush();

                        ServerUploadHandler uHandler = new ServerUploadHandler(user.getCurPath() + "\\" + joinString(opt));
                        
                        int dl_port = uHandler.getPort();
                        oos.writeInt(dl_port);
                        oos.flush();
                        
                        break;
                    // upload files to server
                    case "up":
                        ServerDownloadHandler dHandler = new ServerDownloadHandler(user.getCurPath());
                        
                        int up_port = dHandler.getPort();
                        oos.writeInt(up_port);
                        oos.flush();
                        
                        break;
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
            String directoryList[] = user.getCurPath().split("\\\\");

            //restrict to home path
            if(directoryList.length < 3){
                return "> Cannot leave home folder.";
            }

            String newPath = "";
            for(int i = 0; i < directoryList.length - 1; i++){
                newPath += directoryList[i];
                if(i != directoryList.length - 2)
                    newPath += "\\";
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
                    user.setCurPath(user.getCurPath() + "\\" + path);
                    return "";
                }
                //found file with that name
                else
                    return "> Specified path is not a directory.";
            }
        }

        return "> Invalid path.";
    }

    private String createDirectory(String dirName){
        File f = new File(user.getCurPath() + "\\" + dirName);
        //System.out.println("Dir: " + f);
        if(f.exists() == false){
            f.mkdirs();
            // change user to created directory
            changeDirectory(dirName);
            return "";
            //return "> Directory \"" + dirName + "\" created.";
        }
        return "> Directory already exists.";
    }

    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        if (!file.delete()){
            System.out.println("> Could not delete file \"" + file + "\".");
        }
    }

    //this will remove the first index!!!!
    private static String joinString(String array[]){
        String str = "";
        for(int i = 1; i < array.length; i++){
            str += array[i];
            if(i != array.length - 1)
                str += " ";
        }
        return str;
    }

}