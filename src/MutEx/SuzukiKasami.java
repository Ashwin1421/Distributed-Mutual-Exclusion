/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MutEx;

import Message.Message;
import Utilities.Utils;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ashwin
 */
public class SuzukiKasami implements Serializable{
    Integer[] RN;
    Integer[] LN;
    int pid;
    
    boolean token = false;
    boolean insideCS = false;
    boolean requesting = false;
    boolean havePrivilege = false;
    boolean finished = false;
    
    Integer executionCount = 1;
    long t1 = 0;
    long t2 = 0;
    long t3 = 0;
    Map<ObjectOutputStream, Integer> PROCESS_LIST;
    Queue<Integer> REQUEST_Q = new ConcurrentLinkedQueue<>();
    Utils prop = new Utils();
    Date time = new Date();
    
    //Performance data
    Integer[] msgCount;
    Queue<Long> t1List = new LinkedList<>();
    Queue<Long> t2List = new LinkedList<>();
    Queue<Long> t3List = new LinkedList<>();
    
    
    public SuzukiKasami(int pid, Map<ObjectOutputStream, Integer> PROCESS_LIST){
        this.PROCESS_LIST = PROCESS_LIST;
        this.pid = pid;
        this.RN = new Integer[prop.N+1];
        this.LN = new Integer[prop.N+1];
        Arrays.fill(RN, -1);
        Arrays.fill(LN, -1);
        if(pid == 1){
            token = true;
            havePrivilege = true;
        }
        this.msgCount = new Integer[prop.N+1];
        Arrays.fill(msgCount,0);
    }
    
    public void print(String s){
        System.out.println("[MutEx]$:"+s);
    }
    public boolean hasToken(){
        return token;
    }
    
    public boolean hasPrivilege(){
        return havePrivilege;
    }
    
    public void sendTo(Integer pid, Message msg){
        for(ObjectOutputStream out : PROCESS_LIST.keySet()){
            if(PROCESS_LIST.get(out).equals(pid)){
                try {
                    out.writeObject(msg);
                    out.flush();
                    print("SENT:"+msg.toString());
                } catch (IOException ex) {
                    Logger.getLogger(SuzukiKasami.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    public double getAvgExecutionTime(){
        return t1List.stream().mapToDouble(val->val).average().getAsDouble()/prop.interval;
    }
    
    public double getAvgReleaseTime(){
        return t2List.stream().mapToDouble(val->val).average().getAsDouble()/prop.interval;
    }
    
    public double getAvgWaitTime(){
        double d1 = t1List.stream().mapToDouble(val->val).average().getAsDouble();
        double d2 = t3List.stream().mapToDouble(val->val).average().getAsDouble();
        return Math.abs(d1-d2)/prop.interval;
    }
    
    public long getSynchDelay(long t1, long t2){
        return t1 > t2 ? t1-t2 : t2-t1;
    }
    

    public Integer nextRequest(){
        return REQUEST_Q.peek();
    }
    
    public Integer getCurrentExecCount(){
        return executionCount;
    }
    
    public void setToken(){
        this.token = true;
        this.havePrivilege = true;
    }
    
    public void updateQ(Queue Q){
        REQUEST_Q.addAll(Q);
    }
    
    public void updateLN(Integer[] LN){
        this.LN = LN;
    }
    public void grantToken(int j){
        Message sendMsg = new Message(pid);
        sendMsg.setText("PRIVILEGE");
        sendMsg.setQ(REQUEST_Q);
        sendMsg.setLN(LN);
        sendTo(j, sendMsg);
    }
    public void requestCS(int pid){
        if(!hasToken()){
            requesting = true;
            RN[pid]++;
            Message sendMsg = new Message(pid);
            print("Requesting to enter CS, time="+time.toGMTString());
            t3 = System.currentTimeMillis();
            sendMsg.setText("REQUEST");
            sendMsg.setseqno(RN[pid]);
            for(Integer j: PROCESS_LIST.values()){
                if(j!=pid){
                    sendTo(j, sendMsg);
                    msgCount[pid]++;
                }
            }
        }else{
            executeCS();
            releaseCS(pid);
        }
    }
    public void receiveCSRequest(Integer j, Integer seqNo){
        
        RN[j]=Integer.max(RN[j], seqNo);
        if(hasPrivilege() && !requesting && (RN[j]==(LN[j]+1))){
            token = false;
            havePrivilege= false;
            grantToken(j);
        }
    }
    public void releaseCS(Integer i){

        LN[i] = RN[i];
        for(Integer j : PROCESS_LIST.values()){
            if(!Objects.equals(j, i)){
                if(!REQUEST_Q.contains(j) && (RN[j]==(LN[j]+1))){
                    REQUEST_Q.add(j);
                }
            }
        }
        if(!REQUEST_Q.isEmpty()){
            token = false;
            havePrivilege = false;
            grantToken(REQUEST_Q.poll());
        }
        requesting = false;
    }
    
    public void executeCS(){
        if(!finished){
            print("Executing CS for "+prop.t3+" ms."+"["+executionCount+"]");
            if(executionCount == prop.interval){
                finished = true;
            }
            executionCount++;
            t1 = System.currentTimeMillis();
            t1List.add(t1);
            try {
                Thread.sleep(prop.t3);
            } catch (InterruptedException ex) {
                Logger.getLogger(SuzukiKasami.class.getName()).log(Level.SEVERE, null, ex);
            }
            t2 = System.currentTimeMillis();
        }
        releaseCS(pid);
    }
    
}
