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
import java.util.Scanner;
import java.io.*;

/**
 * Client class responsible for handling
 * the client application
 */
public class Client {
    static Scanner sc;
    static ObjectInputStream ois;
    static ObjectOutputStream oos;
    static boolean onServerDirectory;
    static String localDirectory;
	public static void main(String args[]) {
        clearTerminal();

        System.out.println("              ________         __              ");
        System.out.println(" __ __   ____ \\______ \\_______|__|__  __ ____  ");
        System.out.println("|  |  \\_/ ___\\ |    |  \\_  __ \\  \\  \\/ // __ \\ ");
        System.out.println("|  |  /\\  \\___ |    `   \\  | \\/  |\\   /\\  ___/ ");
        System.out.println("|____/  \\___  >_______  /__|  |__| \\_/  \\___  >");
        System.out.println("            \\/        \\/                    \\/");
        System.out.println("\n ucDrive v0.01\n Client Application\n\n====================\n");

        sc = new Scanner(System.in);
        onServerDirectory = true;
        localDirectory = System.getProperty("user.dir");

        System.out.print("> Server IP [localhost]: ");
        String serverIp = sc.nextLine();
        if (serverIp.equals("")) serverIp = "localhost";

        System.out.print("> Server Port [6000]: ");
        String serverPort = sc.nextLine();
        if (serverPort.equals("")) serverPort = "6000";

        // using regex to determine wether or not the serverPort string is an Integer
        if (!serverPort.matches("-?\\d+")) {
            System.out.println("> Server Port must be an integer.");
            sc.close();
            return;
        }

        try (Socket s = new Socket(serverIp, Integer.parseInt(serverPort))) {
            System.out.println("\n:: Successfully connected to server ::");
            ois = new ObjectInputStream(s.getInputStream());
            oos = new ObjectOutputStream(s.getOutputStream());
            oos.flush();

            // TODO: make so that once user can only be logged in one device at a time?
            // authentication loop
            while (true) {
                // READ STRING FROM KEYBOARD
                System.out.print("> Username: "); 
                String username = sc.nextLine();

                System.out.print("> Password: "); 
                String password = sc.nextLine();

                ClientAuth auth = new ClientAuth(username, password);

                //send authentication data to server
                oos.writeObject(auth);

                //server answers with boolean saying if client is authenticated or not
                if(ois.readBoolean()){
                    System.out.println("> Logged in as: " + username + "\n");
                    break;
                }
                else{
                    System.out.println("> Invalid username/password.");
                }
            }

            String dir;
            String[] opt;
            String response;
            // commands loop
            while (true) {
                
                if(onServerDirectory){
                    dir = ois.readUTF();
                    System.out.print("(Server) " + dir + ">");
                }
                else{
                    System.out.print("(Local) " + localDirectory + ">");
                }

                opt = sc.nextLine().split(" ");

                switch(opt[0]){
                    // list directory
                    case "ls":
                        if(onServerDirectory){
                            oos.writeUTF("ls");
                            oos.flush();
                            System.out.println(ois.readUTF());
                        }
                        else{
                            System.out.println(getFileList());
                        }
                        break;
                    // change directory
                    case "cd":
                        if(onServerDirectory){
                            if (opt.length < 2) {
                                System.out.println("> Too few arguments.");
                                oos.writeUTF("error");
                                oos.flush();
                                break;
                            }
                            //joinString will join the string but not used the first index
                            oos.writeUTF("cd " + joinString(opt));
                            oos.flush();
                            response = ois.readUTF();
                            if(!response.equals(""))
                                System.out.println(response);
                        }
                        else{
                            if(opt.length < 2){
                                System.out.println("> Too few arguments.");
                                break;
                            }
                            System.out.println(changeDirectory(joinString(opt)));
                        }
                        break;
                    // make directory
                    case "mkdir":
                        if(onServerDirectory){
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
                        else{
                            if(opt.length < 2){
                                System.out.println("> Too few arguments.");
                                break;
                            }
                            System.out.println(createDirectory(joinString(opt)));
                        }
                        break;
                    // change password
                    case "pw":
                        if (opt.length < 2) {
                            System.out.println("> Too few arguments.");
                            oos.writeUTF("error");
                            oos.flush();
                            break;
                        }
                        oos.writeUTF("pw " + opt[1]);
                        oos.flush();
                        response = ois.readUTF();
                        System.out.println(response);
                        return;
                    // clear console
                    case "clear":
                        clearTerminal();
                        oos.writeUTF("dir");
                        oos.flush();
                        break;
                    // exit program
                    case "exit":
                        oos.writeUTF("exit");
                        oos.flush();
                        return;
                    // change between local and server directories
                    case "ch":
                        if(onServerDirectory == false){
                            oos.writeUTF("dir");
                            oos.flush();
                        }
                        onServerDirectory = !onServerDirectory;
                        break;
                    // ignore empty input
                    case "":
                        if (onServerDirectory){
                            oos.writeUTF("dir");
                            oos.flush();
                        }
                        break;
                    default:
                        System.out.println("> Command not found.");
                        if (onServerDirectory){
                            oos.writeUTF("error");
                            oos.flush();
                        }
                        break;
                }

            }
        
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
        }

        sc.close();
    }

    private static String getFileList(){
        File f = new File(localDirectory);
        String files[] = f.list();
        String ls = "";
        for(String s : files){
            ls += s;
            ls += "  ";
        }
        return ls;
    }

    private static String changeDirectory(String path){

        if(path.equals("..")){
            String directoryList[] = localDirectory.split("\\\\");

            //restrict to home path
            if(directoryList.length < 3){
                return "> Cannot leave home folder.";
            }

            //create new path without hte last "/" -> "Users/asd" to "Users"
            String newPath = "";
            for(int i = 0; i < directoryList.length - 1; i++){
                newPath += directoryList[i];
                if(i != directoryList.length - 2)
                    newPath += "\\";
            }

            localDirectory = newPath;
            return "";
        }

        File f = new File(localDirectory);
        File files[] = f.listFiles();

        //check if directory exists
        for(File file : files){
            //file.tostring will return a path like "Users\alex\asd", we want only "asd"
            String fileNames[] = file.toString().split("\\\\");
            String fileName = fileNames[fileNames.length-1];

            //checks if the path is directory and if the name equals the desired path
            if(fileName.equals(path)){
                if(file.isDirectory()){
                    localDirectory = localDirectory + "\\" + path;
                    return "";
                }
                //found file with that name
                else
                    return "> Specified path is not a directory.";
            }
        }

        return "> Invalid path.";
    }

    private static String createDirectory(String dirName){
        File f = new File(localDirectory + "\\" + dirName);
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

    private static void clearTerminal(){
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (InterruptedException e) {
            System.out.println("InterruptedException:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException:" + e.getMessage());
        }
    }
}