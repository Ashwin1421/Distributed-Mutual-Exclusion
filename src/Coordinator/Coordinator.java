/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Coordinator;

import Utilities.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Ashwin
 */
public class Coordinator {
    ServerSocket coordinatorSocket = null;
    int CLOCK=0;
    Socket processSocket;
    String HOST;
    int PROC_ID;
    int PORT;
    int MAX_PROC_NUM;
    int PROCESS_ID;
    Map<Socket, Integer> PROC_IDS = new HashMap<>();
    Utils prop =new Utils();
    
    
    public Coordinator(int PORT, int pid){
        this.PORT = PORT;
        this.PROC_ID = pid;
        
    }

    
    public void start(){
        try {
            coordinatorSocket = new ServerSocket(this.PORT);
            PROCESS_ID = PROC_ID;
            System.out.println("Coordinator started at ["+coordinatorSocket.getInetAddress()+"].");
            coordinatorSocket.setSoTimeout(1000*60*60);
            coordinatorSocket.setReuseAddress(true);
            
            while(true){
                processSocket = coordinatorSocket.accept();
                PROCESS_ID++;
                PROC_IDS.put(processSocket,PROCESS_ID);
                new processHandler(processSocket, PROC_IDS, CLOCK).start();
            }
            
        } catch (IOException ex) {
            Logger.getLogger(Coordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}

class processHandler extends Thread{
    static int CLOCK;
    Socket processSocket;
    BufferedReader inputReader;
    PrintWriter outputWriter;
    Map<Socket, Integer> PROCESS_IDS;
    Utils prop = new Utils();
    
    public processHandler(Socket processSocket, Map<Socket, Integer> PROCESS_IDS, int CLOCK){
        this.processSocket = processSocket;
        this.PROCESS_IDS = PROCESS_IDS;
        this.CLOCK = CLOCK;
    }

    public Socket getSocket(Integer pid){
        for(Socket s: PROCESS_IDS.keySet()){
            if(PROCESS_IDS.get(s).equals(pid)){
                return s;
            }
        }
        return null;
    }
    
    public void send(Socket process, String msg){
        try {
            PrintWriter pw = new PrintWriter(process.getOutputStream(),true);
            //Increment clock value and send message with latest timestamp.
            CLOCK++;
            msg += ",TS="+CLOCK;
            pw.println(msg);
            System.out.println("SENT_TO=["+process.getRemoteSocketAddress()+"]$:"+msg);
        } catch (IOException ex) {
            Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public void receive(Socket process, String msg){
        int TS = Integer.parseInt(msg.split("TS=")[1]);
        CLOCK = Integer.max(CLOCK, TS)+1;
        System.out.println("RECEIVED_FROM=["+process.getRemoteSocketAddress()+"]$:"+msg);
    }
    public void sendAll(){
        /**
         * Constructing a message with process id.
         **/
        String pid_msg = "PID=";
        String nb_msg = "NB=";
        for(Socket nb: PROCESS_IDS.keySet()){
            pid_msg += PROCESS_IDS.get(nb);
            for(Integer nb_pid: prop.getNBList(PROCESS_IDS.get(nb))){
                if(nb_pid == 1){
                    nb_msg += "[coordinator]"+",";
                }
                else{
                    nb_msg += "["+getSocket(nb_pid).getInetAddress().getHostName()+","+nb_pid+"]";
                }
            }
            String msg = pid_msg + ";" +nb_msg;
            send(nb, msg);
        }
    }
    
    @Override
    public void run(){
        String sendMsg, recvMsg;
        try {
            inputReader = new BufferedReader(new InputStreamReader(processSocket.getInputStream()));
            outputWriter = new PrintWriter(processSocket.getOutputStream());
            System.out.println("Connected to ["+processSocket.getRemoteSocketAddress()+"].");
            while(true){
                recvMsg = inputReader.readLine();
                if(recvMsg.startsWith("REGISTER")){
                    receive(processSocket, recvMsg);
                    if(PROCESS_IDS.size() == (prop.N-1)){
                        /**
                         * Receive N-1 register messages.
                         **/
                        sendAll();
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}