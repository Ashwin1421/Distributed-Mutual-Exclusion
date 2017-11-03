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
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
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
    Integer executionCount = 1;
    long t1 = 0;
    long t2 = 0;
    Map<ObjectOutputStream, Integer> PROCESS_LIST;
    Queue<Integer> REQUEST_Q = new ConcurrentLinkedQueue<>();
    Utils prop = new Utils();
    
    public SuzukiKasami(int pid, Map<ObjectOutputStream, Integer> PROCESS_LIST){
        this.PROCESS_LIST = PROCESS_LIST;
        this.pid = pid;
        this.RN = new Integer[prop.N+1];
        this.LN = new Integer[prop.N+1];
        Arrays.fill(RN, -1);
        Arrays.fill(LN, -1);
        if(pid == 1){
            token = true;
        }
    }
    
    public void print(String s){
        System.out.println("[MutEx]$:"+s);
    }
    public boolean hasToken(){
        return token;
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
    
    public long getExecutionTime(){
        return t1;
    }
    
    public long getReleaseTime(){
        return t2;
    }
    
    public long getSynchDelay(long t1, long t2){
        return t1 > t2 ? t1-t2 : t2-t1;
    }
    
    public void addRequest(int pid){
        REQUEST_Q.add(pid);
    }
    
    public void delRequest(int pid){
        REQUEST_Q.remove(pid);
    }
    
    public Integer nextRequest(){
        return REQUEST_Q.peek();
    }
    
    public Integer getCurrentExecCount(){
        return executionCount;
    }
    
    public void setToken(boolean token){
        this.token = token;
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
            sendMsg.setText("REQUEST");
            sendMsg.setseqno(RN[pid]);
            for(Integer j: PROCESS_LIST.values()){
                if(j!=pid){
                    sendTo(j, sendMsg);
                }
            }
        }
    }
    public void receiveCSRequest(Integer j, Integer seqNo){
        
        RN[j]=Integer.max(RN[j], seqNo);
        if(hasToken() && !requesting && (RN[j]==(LN[j]+1))){
            setToken(false);
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
            grantToken(REQUEST_Q.poll());
        }
        requesting = false;
    }
    
    public void executeCS(){
        
        print("Executing CS for "+prop.t3+" ms."+"["+executionCount+"]");
        executionCount++;
        t1 = System.currentTimeMillis();
        try {
            Thread.sleep(prop.t3);
        } catch (InterruptedException ex) {
            Logger.getLogger(SuzukiKasami.class.getName()).log(Level.SEVERE, null, ex);
        }
        t2 = System.currentTimeMillis();
    }
    
}
