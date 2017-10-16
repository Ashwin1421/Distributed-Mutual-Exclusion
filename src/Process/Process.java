/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Process;

import MutEx.SuzukiKasami;
import Utilities.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ashwin
 */
public class Process {
    //Offset port number along with process' own pid, to make every process'
    //server socket to be different on different machines.
    int NB_PORT = 1400;
    int PID;
    ServerSocket nbSocket;
    BufferedReader inputReader;
    PrintWriter outputWriter;
    Socket processSocket;
    String HOST;
    int PORT;
    Map<String, Integer> P_HOSTNAMES = new HashMap<>();
    Map<PrintWriter, Integer> P_LIST = new HashMap<>();
    Utils prop = new Utils();
    int seqNo = 0;
    
    
    public Process(String HOST, int PORT){
        this.HOST = HOST;
        this.PORT = PORT;
    }
    
    public void sendTo(Integer pid, String msg){
        if(pid != PID){
            for(PrintWriter out: P_LIST.keySet()){
                if(P_LIST.get(out).equals(pid)){
                    send(out, msg);
                }
            }
        }else{
            System.out.println("Cannot send to the same process!");
        }
    }
    
    public void send(PrintWriter out, String msg){
        out.println(msg);
        System.out.println("SENT="+msg);
    }
    
    public void receive(String msg) {
        System.out.println("RECEIVED="+msg);
    }
    
    public void configure(){
        if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
            if(outputWriter!=null){
                P_LIST.put(outputWriter, 1);
            }
            for(String hostName: P_HOSTNAMES.keySet()){
                try {
                    Socket pSocket = new Socket(hostName.substring(0, hostName.length()-1), NB_PORT+P_HOSTNAMES.get(hostName));
                    P_LIST.put(new PrintWriter(pSocket.getOutputStream(),true),P_HOSTNAMES.get(hostName));
                } catch (IOException ex) {
                    Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }else{
            for(String hostName: P_HOSTNAMES.keySet()){
                for(Integer pid: prop.getNBList(PID)){
                    if(P_HOSTNAMES.get(hostName).equals(pid)){
                        try {
                            Socket pSocket = new Socket(hostName.substring(0, hostName.length()-1), NB_PORT+pid);
                            P_LIST.put(new PrintWriter(pSocket.getOutputStream(), true), pid);
                        } catch (IOException ex) {
                            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
    }
    
    public void sleep(){
        Random rnd = new Random(System.currentTimeMillis()+PID);
        try {
            long t = rnd.nextInt(prop.t2-prop.t1)+1;
            System.out.println("Sleeping for "+t+" millis.");
            Thread.sleep(t);
        } catch (InterruptedException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void sendAll(String msg){
        for(PrintWriter out: P_LIST.keySet()){
            send(out, msg);
        }
    }
    public void start(){
        String sendMsg, recvMsg;
        try {
            processSocket = new Socket(HOST, PORT);
            System.out.println("Started PROCESS at ["+processSocket.getLocalSocketAddress()+"].");
            System.out.println("Connected to Coordinator.");
            inputReader = new BufferedReader(new InputStreamReader(processSocket.getInputStream()));
            outputWriter = new PrintWriter(processSocket.getOutputStream(),true);
            
            SuzukiKasami sk = null;
            sendMsg = "REGISTER";
            send(outputWriter, sendMsg);
            while(true){
                recvMsg = inputReader.readLine();
                receive(recvMsg);
                if(recvMsg.startsWith("PID")){
                    /**
                     * This is part of the initial setup of
                     * processes in the distributed system.
                     * Everyone knows the coordinator, but doesn't
                     * have any clue about the other process's
                     * host names, so the coordinator shall reply 
                     * to each process with the pid and a list of
                     * host names. 
                     * In case of Suzuki-Kasami Token based algorithm,
                     * the distributed system is a fully connected graph.
                     * Therefore each process will have every other 
                     * process's host name.
                     * In Raymond's algorithm, the coordinator 
                     * will reply back with only those host names 
                     * which are neighbours of the registering process.
                     **/
                    PID = Integer.parseInt(recvMsg.split("PID=")[1].split(";")[0]);
                    for(String s: recvMsg.split("PHOST=")[1].split("/")){
                        if(!s.startsWith("coordinator")){
                            String hostName = s.split("[(\\,\\)]")[1];
                            Integer pid = Integer.parseInt(s.split("[(\\,\\)]")[2]);
                            if(pid!=PID){
                                P_HOSTNAMES.put(hostName+pid, pid);
                            }
                        }
                    }
                    nbSocket = new ServerSocket(NB_PORT+PID);
                    nbSocket.setSoTimeout(1000*3600);
                    nbSocket.setReuseAddress(true);
                    configure();
                    sk = new SuzukiKasami(PID, P_LIST);
                    for(Integer pid: P_HOSTNAMES.values()){
                        Socket nb = nbSocket.accept();
                        new neighbourHandler(nb, pid, P_LIST).start();
                    }
                    
                    sendMsg = "READY";
                    send(outputWriter, sendMsg);
                    
                    if(PID == 2){
                        sleep();
                        sk.requestCS(PID);
                    }
                    
                }
                
                if(recvMsg.startsWith("GRANT_CS")){
                    sk.setToken(true);
                    sk.executeCS();
                }
                
                
            }
            
        } catch (IOException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally{
            System.exit(0);
        }
    }
}

class neighbourHandler extends Thread{
    Socket neighbourSocket;
    BufferedReader inputReader;
    PrintWriter outputWriter;
    String HOST;
    int PORT;
    int PID;
    Map<PrintWriter, Integer> P_LIST;
    
    public neighbourHandler(Socket neighbourSocket, int PID, Map<PrintWriter, Integer> P_LIST){
        this.neighbourSocket = neighbourSocket;
        this.PID = PID;
        this.P_LIST = P_LIST;
    }
    
    public void send(PrintWriter out,String msg){
        out.println(msg);
        System.out.println("SENT="+msg);
    }
    
    
    @Override
    public void run(){
        String sendMsg, recvMsg;
        try {
            inputReader = new BufferedReader(new InputStreamReader(neighbourSocket.getInputStream()));
            outputWriter = new PrintWriter(neighbourSocket.getOutputStream(), true);
            
            while(true){
                recvMsg = inputReader.readLine();
                System.out.println("RECEIVED="+recvMsg);
                
            }
        } catch (IOException ex) {
            Logger.getLogger(neighbourHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}