/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Coordinator;

import MutEx.Raymond;
import Utilities.Utils;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import MutEx.SuzukiKasami;
import java.util.Random;
import Message.Message;
import java.io.BufferedWriter;
import java.io.FileWriter;
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
            System.out.println("Coordinator started at ["+coordinatorSocket.getInetAddress()+":"+coordinatorSocket.getLocalPort()+"].");
            coordinatorSocket.setSoTimeout(1000*60*60);
            coordinatorSocket.setReuseAddress(true);
            
            while(true && PROCESS_ID<=prop.N){
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
    static BufferedWriter outToFile;
    Socket processSocket;
    ObjectInputStream objin;
    ObjectOutputStream objout;
    Map<Socket, Integer> PROCESS_IDS;
    static Map<ObjectOutputStream, Integer> PROCESS_LIST = new HashMap<>();
    Utils prop = new Utils();
    int PID = 1;
    static SuzukiKasami sk;
    static Raymond rd;
    Message sendMsg = new Message(PID);
    Map<String, Integer> hostNames = new HashMap<>();
    public static int releaseMsgCount = 0;
    public static int requestCount = 0;
    public static boolean released = false;
    static boolean startComp = false;
    static int finCount = 0;
    
    //Performance data
    static Map<Integer, Double> csInTimes = new HashMap<>();
    static Map<Integer, Double> csOutTimes = new HashMap<>();
    static Map<Integer, Integer> msgCounts = new HashMap<>();
    static Map<Integer, Double> waitTimes = new HashMap<>();
    
    
    public processHandler(Socket processSocket, Map<Socket, Integer> PROCESS_IDS){
        this.processSocket = processSocket;
        this.PROCESS_IDS = PROCESS_IDS;
    }

    public void print(String text){
        System.out.println("[p"+PID+"]$:"+text);
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
    
    public void captureResult(Message msg){
        receive(msg);
        print("Total Msg Count = "+msg.getMsgCount()+", for pid="+msg.getPid());
        print("Avg Wait time to enter CS = "+msg.getCSWaitTime()+", for pid="+msg.getPid());
        waitTimes.put(msg.getPid(), msg.getCSWaitTime());
        msgCounts.put(msg.getPid(), msg.getMsgCount());
        csInTimes.put(msg.getPid(), msg.getCSInTime());
        csOutTimes.put(msg.getPid(), msg.getCSOutTime());
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
            print("Sleeping for "+t+" ms.");
            Thread.sleep(t);
        } catch (InterruptedException ex) {
            Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void sendAll(){
        /**
         * Constructing a message with process id.
         **/
        
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
                                if(!prop.getChildren(PROCESS_IDS.get(process)).isEmpty()){
                                    for(Integer c: prop.getChildren(PROCESS_IDS.get(process))){
                                        hostNames.put(getProcessSocket(c).getInetAddress().getHostName()+c, c);
                                    }
                                }
                                sendMsg.addMap(hostNames);
                                send(out, sendMsg);
                                hostNames.remove("coordinator", id);
                                if(!prop.getChildren(PROCESS_IDS.get(process)).isEmpty()){
                                    for(Integer c: prop.getChildren(PROCESS_IDS.get(process))){
                                        hostNames.remove(getProcessSocket(c).getInetAddress().getHostName()+c, c);
                                    }
                                }
                            }
                            else{
                                hostNames.put(getProcessSocket(id).getInetAddress().getHostName()+id, id);
                                if(!prop.getChildren(PROCESS_IDS.get(process)).isEmpty()){
                                    for(Integer c: prop.getChildren(PROCESS_IDS.get(process))){
                                        hostNames.put(getProcessSocket(c).getInetAddress().getHostName()+c, c);
                                    }
                                }
                                sendMsg.addMap(hostNames);
                                send(out, sendMsg);
                                hostNames.remove(getProcessSocket(id).getInetAddress().getHostName()+id, id);
                                if(!prop.getChildren(PROCESS_IDS.get(process)).isEmpty()){
                                    for(Integer c: prop.getChildren(PROCESS_IDS.get(process))){
                                        hostNames.remove(getProcessSocket(c).getInetAddress().getHostName()+c, c);
                                    }
                                }
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
            boolean fin = false;
            
            int N = prop.interval;
            while(true){
                recvMsg = (Message)objin.readObject();
                
                if(recvMsg.getText().equalsIgnoreCase("REGISTER")){
                    receive(recvMsg);
                    if(PROCESS_IDS.containsKey(processSocket)){
                        PROCESS_LIST.put(objout, PROCESS_IDS.get(processSocket));
                        if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                            sk = new SuzukiKasami(PID, PROCESS_LIST);
                        }else{
                            rd = new Raymond(PID, PROCESS_LIST);
                        }
                    }
                    if(PROCESS_IDS.size() == (prop.N-1)){
                        //configure();
                        sendAll();
                    }
                }
                

                if(recvMsg.getText().equalsIgnoreCase("READY")){
                    receive(recvMsg);
                    Coordinator.ready_count++;
                    if(Coordinator.ready_count == (prop.N-1)){
                        print("All processes ready to start computation.");

                        for(ObjectOutputStream out: PROCESS_LIST.keySet()){
                            sendMsg = new Message(PID);
                            sendMsg.setText("START");
                            send(out, sendMsg);
                        }
                        startComp = true;
                        sleep();
                    }
                }
                
                
                if(recvMsg.getText().equalsIgnoreCase("REQUEST")){
                    receive(recvMsg);
                    if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                        //
                        sk.receiveCSRequest(recvMsg.getPid(), recvMsg.getseqno());
                    }else{
                        rd.receiveCSRequest(recvMsg.getPid());
                    }
                }
                
                if(recvMsg.getText().equalsIgnoreCase("PRIVILEGE")){
                    receive(recvMsg);
                    if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                        //
                        sk.setToken();
                        sk.updateLN(recvMsg.getLN());
                        sk.updateQ(recvMsg.getQ());
                        sk.executeCS();
                    }else{
                        rd.setToken();
                        rd.assignPrivilege();
                        rd.makeRequest();
                    } 
                }
                if(startComp){
                    if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                        if(sk.getCurrentExecCount()<=N && sk.getCurrentExecCount()>=2){
                            //
                            sleep();
                            sk.requestCS(PID);
                        }
                    }else{
                        if(rd.getCurrentExecCount() <= N){
                            sleep();
                            rd.addRequest(PID);
                            rd.assignPrivilege();
                            rd.makeRequest();
                        }
                    }
                }
                
                if(prop.Algorithm.equalsIgnoreCase("Suzuki-Kasami")){
                    if(sk.getCurrentExecCount() == (N+1)){
                        print("ok");
                    }
                }else{
                    if(rd.getCurrentExecCount() == (N+1)){
                        csInTimes.put(PID, rd.getAvgExecutionTime());
                        csOutTimes.put(PID, rd.getAvgReleaseTime());
                        msgCounts.put(PID, rd.getTotalMsgCount(PID));
                        waitTimes.put(PID, rd.getAvgWaitTime());
                        fin = true;
                    }
                }
                if(recvMsg.getText().equalsIgnoreCase("FIN")){
                    captureResult(recvMsg);
                    finCount++;
                    if(finCount == (prop.N-1)){
                        print("Completed.");
                        print("Received all Complete messages.");
                    }
                    if(fin){
                    double t1 = csInTimes.values().stream().mapToDouble(val->val).average().getAsDouble();
                    double t2 = csOutTimes.values().stream().mapToDouble(val->val).average().getAsDouble();
                    double synchDelay = Math.abs(t1-t2);
                    double mc = msgCounts.values().stream().mapToDouble(val->val).average().getAsDouble();
                    double wt = waitTimes.values().stream().mapToDouble(val->val).average().getAsDouble();
                    print("-------------------------------------");
                    print("Avg. Synch Delay = "+synchDelay);
                    print("Avg. Wait Time = "+wt);
                    print("Avg. Msg Count = "+mc);
                    for(ObjectOutputStream out : PROCESS_LIST.keySet()){
                        Message msg = new Message(PID);
                        msg.setText("TERMINATE");
                        send(out, msg);
                    }
                    print("All processes terminated gracefully.");
                    }
                    //System.exit(0);
                }
            }
        } catch (IOException ex) {
            //Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            //Logger.getLogger(processHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}