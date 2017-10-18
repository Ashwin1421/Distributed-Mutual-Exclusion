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
import MutEx.SuzukiKasami;
import java.util.Random;

/**
 *
 * @author Ashwin
 */
public class Coordinator {
    ServerSocket coordinatorSocket = null;
    Socket processSocket;
    String HOST;
    int PROC_ID;
    int PORT;
    int MAX_PROC_NUM;
    int PROCESS_ID;
    Map<Socket, Integer> PROC_IDS = new HashMap<>();
    Utils prop =new Utils();
    public static int ready_count = 0;
    
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
                new processHandler(processSocket, PROC_IDS).start();
            }
            
        } catch (IOException ex) {
            Logger.getLogger(Coordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}

class processHandler extends Thread{
    Socket processSocket;
    BufferedReader inputReader;
    PrintWriter outputWriter;
    Map<Socket, Integer> PROCESS_IDS;
    static Map<PrintWriter, Integer> PROCESS_LIST = new HashMap<>();
    Utils prop = new Utils();
    
    public processHandler(Socket processSocket, Map<Socket, Integer> PROCESS_IDS){
        this.processSocket = processSocket;
        this.PROCESS_IDS = PROCESS_IDS;
    }

    public void print(String text){
        System.out.println("["+prop.Coordinator+"]$:"+text);
    }
    public void send(PrintWriter out, String msg){
        out.println(msg);
        print("SENT="+msg);
    }
    
    public void receive(String msg){
        print("RECEIVED="+msg);
    }
    
    public void configure(){
        for(Socket p: PROCESS_IDS.keySet()){
            try {
                PROCESS_LIST.put(new PrintWriter(p.getOutputStream(), true), PROCESS_IDS.get(p));
            } catch (IOException ex) {
                Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    public Socket getProcessSocket(Integer id){
        for(Socket p: PROCESS_IDS.keySet()){
            if(PROCESS_IDS.get(p).equals(id)){
                return p;
            }
        }
        return null;
    }
    
    public void sleep(){
        Random rnd = new Random(System.currentTimeMillis()+1);
        try {
            long t = rnd.nextInt(prop.t2-prop.t1)+1;
            print("Sleeping for "+t+" millis.");
            Thread.sleep(t);
        } catch (InterruptedException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void sendAll(){
        /**
         * Constructing a message with process id.
         **/
        String pid_msg = "PID=";
        String nb_msg = "PHOST=";
        String root_msg = "ROOT=";
        if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
            for(Socket p: PROCESS_IDS.keySet()){
                nb_msg += "("+p.getInetAddress().getHostName()+","+PROCESS_IDS.get(p)+")"+"/";
            }
            for(PrintWriter out: PROCESS_LIST.keySet()){
                send(out, pid_msg+PROCESS_LIST.get(out)+";"+nb_msg);
            }
        } else{
            for(Socket process : PROCESS_IDS.keySet()){
                Integer id = prop.getParent(PROCESS_IDS.get(process));
                for(PrintWriter out: PROCESS_LIST.keySet()){
                    if(PROCESS_IDS.get(process).equals(PROCESS_LIST.get(out))){
                        if(id!=null){
                            if(id == 1){
                                root_msg += "("+"coordinator"+","+id+")"+"/";
                                send(out, pid_msg+PROCESS_LIST.get(out)+";"+root_msg);
                                root_msg = "ROOT=";
                            }else{
                                root_msg += "("+getProcessSocket(id).getInetAddress().getHostName()+","+id+")"+"/";
                                send(out, pid_msg+PROCESS_LIST.get(out)+";"+root_msg);
                                root_msg = "ROOT=";
                            }
                        }
                    }
                }
            }
        }
        
    }
    
    @Override
    public void run(){
        String sendMsg, recvMsg;
        try {
            
            inputReader = new BufferedReader(new InputStreamReader(processSocket.getInputStream()));
            outputWriter = new PrintWriter(processSocket.getOutputStream(), true);
            System.out.println("Connected to ["+processSocket.getRemoteSocketAddress()+"].");
            
            
            
            while(true){
                recvMsg = inputReader.readLine();
                receive(recvMsg);

                
                if(recvMsg.startsWith("REGISTER")){
                    if(PROCESS_IDS.size() == (prop.N-1)){
                        configure();
                        sendAll();
                    }
                }
                
                if(recvMsg.startsWith("READY")){
                    Coordinator.ready_count++;
                    if(Coordinator.ready_count == (prop.N-1)){
                        sleep();
                    }
                }
                
            }
        } catch (IOException ex) {
            Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally{
            System.exit(0);
        }
        
    }
}