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

package source.Client;
import java.net.*;
import java.util.Scanner;
import java.io.*;
import source.ClientAuth;

/**
 * Client class responsible for handling
 * the client application
 */
public class Client {

	public static void main(String args[]) {
        clearTerminal();

        System.out.println("              ________         __              ");
        System.out.println(" __ __   ____ \\______ \\_______|__|__  __ ____  ");
        System.out.println("|  |  \\_/ ___\\ |    |  \\_  __ \\  \\  \\/ // __ \\ ");
        System.out.println("|  |  /\\  \\___ |    `   \\  | \\/  |\\   /\\  ___/ ");
        System.out.println("|____/  \\___  >_______  /__|  |__| \\_/  \\___  >");
        System.out.println("            \\/        \\/                    \\/");
        System.out.println("\n ucDrive v0.01\n Client Application\n\n====================\n");
        
        ObjectInputStream ois;
        ObjectOutputStream oos;

        Scanner sc = new Scanner(System.in);
        
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
                    System.out.println("> Logged in as: " + username);
                    break;
                }
                else{
                    System.out.println("> Invalid username/password.");
                }
            }

            clearTerminal();

            String dir;
            String[] opt;
            String response;
            // commands loop
            while (true) {
                dir = ois.readUTF();
                
                System.out.print(dir + ">");

                opt = sc.nextLine().split(" ");

                switch(opt[0]){
                    // list directory
                    case "ls":
                        oos.writeUTF("ls");
                        oos.flush();
                        System.out.println(ois.readUTF());
                        break;
                    // change directory
                    case "cd":
                        // TODO: restrict to home folder
                        if (opt.length < 2) {
                            System.out.println("> Too few arguments.");
                            oos.writeUTF("error");
                            oos.flush();
                            break;
                        }
                        oos.writeUTF("cd " + opt[1]);
                        oos.flush();
                        response = ois.readUTF();
                        if(!response.equals(""))
                            System.out.println(response);
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
                    case "clear":
                        clearTerminal();
                        oos.writeUTF("dir");
                        oos.flush();
                        break;
                    case "exit":
                        oos.writeUTF("exit");
                        oos.flush();
                        return;
                    default:
                        System.out.println("> Command not found.");
                        oos.writeUTF("error");
                        oos.flush();
                        break;
                }

            }
        
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
        }

        sc.close();
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