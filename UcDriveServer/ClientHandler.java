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
public class ClientHandler extends Thread {
    private Socket clientSocket;            // socket to handle the client this thread is responsible with
    private ObjectInputStream ois;          // object input stream to receive data from client
    private ObjectOutputStream oos;         // object output stream to send data to client
    private boolean isAuthenticated;        // indicates if user is authencicated
    private User user;                      // user instance

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.isAuthenticated = false;

        try {
            this.oos = new ObjectOutputStream(clientSocket.getOutputStream());
            oos.flush();
            this.ois = new ObjectInputStream(clientSocket.getInputStream());
            this.start();
        } catch (IOException e) {
            System.out.println("<ClientHandler> IO: " + e.getMessage());
        }
    }

    public void run() {
        // authentication loop
        outer:
        while (true) {
            try {
                ClientAuth auth = (ClientAuth) ois.readObject();
                for (User u : UcDriveServer.users) {
                    //System.out.println(u);
                    if (u.compareAuth(auth)) {

                        //check if user already logged
                        if(u.getLogged()){
                            System.out.println("<ClientHandler> Client " + auth.getUsername() + " is already logged.");
                            oos.writeInt(0); // 0 for already logged
                            oos.flush();
                            continue outer;
                        }

                        System.out.println("<ClientHandler> Client " + auth.getUsername() + " authenticated");
                        this.user = u;
                        this.isAuthenticated = true;
                        this.user.setLogged(true);
                        oos.writeInt(1); // 1 for success
                        oos.flush();
                        break outer;
                    }
                    
                }

                oos.writeInt(-1); // -1 for wrong name and/or password
                oos.flush();

            } catch (ClassNotFoundException e) {
                System.out.println("<ClientHandler> ClassNotFound: " + e.getMessage());
                return;
            } catch (IOException e) {
                System.out.println("<ClientHandler> IO: " + e.getMessage());
                return;
            }
        }

        File file;
        String dirPath, curPath, res, opt[];
        // commands loop
        while (true) {
            try {
                //send current directory to client
                oos.writeUTF(user.getClientPath());
                oos.flush();

                opt = ois.readUTF().split(" ");

                switch (opt[0]) {
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
                        dirPath = joinString(opt);
                        curPath = user.getCurPath();
                        oos.writeUTF(createDirectory(dirPath));
                        oos.flush();
                        
                        //send information to secondary server to create dir
                        UDPPortManager.addFileTransfer(2, curPath + "\\" + dirPath, "");
                        new UDPPortManager(UcDriveServer.otherServerIp, UcDriveServer.portManager, true);

                        break;
                    // remove directory
                    case "rm":
                        dirPath = joinString(opt);
                        curPath = user.getCurPath();
                        file = new File(user.getCurPath() + "\\" + joinString(opt));
                        // directory not found
                        if (file.exists() == false) {
                            oos.writeUTF("> Directory not found.");
                            oos.flush();
                            break;
                        }

                        UDPPortManager.addFileTransfer(3, curPath + "\\" + dirPath, "");
                        new UDPPortManager(UcDriveServer.otherServerIp, UcDriveServer.portManager, true);
                        
                        deleteDir(file);
                        oos.writeUTF("> Directory \"" + joinString(opt) + "\" deleted.");
                        oos.flush();
                        break;
                    // change password
                    case "pw":
                        user.getClientData().setPassword(opt[1]);
                        oos.writeUTF("> Password changed.");
                        oos.flush();
                        System.out.println("> Client " + user.getClientData().getUsername() + " left.");
                        UcDriveServer.saveUsers();
                        return;
                    // download files from server
                    case "dw":
                        file = new File(user.getCurPath() + "\\" + joinString(opt));
                        // directory not found
                        if (file.exists() == false) {
                            oos.writeUTF("> Directory not found.");
                            oos.flush();
                            break;
                        }
                        // directory is not a file
                        if (!file.isFile()) {
                            oos.writeUTF("> Directory is not a file.");
                            oos.flush();
                            break;
                        }
                        // start download message
                        oos.writeUTF("dw_start");
                        oos.flush();

                        res = ois.readUTF();
                        // abort download
                        if (!res.equals("confirm")) {
                            break;
                        }

                        ServerUploadHandler uHandler = new ServerUploadHandler(user.getCurPath() + "\\" + joinString(opt));
                        
                        int dl_port = uHandler.getPort();
                        oos.writeInt(dl_port);
                        oos.flush();
                        
                        break;
                    // upload files to server
                    case "up":
                        // check if file is already in server directory
                        file = new File(user.getCurPath() + "\\" + joinString(opt));
                        if (file.exists()) {
                            oos.writeUTF("> File already exists in server directory. Do you wish to upload anyway? ");
                            oos.flush();

                            res = ois.readUTF();
                            // abort upload
                            if (!res.equals("confirm")) {
                                break;
                            }
                        }
                        else {
                            oos.writeUTF("up_start");
                            oos.flush();
                        }

                        ServerDownloadHandler dHandler = new ServerDownloadHandler(user.getCurPath());
                        
                        int up_port = dHandler.getPort();
                        oos.writeInt(up_port);
                        oos.flush();
                        
                        break;
                    // exit
                    case "exit":
                        user.setLogged(false);
                        System.out.println("> Client " + user.getClientData().getUsername() + " left.");
                        return;
                    default:
                        break;
                }

            } catch (EOFException e) {
                System.out.println("<ClientHandler> EOF: " + e);
                return;
            } catch (IOException e) {
                System.out.println("<ClientHandler> IO: <ClientHandler> " + e);
                return;
            }
        }
    }

    // get all files from user server directory
    private String getFileList() {
        File f = new File(user.getCurPath());
        String files[] = f.list();
        String ls = "";
        for (String s : files) {
            ls += s;
            ls += "  ";
        }
        return ls;
    }

    // change user server directory
    private String changeDirectory(String path) {
        if (path.equals("..")) {
            String directoryList[] = user.getCurPath().split("\\\\");

            //restrict to home path
            if (directoryList.length < 4) {
                return "> Cannot leave home folder.";
            }

            String newPath = "";
            for (int i = 0; i < directoryList.length - 1; i++) {
                newPath += directoryList[i];
                if (i != directoryList.length - 2)
                    newPath += "\\";
            }
            user.setCurPath(newPath);
            return "";
        }

        File f = new File(user.getCurPath());
        File files[] = f.listFiles();

        //check if directory exists
        for (File file : files) {
            //file.tostring will return a path like "Users\alex\asd", we want only "asd"
            String fileNames[] = file.toString().split("\\\\");
            String fileName = fileNames[fileNames.length-1];

            //checks if the path is directory and if the name equals the desired path
            if (fileName.equals(path)) {
                if (file.isDirectory()) {
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

    private String createDirectory(String dirName) {
        //System.out.println("DirName: " + user.getCurPath() + "\\" + dirName);
        File f = new File(user.getCurPath() + "\\" + dirName);
        //System.out.println("Dir: " + f);
        if (f.exists() == false) {
            f.mkdirs();
            // change user to created directory
            changeDirectory(dirName);
            return "";
            //return "> Directory \"" + dirName + "\" created.";
        }
        return "> Directory already exists.";
    }

    // delete specified file / directory
    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        if (!file.delete())
            System.out.println("> Could not delete file \"" + file + "\".");
    }

    // function to join a string array except the first element
    private static String joinString(String array[]) {
        String str = "";
        for (int i = 1; i < array.length; i++) {
            str += array[i];
            if (i != array.length - 1)
                str += " ";
        }
        return str;
    }

}