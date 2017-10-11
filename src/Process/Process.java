/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ashwin
 */
public class Process {
    static int CLOCK = 0;
    ServerSocket nbSocket;
    BufferedReader inputReader;
    PrintWriter outputWriter;
    Socket processSocket;
    String HOST;
    int PORT;
    Map<String, Integer> NN_HOSTNAMES = new HashMap<>();
    
    public Process(String HOST, int PORT){
        this.HOST = HOST;
        this.PORT = PORT;
    }
    
    public void send(PrintWriter out, String msg){
        CLOCK++;
        msg += ",TS="+CLOCK;
        out.println(msg);
        System.out.println("SENT="+msg);
    }
    
    public void receive(String msg){
        int TS = Integer.parseInt(msg.split("TS=")[1]);
        CLOCK = Integer.max(CLOCK, TS)+1;
        System.out.println("RECEIVED="+msg);
    }
    public void start(){
        String sendMsg, recvMsg;
        try {
            processSocket = new Socket(HOST, PORT);
            System.out.println("Started PROCESS at ["+processSocket.getLocalSocketAddress()+"].");
            System.out.println("Connected to Coordinator.");
            inputReader = new BufferedReader(new InputStreamReader(processSocket.getInputStream()));
            outputWriter = new PrintWriter(processSocket.getOutputStream(),true);
            
            sendMsg = "REGISTER";
            send(outputWriter, sendMsg);
            while(true){
                recvMsg = inputReader.readLine();
                receive(recvMsg);
            }
            
        } catch (IOException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
