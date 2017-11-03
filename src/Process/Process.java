/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Process;

import Message.Message;
import MutEx.Raymond;
import MutEx.SuzukiKasami;
import Utilities.Utils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    static ServerSocket nbSocket;
    static ObjectInputStream objin;
    static ObjectOutputStream objout;
    static Socket processSocket;
    String HOST;
    int PORT;
    Map<String, Integer> P_HOSTNAMES = new HashMap<>();
    Map<ObjectOutputStream, Integer> P_LIST = new HashMap<>();
    Utils prop = new Utils();
    int seqNo = 0;
    public static SuzukiKasami sk;
    public static Raymond rd;
    public static boolean sentReady = false;
    public static int releaseCount = 0;
    public static int requestCount = 0;
    Message sendMsg = new Message(null);
    
    public Process(String HOST, int PORT){
        this.HOST = HOST;
        this.PORT = PORT;
    }
   
    public void print(String text){
        if(PID==0){
            System.out.println("[p]$:"+text);
        }else{
            System.out.println("[p"+PID+"]$:"+text);
        }
    }
    
    public void sendTo(Integer pid, Message msg){
        if(pid != PID){
            for(ObjectOutputStream out: P_LIST.keySet()){
                if(P_LIST.get(out).equals(pid)){
                    send(out, msg);
                }
            }
        }else{
            System.out.println("Cannot send to the same process!");
        }
    }
    
    public void send(ObjectOutputStream out, Message msg){
        try {
            out.writeObject(msg);
            out.flush();
            
        } catch (IOException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
        print("SENT="+msg.toString());
    }
    
    public void receive(Message msg) {
        print("RECEIVED="+msg.toString());
    }
    
    public void configure(){
        if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
            if(objout!=null){
                P_LIST.put(objout, 1);
            }
            for(String hostName: P_HOSTNAMES.keySet()){
                try {
                    Socket pSocket = new Socket(hostName.split("\\d*$")[0], NB_PORT+P_HOSTNAMES.get(hostName));
                    P_LIST.put(new ObjectOutputStream(pSocket.getOutputStream()),P_HOSTNAMES.get(hostName));
                } catch (IOException ex) {
                    Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }else{
            for(String hostName: P_HOSTNAMES.keySet()){
                if(hostName.equalsIgnoreCase("coordinator")){
                    P_LIST.put(objout, 1);
                }
                else{
                    try {
                        Socket pSocket = new Socket(hostName.split("\\d*$")[0], NB_PORT+P_HOSTNAMES.get(hostName));
                        P_LIST.put(new ObjectOutputStream(pSocket.getOutputStream()), P_HOSTNAMES.get(hostName));
                    } catch (IOException ex) {
                        Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }
    
    public void sleep(){
        Random rnd = new Random(System.currentTimeMillis()+PID);
        try {
            long t = rnd.nextInt(prop.t2-prop.t1)+1;
            print("Sleeping for "+t+" ms.");
            Thread.sleep(t);
        } catch (InterruptedException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void sendAll(Message msg){
        for(ObjectOutputStream out: P_LIST.keySet()){
            if(!P_LIST.get(out).equals(PID) && !P_LIST.get(out).equals(1)){
                send(out, msg);
            }
        }
    }
    

    public void  start(){
        int N = prop.interval;
        boolean released = false;

        synchronized(this){
        try {
            processSocket = new Socket(HOST, PORT);
            print("Process started at ["+processSocket.getLocalSocketAddress()+"]");
            print("Connection established.");
            objout = new ObjectOutputStream(processSocket.getOutputStream());
            objin = new ObjectInputStream(processSocket.getInputStream());
            
            
            sendMsg.setText("REGISTER");
            send(objout, sendMsg);

            while(true){
                Message recvMsg = (Message)objin.readObject();
                
                if(recvMsg.getText().startsWith("PID")){
                    
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
                    receive(recvMsg);
                    PID = recvMsg.receivepid();
                    sendMsg.setownpid(PID);
                    P_HOSTNAMES = recvMsg.getHostnames();
                    
                    nbSocket = new ServerSocket(NB_PORT+PID);
                    nbSocket.setSoTimeout(1000*3600);
                    nbSocket.setReuseAddress(true);
                    print("Listening on PORT="+(NB_PORT+PID));
                    configure();
                    if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                        sk = new SuzukiKasami(PID, P_LIST);
                    }else{
                        rd = new Raymond(PID, P_LIST);
                    }
                    if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                        for(Integer pid: P_HOSTNAMES.values()){
                            Socket nb = nbSocket.accept();
                            new neighbourHandler(nb, PID, P_LIST).start();
                        }
                    }else{
                        if(!prop.getChildren(PID).isEmpty()){
                            for(Integer pid: prop.getChildren(PID)){
                                Socket nb = nbSocket.accept();
                                new neighbourHandler(nb, PID, P_LIST).start();
                            }
                            
                        }
                        if(prop.getParent(PID)!=null && prop.getParent(PID)!=1){
                            Socket nb = nbSocket.accept();
                            new neighbourHandler(nb, PID, P_LIST).start();
                        }
                    }
                    sendMsg = new Message(PID);
                    sendMsg.setText("READY");
                    send(objout, sendMsg);
                    sentReady = true;
                }
                //simulate(N, recvMsg, released);
                if(recvMsg.getText().equalsIgnoreCase("START") && sentReady){
                    receive(recvMsg);
                    sleep();
                    if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                        sk.requestCS(PID);
                    }else{
                        rd.requestCS(PID);
                    }
                }
                
                
                if(recvMsg.getText().equalsIgnoreCase("REQUEST")){
                    receive(recvMsg);
                    if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                        sk.receiveCSRequest(recvMsg.getPid(), recvMsg.getseqno());
                    }else{
                        rd.receiveCSRequest(recvMsg.getPid());
                    }
                }
                
                if(recvMsg.getText().equalsIgnoreCase("PRIVILEGE")){
                    receive(recvMsg);
                    if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                        sk.setToken(true);
                        sk.executeCS();
                        sk.releaseCS(PID);
                    }else{
                        rd.setToken();
                        rd.assignPrivilege();
                    }
                }
                if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                    if(sk.getCurrentExecCount() <= N && sk.getCurrentExecCount()>=2){
                        sk.requestCS(PID);
                    }
                }else{
                    if(rd.getCurrentExecCount() <= N && rd.getCurrentExecCount()>=2){
                        sleep();
                        rd.requestCS(PID);
                    }
                }

                if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                    if(sk.getCurrentExecCount() == (N+1)){
                        print("Completed");
                    }
                }else{
                    if(rd.getCurrentExecCount() == (N+1)){
                        print("Completed.");
                        
                    }
                }
            }
        
        } catch (IOException ex) {
            //Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            //Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
    }
}

class neighbourHandler extends Thread{
    Socket neighbourSocket;
    ObjectInputStream objin;
    ObjectOutputStream objout;
    String HOST;
    int PORT;
    int PID;
    Map<ObjectOutputStream, Integer> P_LIST;
    Utils prop = new Utils();
    public static int releaseCount = 0;
    public static int requestCount = 0;
    
    public neighbourHandler(Socket neighbourSocket, int PID, Map<ObjectOutputStream, Integer> P_LIST){
        this.neighbourSocket = neighbourSocket;
        this.PID = PID;
        this.P_LIST = P_LIST;
    }
    
    public void print(String txt){
        System.out.println("[p"+PID+"]$:"+txt);
    }
    
    public void receive(Message msg){
        print("RECEIVED="+msg.toString());
    }
    
    public void sleep(){
        Random rnd = new Random(System.currentTimeMillis()+PID);
        try {
            long t = rnd.nextInt(prop.t2-prop.t1)+1;
            print("Sleeping for "+t+" ms.");
            Thread.sleep(t);
        } catch (InterruptedException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run(){
        synchronized(this){
        boolean released = false;
        try {
            objin = new ObjectInputStream(neighbourSocket.getInputStream());
            int N = prop.interval;
            while(true){
                
                Message recvMsg = (Message)objin.readObject();
                receive(recvMsg);
                //Accepting a request.
                if(recvMsg.getText().equalsIgnoreCase("REQUEST")){
                    if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                        Process.sk.receiveCSRequest(recvMsg.getPid(), recvMsg.getseqno());
                    }else{
                        Process.rd.receiveCSRequest(recvMsg.getPid());
                    }
                }
                
                //Accepting Token and assigning it.
                if(recvMsg.getText().equalsIgnoreCase("PRIVILEGE")){
                    if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                        Process.sk.setToken(true);
                        Process.sk.executeCS();
                        Process.sk.releaseCS(PID);
                    }else{
                        Process.rd.setToken();
                        Process.rd.assignPrivilege();
                        
                    }
                }
                
                if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                    if(Process.sk.getCurrentExecCount() <= N && Process.sk.getCurrentExecCount()>=2){
                        Process.sk.requestCS(PID);
                    }
                }else{
                    if(Process.rd.getCurrentExecCount() <= N && Process.rd.getCurrentExecCount()>=2){
                        sleep();
                        Process.rd.requestCS(PID);
                    }
                }

                if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                    if(Process.sk.getCurrentExecCount() == (N+1)){
                        print("Completed.");
                    }
                }else{
                    if(Process.rd.getCurrentExecCount() == (N+1)){
                        print("Completed.");
                    }
                }
            }
        } catch (IOException ex) {
            //Logger.getLogger(neighbourHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            //Logger.getLogger(neighbourHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
    }
}