/*
 *  "Client.java"
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
import java.util.Scanner;
import java.io.*;

/**
 * Client class responsible for handling
 * the client application
 */
public class Client {
    // class atributes
    private static Scanner sc;                                  // scanner
    private static String priServerIp;                          // primary server Ip
    private static String priServerPort;                        // primary server Port
    private static String secServerIp;                          // secondary server Ip
    private static String secServerPort;                        // secondary server Port
    private static Socket s;                                    // socket that is connected to the primary server
    private static ObjectInputStream ois;                       // used to read from sockets
    private static ObjectOutputStream oos;                      // used to write to sockets
    private static int connectedServer;                         // denotates to which server the client is connected (0 = none)
    protected static boolean onServerDirectory;                 // indicates wether the client is on the server directory (true) or on his local directory (false)
    protected static String serverDirectory;                    // string with users' current server directory 
    protected static String localDirectory;                     // string with users' current local directory 
    //static ClientDownloadHandler dHandler;

    // main function
	public static void main(String args[]) {
        clearTerminal();

        System.out.println("              ________         __              ");
        System.out.println(" __ __   ____ \\______ \\_______|__|__  __ ____  ");
        System.out.println("|  |  \\_/ ___\\ |    |  \\_  __ \\  \\  \\/ // __ \\ ");
        System.out.println("|  |  /\\  \\___ |    `   \\  | \\/  |\\   /\\  ___/ ");
        System.out.println("|____/  \\___  >_______  /__|  |__| \\_/  \\___  >");
        System.out.println("            \\/        \\/                    \\/");
        System.out.println("\n ucDrive v0.01\n Client Application\n\n====================\n");

        // some initializations
        sc = new Scanner(System.in);
        connectedServer = 0;
        onServerDirectory = true;
        localDirectory = System.getProperty("user.dir");

        //add hook to catch SIGINT
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                try {
                    oos.writeUTF("exit");
                    oos.flush();
                    s.close();
                } catch (IOException e) {
                    System.out.println("IO: " + e.getMessage());
                }
                sc.close();;
            }
        }));

        /*
        *   Start by getting each server information
        */
        getServerIp();

        /*
        *   Connect to the current primary server
        */
        // connect to server
        if (!connectToServer())
            return;

        // as soon as the user is connected, ask for authentication
        sendAuthentication();

        // at last, start handling client operations
        clientOperations();

        // close socket and scanner before exiting
        try {
            s.close();
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
        sc.close();
    }

    private static void getServerIp() {
        while(true){
            System.out.print("> Primary Server IP [localhost]: ");
            priServerIp = sc.nextLine();
            if (priServerIp.equals("")) priServerIp = "localhost";

            System.out.print("> Primary Server Port [6000]: ");
            priServerPort = sc.nextLine();
            if (priServerPort.equals("")) priServerPort = "6000";

            // using regex to determine wether or not the serverPort string is an Integer
            if (!priServerPort.matches("-?\\d+")) {
                System.out.println("> Server Port must be an integer.");
                continue;
            }
            break;
        }

        while(true){
            System.out.print("> Secondary Server IP [localhost]: ");
            secServerIp = sc.nextLine();
            if (secServerIp.equals("")) secServerIp = "localhost";

            System.out.print("> Secondary Server Port [7000]: ");
            secServerPort = sc.nextLine();
            if (secServerPort.equals("")) secServerPort = "7000";

            // using regex to determine wether or not the serverPort string is an Integer
            if (!secServerPort.matches("-?\\d+")) {
                System.out.println("> Server Port must be an integer.");
                continue;
            }
            break;
        }
    }

    // method to stablish connection with which ever is the primary server
    private static boolean connectToServer() {
        System.out.println("> Retrieving online servers...");

        // in case the client was previously already connected to a server but the connection was lost
        if (connectedServer > 0) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e2) {
                System.out.println("Interrupted: " + e2.getMessage());
                return false;
            }
        }

        System.out.println("> Attempting to connect to primary server.");
        try {
            // attempt to connect to primary server
            s = new Socket(priServerIp, Integer.parseInt(priServerPort));
            System.out.println("\n:: Successfully connected to primary server ::");

            ois = new ObjectInputStream(s.getInputStream());
            oos = new ObjectOutputStream(s.getOutputStream());
            oos.flush();
            connectedServer = 1;
        } catch (IOException e) {
            // if there's an error connecting to the primary server
            System.out.println("> Primary server offline.\n> Attempting to connect to secondary server.");

            try {
                // attempt to connect to secondary server
                s = new Socket(secServerIp, Integer.parseInt(secServerPort));
                System.out.println("\n:: Successfully connected to secondary server ::");

                ois = new ObjectInputStream(s.getInputStream());
                oos = new ObjectOutputStream(s.getOutputStream());
                oos.flush();
                connectedServer = 2;
            } catch (IOException e1) {
                // there's an error connecting to the secondary server
                // no servers online
                System.out.println("> Secondary server offline.\n> No server online, exiting.");
                return false;
            }
        }
        return true;
    }

    // user authentication method
    private static void sendAuthentication(){
        // authentication loop
        while (true) {
            try {
                // READ STRING FROM KEYBOARD
                System.out.print("> Username: "); 
                String username = sc.nextLine();

                System.out.print("> Password: "); 
                String password = sc.nextLine();

                // create client auth object that will be verified by the server
                ClientAuth auth = new ClientAuth(username, password);

                // send authentication data to server
                oos.writeObject(auth);

                // server answers with int saying if client is authenticated or not, -1 for wrong auth, 0 for already logged and 1 for success
                int opt = ois.readInt();

                if (opt == 1) {
                    System.out.println("> Logged in as: " + username + "\n");
                    break;
                }
                else if(opt == 0){
                    System.out.println("> User already logged in.");
                }
                else {
                    System.out.println("> Invalid username/password.");
                }
            } catch (IOException e) {
                System.out.println("IO: " + e.getMessage());
                
                // try to reconnect to server
                if(!connectToServer())
                    return;
            }
        }
    }

    // client operations handler function
    private static void clientOperations() {
        String[] opt;           // input split string
        String response;        // server response
        File file;              // file object used to instantiate files

        // our main loop
        while (true) {
            try {
                // we only need to request the server directory, local directory is handled by the client
                if (onServerDirectory) {
                    serverDirectory = ois.readUTF();
                    System.out.print("(Server) " + serverDirectory + ">");
                }
                else {
                    System.out.print("(Local) " + localDirectory + ">");
                }

                // get user commands
                opt = sc.nextLine().split(" ");

                // in case user enters the string " " for example
                if(opt.length == 0) {
                    oos.writeUTF("error");
                    oos.flush();
                    continue;
                }

                switch (opt[0]) {
                    // list directory
                    case "ls":
                        if (onServerDirectory) {
                            oos.writeUTF("ls");
                            oos.flush();
                            System.out.println(ois.readUTF());
                        }
                        else {
                            System.out.println(getFileList());
                        }
                        break;
                    // change directory
                    case "cd":
                        if (onServerDirectory) {
                            if (opt.length < 2) {
                                System.out.println("> Too few arguments.");
                                oos.writeUTF("error");
                                oos.flush();
                                break;
                            }
                            // joinString will join the whole string array but the first element
                            oos.writeUTF("cd " + joinString(opt));
                            oos.flush();
                            response = ois.readUTF();
                            if (!response.equals(""))
                                System.out.println(response);
                        }
                        else {
                            if (opt.length < 2) {
                                System.out.println("> Too few arguments.");
                                break;
                            }
                            System.out.println(changeDirectory(joinString(opt)));
                        }
                        break;
                    // make directory
                    case "mkdir":
                        if (onServerDirectory) {
                            if (opt.length < 2) {
                                System.out.println("> Too few arguments.");
                                oos.writeUTF("error");
                                oos.flush();
                                break;
                            }
                            // the desired directory can be a folder named "new folder", so we need to join
                            oos.writeUTF("mkdir " + joinString(opt));
                            oos.flush();
                            response = ois.readUTF();
                            if(!response.equals(""))
                                System.out.println(response);
                        }
                        else {
                            if (opt.length < 2) {
                                System.out.println("> Too few arguments.");
                                break;
                            }
                            System.out.println(createDirectory(joinString(opt)));
                        }
                        break;
                    // remove directory
                    case "rm":
                        if (onServerDirectory) {
                            if (opt.length < 2) {
                                System.out.println("> Too few arguments.");
                                oos.writeUTF("error");
                                oos.flush();
                                break;
                            }

                            System.out.print("> Are you sure you want to try to remove \"" + joinString(opt) + "\" directory? ");
                            String ans = sc.nextLine();
                            // abort rm
                            if (!ans.equals("yes") && !ans.equals("y")) {
                                System.out.println("> Remove cancelled.");
                                oos.writeUTF("dir");
                                oos.flush();
                                break;
                            }

                            // the desired directory can be a folder named "new folder", so we need to join the string
                            oos.writeUTF("rm " + joinString(opt));
                            oos.flush();
                            response = ois.readUTF();
                            if (!response.equals(""))
                                System.out.println(response);
                        }
                        else {
                            if (opt.length < 2) {
                                System.out.println("> Too few arguments.");
                                break;
                            }

                            System.out.print("> Are you sure you want to try to remove \"" + joinString(opt) + "\" directory? ");
                            String ans = sc.nextLine();
                            // abort rm
                            if (!ans.equals("yes") && !ans.equals("y")) {
                                System.out.println("> Remove cancelled.");
                                break;
                            }

                            // the desired directory can be a folder named "new folder", so we need to join
                            file = new File(localDirectory + "\\" + joinString(opt));
                            // directory not found
                            if (file.exists() == false) {
                                System.out.println("> Directory not found.");
                                break;
                            }
                            
                            deleteDir(file);
                            System.out.println("> Directory \"" + joinString(opt) + "\" deleted.");
                        }
                        break;
                    // change password
                    case "pw":
                        if (opt.length < 2) {
                            System.out.println("> Too few arguments.");
                            // we only need to flush the server when on its directory
                            if (onServerDirectory) {
                                oos.writeUTF("error");
                                oos.flush();
                            }
                            break;
                        }

                        oos.writeUTF("pw " + opt[1]);
                        oos.flush();
                        response = ois.readUTF();
                        System.out.println(response);

                        // in case the password is changed we want to stop the application in order to force the client to authenticate again
                        // this could also be done by calling "sendAuthentication()" so that the client wouldnt need to restart the app
                        return;
                    // download file from server
                    case "dw":
                        if (!onServerDirectory) {
                            System.out.println("> Cannot download from local directory.");
                            break;
                        }
                        if (opt.length < 2) {
                            System.out.println("> Too few arguments.");
                            oos.writeUTF("error");
                            oos.flush();
                            break;
                        }

                        oos.writeUTF("dw " + joinString(opt));
                        oos.flush();
                        
                        //check if server found the file
                        String res = ois.readUTF();
                        if (!res.equals("dw_start")) {
                            System.out.println(res);
                            break;
                        }

                        // check if file is already in local directory
                        file = new File(localDirectory + "\\" + joinString(opt));
                        if (file.exists()) {
                            System.out.print("> File already exists in local directory. Do you wish to download anyway? ");
                            String ans = sc.nextLine();
                            // abort download
                            if (!ans.equals("yes") && !ans.equals("y")) {
                                System.out.println("> Download cancelled.");
                                oos.writeUTF("dir");
                                oos.flush();
                                break;
                            }
                        }
                        oos.writeUTF("confirm");
                        oos.flush();

                        //server found the file and is now sending the port
                        int port = ois.readInt();
                        if (port == -1) {
                            System.out.println("> Error: Cannot download file.");
                            break;
                        }

                        // check from which server we are downloading the file
                        if (connectedServer == 1)
                            new ClientDownloadHandler(priServerIp, port, localDirectory);
                        else if (connectedServer == 2)
                            new ClientDownloadHandler(secServerIp, port, localDirectory);

                        break;
                    // upload file to server
                    case "up":
                        if (onServerDirectory) {
                            System.out.println("> Cannot upload from server directory.");
                            oos.writeUTF("error");
                            oos.flush();
                            break;
                        }
                        if (opt.length < 2) {
                            System.out.println("> Too few arguments.");
                            break;
                        }
                        file = new File(localDirectory + "\\" + joinString(opt));

                        // directory not found
                        if (file.exists() == false) {
                            System.out.println("> Directory not found.");
                            break;
                        }
                        // directory is not a file
                        if (!file.isFile()) {
                            System.out.println("> Directory is not a file.");
                            break;
                        }

                        // we need to send the file name to make the server check if it already exists
                        oos.writeUTF("up " + joinString(opt));
                        oos.flush();

                        String up_ans = ois.readUTF();
                        if (!up_ans.equals("up_start")) {
                            System.out.print(up_ans);
                            
                            up_ans = sc.nextLine();
                            // abort upload
                            if (!up_ans.equals("yes") && !up_ans.equals("y")) {
                                System.out.println("> Upload cancelled.");
                                oos.writeUTF("dir");
                                oos.flush();

                                // make a read in order to empty oos
                                ois.readUTF();

                                break;
                            }
                            oos.writeUTF("confirm");
                            oos.flush();
                        }

                        // server is sending the port
                        int up_port = ois.readInt();
                        if (up_port == -1) {
                            System.out.println("> Error: Cannot download file.");

                            // make a read in order to empty oos
                            ois.readUTF();

                            break;
                        }

                        // check to which server we are uploading the file
                        if (connectedServer == 1)
                            new ClientUploadHandler(priServerIp, up_port, localDirectory + "\\" + joinString(opt));
                        else if (connectedServer == 2)
                            new ClientUploadHandler(secServerIp, up_port, localDirectory + "\\" + joinString(opt));
                        
                        // make a read in order to empty oos
                        ois.readUTF();

                        break;
                    // clear console
                    case "clear":
                        clearTerminal();
                        // we only need to flush the server when on its directory
                        if (onServerDirectory) {
                            oos.writeUTF("dir");
                            oos.flush();
                        }
                        break;
                    // change server ip
                    case "sv":
                        // tell server this client is leaving
                        oos.writeUTF("exit");
                        oos.flush();

                        getServerIp();

                        // connect to server
                        if (!connectToServer())
                            return;

                        // as soon as the user is connected, ask for authentication
                        sendAuthentication();
                        
                        // reset directory to server directory
                        onServerDirectory = true;

                        break;
                    // exit program
                    case "exit":
                        oos.writeUTF("exit");
                        oos.flush();
                        return;
                    // change between local and server directories
                    case "ch":
                        if (onServerDirectory == false) {
                            oos.writeUTF("dir");
                            oos.flush();
                        }
                        onServerDirectory = !onServerDirectory;
                        break;
                    // ignore empty input
                    case "":
                        if (onServerDirectory) {
                            oos.writeUTF("dir");
                            oos.flush();
                        }
                        break;
                    default:
                        System.out.println("> Command not found.");
                        if (onServerDirectory) {
                            oos.writeUTF("error");
                            oos.flush();
                        }
                        break;
                }
            } catch (IOException e) {
                // try to reconnect to server
                if (!connectToServer())
                    return;

                // re-send authentication
                sendAuthentication();

                // reset directory to server directory
                onServerDirectory = true;
            }
        }
    }

    // method to get all files from a given directory (will always be localDirectory in this case)
    private static String getFileList() {
        File f = new File(localDirectory);
        String files[] = f.list();
        String ls = "";
        for (String s : files) {
            ls += s;
            ls += "  ";
        }
        return ls;
    }

    // change current directory 
    private static String changeDirectory(String path) {
        if (path.equals("..")) {
            String directoryList[] = localDirectory.split("\\\\");

            // restrict to home path
            if (directoryList.length < 3)
                return "> Cannot leave home folder.";

            // create new path without the last "/" -> "Users/my_folder" to "Users"
            String newPath = "";
            for (int i = 0; i < directoryList.length - 1; i++) {
                newPath += directoryList[i];
                if (i != directoryList.length - 2)
                    newPath += "\\";
            }

            localDirectory = newPath;
            return "";
        }

        File f = new File(localDirectory);
        File files[] = f.listFiles();

        // check if directory exists
        for (File file : files) {
            // file.toString() will return a path like "Users\alex\my_folder", we only want "my_folder"
            String fileNames[] = file.toString().split("\\\\");
            String fileName = fileNames[fileNames.length-1];

            // checks if the path is directory and if the name equals the desired path
            if (fileName.equals(path)) {
                if (file.isDirectory()) {
                    localDirectory = localDirectory + "\\" + path;
                    return "";
                }
                // found file with the suposed "directory" name
                else
                    return "> Specified path is not a directory.";
            }
        }

        return "> Invalid path.";
    }

    // function to create a directory
    private static String createDirectory(String dirName) {
        File f = new File(localDirectory + "\\" + dirName);
        //System.out.println("Dir: " + f);
        if (f.exists() == false) {
            f.mkdirs();
            // change user path to created directory
            changeDirectory(dirName);
            return "";
            //return "> Directory \"" + dirName + "\" created.";
        }
        return "> Directory already exists.";
    }

    // delete a file or all files within a folder
    private static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        if (!file.delete()) {
            System.out.println("> Could not delete file \"" + file + "\".");
        }
    }
    
    // method to join a string array except the first element
    private static String joinString(String array[]) {
        String str = "";
        for (int i = 1; i < array.length; i++) {
            str += array[i];
            if (i != array.length - 1)
                str += " ";
        }
        return str;
    }

    // clear terminal function (may only work on windows)
    private static void clearTerminal() {
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (InterruptedException e) {
            System.out.println("InterruptedException:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException:" + e.getMessage());
        }
    }
}