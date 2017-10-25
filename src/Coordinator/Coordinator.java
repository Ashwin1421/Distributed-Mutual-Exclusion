/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Coordinator;

import MutEx.Raymond;
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
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import Message.Message;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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
    ObjectInputStream objin;
    ObjectOutputStream objout;
    BufferedReader inputReader;
    PrintWriter outputWriter;
    Map<Socket, Integer> PROCESS_IDS;
    static Map<ObjectOutputStream, Integer> PROCESS_LIST = new HashMap<>();
    Utils prop = new Utils();
    int PID = 1;
    static SuzukiKasami sk;
    static Raymond rd;
    Message sendMsg = new Message(PID);
    Map<String, Integer> hostNames = new HashMap<>();
    
    public processHandler(Socket processSocket, Map<Socket, Integer> PROCESS_IDS){
        this.processSocket = processSocket;
        this.PROCESS_IDS = PROCESS_IDS;
    }

    public void print(String text){
        System.out.println("["+prop.Coordinator+"]$:"+text);
    }
    
    public void send(ObjectOutputStream out, Message msg){
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException ex) {
            Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        print("SENT="+msg);
    }
    
    public void receive(Message msg){
        print("RECEIVED="+msg.toString());
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
        String pid_msg = "PID";
        String nb_msg = "PHOST";
        String root_msg = "ROOT";
        if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
            for(Socket p: PROCESS_IDS.keySet()){
                hostNames.put(p.getInetAddress().getHostName()+PROCESS_IDS.get(p), PROCESS_IDS.get(p));
            }
            for(ObjectOutputStream out: PROCESS_LIST.keySet()){
                
                sendMsg.addMap(hostNames);
                sendMsg.addPID(PROCESS_LIST.get(out));
                sendMsg.setText("PID");
                send(out, sendMsg);

            }
        } else{
            for(Socket process : PROCESS_IDS.keySet()){
                sendMsg.setText("PID");
                sendMsg.addPID(PROCESS_IDS.get(process));
                Integer id = prop.getParent(PROCESS_IDS.get(process));
                for(ObjectOutputStream out: PROCESS_LIST.keySet()){
                    if(PROCESS_IDS.get(process).equals(PROCESS_LIST.get(out))){
                        if(id!=null){
                            if(id == 1){
                                hostNames.put("coordinator", id);
                                sendMsg.addMap(hostNames);
                                send(out, sendMsg);
                                hostNames.remove("coordinator", id);
                            }
                            else{
                                hostNames.put(getProcessSocket(id).getInetAddress().getHostName()+id, id);
                                sendMsg.addMap(hostNames);
                                send(out, sendMsg);
                                hostNames.remove(getProcessSocket(id).getInetAddress().getHostName()+id, id);
                            }
                        }
                    }
                }
                
            }
            
        
        }
    }
    
    @Override
    public void run(){
        Message recvMsg;
        try {
            objout = new ObjectOutputStream(processSocket.getOutputStream());
            objin = new ObjectInputStream(processSocket.getInputStream());
            
            System.out.println("Connected to ["+processSocket.getRemoteSocketAddress()+"].");
            
            
            int N = prop.interval;
            while(true){
                recvMsg = (Message)objin.readObject();
                receive(recvMsg);
                if(recvMsg.getText().equalsIgnoreCase("REGISTER")){
                    if(PROCESS_IDS.containsKey(processSocket)){
                        PROCESS_LIST.put(objout, PROCESS_IDS.get(processSocket));
                    }
                    if(PROCESS_IDS.size() == (prop.N-1)){
                        //configure();
                        sendAll();
                    }
                }
                
                if(recvMsg.getText().equalsIgnoreCase("READY")){
                    Coordinator.ready_count++;
                    if(Coordinator.ready_count == (prop.N-1)){
                        sleep();
                    }
                }
               
            }
        } catch (IOException ex) {
            Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        } 
        finally{
            System.exit(0);
        }
        
    }
}