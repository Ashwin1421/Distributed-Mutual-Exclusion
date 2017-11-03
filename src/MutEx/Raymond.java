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
import java.util.HashMap;
import java.util.LinkedList;
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
public class Raymond implements Serializable {
    Integer holder;
    Integer pid;
    boolean insideCS = false;
    boolean token = false;
    boolean requested = false;
    boolean forwardrequest = false;
    boolean done = false;
    
    static Queue<Integer> REQUEST_Q = new ConcurrentLinkedQueue<Integer>();
    Integer[] msgCount;
    Map<ObjectOutputStream, Integer> PROCESS_LIST;
    Utils prop = new Utils();
    long synchDelay=0;
    long t1=0,t2=0,t3=0;
    static int execCount = 1;
    Date time = new Date();
    
    public Raymond(Integer pid, Map<ObjectOutputStream, Integer> PROCESS_LIST){
        this.pid = pid;
        this.PROCESS_LIST = PROCESS_LIST;
        if(prop.getParent(pid)!=null){
            this.holder = prop.getParent(pid);
        }else{
            this.holder = pid;
            this.token = true;
        }
        this.msgCount = new Integer[prop.N+1];
        Arrays.fill(msgCount, 0);
    }
    
    public void setToken(){
        this.token = true;
        this.holder = pid;
    }
    
    
    public void sendTo(Integer pid, Message msg){
        for(ObjectOutputStream out: PROCESS_LIST.keySet()){
            if(PROCESS_LIST.get(out).equals(pid)){
                try {
                    out.writeObject(msg);
                    out.flush();
                    print("SENT="+msg.toString());
                } catch (IOException ex) {
                    Logger.getLogger(Raymond.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    public long getSynchDelay(long t1, long t2){
        return t1 > t2 ? t1-t2 : t2-t1;
    }
    
    public Integer getHolder() {
        return holder;
    }
    public long getExecutionTime(){
        return t1;
    }
    public long getReleaseTime(){
        return t2;
    }
    public Integer nextRequest() {
        return REQUEST_Q.peek();
    }
    
    public boolean hasToken(){
        return token;
    }
    public int getCurrentExecCount(){
        return execCount;
    }
    
    public long getWaitTime(){
        return t3 < t1 ? t1-t3 : t3-t1;
    }
    public void removeRequest(){
        REQUEST_Q.poll();
    }
    
    public Integer getTotalMsgCount(Integer i){
        return msgCount[i];
    }
    public void print(String s){
        System.out.println("[MutEx]$:"+s);
    }
    
    /**
     * Making a request to enter CS.
     */
    public void requestCS(int pid){
        if(!hasToken() && !REQUEST_Q.isEmpty()){
            REQUEST_Q.add(pid);
        }else if(!hasToken() && REQUEST_Q.isEmpty() && !requested && !done){
            Message sendMsg = new Message(pid);
            sendMsg.setText("REQUEST");
            print("Requesting token from="+holder+", time="+time.toGMTString());
            requested = true;
            msgCount[pid]++;
            t3 = System.currentTimeMillis();
            REQUEST_Q.add(pid);
            sendTo(holder, sendMsg);
        }    
    }
    /**
     * Accepting a request.
     * If this process doesn't have the token
     * it forwards a request to its holder.
     * If it has the token, then it grants it.
     */
    public void receiveCSRequest(int j){
        REQUEST_Q.add(j);
        if(!hasToken() && holder!=pid && !forwardrequest && !requested){
            print("Forwarding request to="+holder);
            msgCount[j]++;
            forwardrequest = true;
            Message sendMsg = new Message(pid);
            sendMsg.setText("REQUEST");
            sendTo(holder, sendMsg);
        }
        if(holder==pid && !insideCS && !REQUEST_Q.isEmpty() && nextRequest()==j){
            int grant = REQUEST_Q.poll();
            grantToken(grant);
            if(!REQUEST_Q.isEmpty()){
                sendRequest(grant);
            }
        }
    }
    public void assignPrivilege(){
        if(holder==pid && !insideCS && !REQUEST_Q.isEmpty()){
            holder = REQUEST_Q.poll();
            requested = false;
            if(holder==pid){
                executeCS();
            }else{
                grantToken(holder);
                if(!REQUEST_Q.isEmpty()){
                    sendRequest(holder);
                }
            }
        }
    }
    /**
     * Sending a request.
     * Different than making a request for your own.
     * This is making a request for the 
     * remaining request in its request queue.
     */
    public void sendRequest(Integer i){
        if(!requested){
            REQUEST_Q.add(pid);
            Message sendMsg = new Message(pid);
            sendMsg.setText("REQUEST");
            sendTo(i, sendMsg);
        }
    }
    /**
     * Granting the token to 
     * the process at the head of 
     * request queue.
     */
    public void grantToken(Integer i){
        token = false;
        holder = i;
        Message sendMsg = new Message(pid);
        sendMsg.setText("PRIVILEGE");
        print("Granting token to="+i);
        sendTo(i,sendMsg);
    }
    
    public void executeCS(){

        insideCS = true;
        requested = false;
        forwardrequest = false;
        print("Entering CS for "+prop.t3+" ms."+"[#"+execCount+"]");
        if(execCount==prop.interval){
            done = true;
        }
        execCount++;
        t1 = System.currentTimeMillis();
        try {
            Thread.sleep(prop.t3);
        } catch (InterruptedException ex) {
            Logger.getLogger(Raymond.class.getName()).log(Level.SEVERE, null, ex);
        }
        t2 = System.currentTimeMillis();
    }
    
    
}
