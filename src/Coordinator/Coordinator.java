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

    public Socket getSocket(Integer pid){
        for(Socket s: PROCESS_IDS.keySet()){
            if(PROCESS_IDS.get(s).equals(pid)){
                return s;
            }
        }
        return null;
    }
    
    public void send(PrintWriter out, String msg){
        out.println(msg);
        System.out.println("SENT="+msg);
    }
    
    public void receive(String msg){
        System.out.println("RECEIVED="+msg);
    }
    
    public void configure(){
        if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
            for(Socket p: PROCESS_IDS.keySet()){
                try {
                    PROCESS_LIST.put(new PrintWriter(p.getOutputStream(), true), PROCESS_IDS.get(p));
                } catch (IOException ex) {
                    Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }else{
            for(Socket p: PROCESS_IDS.keySet()){
                for(Integer nb_pid: prop.getNBList(1)){
                    if(PROCESS_IDS.get(p).equals(nb_pid)){
                        try {
                            PROCESS_LIST.put(new PrintWriter(p.getOutputStream(), true), nb_pid);
                        } catch (IOException ex) {
                            Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
    }
    
    public void sendAll(){
        /**
         * Constructing a message with process id.
         **/
        String pid_msg = "PID=";
        String nb_msg = "PHOST=";
        
        if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
            for(Socket p: PROCESS_IDS.keySet()){
                nb_msg += "("+p.getInetAddress().getHostName()+","+PROCESS_IDS.get(p)+")"+"/";
            }
        } else{
            for(Socket p: PROCESS_IDS.keySet()){
                for(Integer nb_pid: prop.getNBList(PROCESS_IDS.get(p))){
                    if(nb_pid == 1){
                        nb_msg += "coordinator"+"/";
                    } else{
                        nb_msg += "("+p.getInetAddress().getHostName()+","+nb_pid+")"+"/";
                    }
                }
                nb_msg = "PHOST=";
            }
        }
        for(PrintWriter out: PROCESS_LIST.keySet()){
            send(out, pid_msg+PROCESS_LIST.get(out)+";"+nb_msg);
        }
    }
    
    @Override
    public void run(){
        String sendMsg, recvMsg;
        try {
            
            inputReader = new BufferedReader(new InputStreamReader(processSocket.getInputStream()));
            outputWriter = new PrintWriter(processSocket.getOutputStream(), true);
            System.out.println("Connected to ["+processSocket.getRemoteSocketAddress()+"].");
            
            
            int ready_count = 0;
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
                    
                    System.out.println(processSocket.getRemoteSocketAddress());
                    ready_count++;
                    if(ready_count == (prop.N-1)){
                        //break;
                        
                    }
                }
                
                
                if(recvMsg.startsWith("REQUEST_CS")){
                    
                    SuzukiKasami sk = new SuzukiKasami(1, PROCESS_LIST);
                    sk.setToken(true);
                    int pid = Integer.parseInt(recvMsg.split("=")[1].split(";")[0]);
                    int RNp = Integer.parseInt(recvMsg.split("=")[1].split(";")[1]);
                    sk.receiveCSRequest(pid, RNp);
                    System.out.println(sk.hasToken());
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