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
import java.util.ArrayList;
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

    Map<ObjectOutputStream, Integer> PROCESS_LIST;
    Utils prop = new Utils();
    long synchDelay=0;
    long t1=0,t2=0,t3=0;
    static int execCount = 1;
    Date time = new Date();
    //Peformance data
    Integer[] msgCount;
    Queue<Long> t1List = new LinkedList<>();
    Queue<Long> t2List = new LinkedList<>();
    Queue<Long> t3List = new LinkedList<>();
    
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
    
    
    public void sendTo(Integer i, String text){
        for(ObjectOutputStream out: PROCESS_LIST.keySet()){
            if(PROCESS_LIST.get(out).equals(i)){
                try {
                    Message msg = new Message(pid);
                    msg.setText(text);
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
        return Math.abs(t1-t2);
    }
    
    public Integer getHolder() {
        return holder;
    }
    public double getAvgExecutionTime(){
        return t1List.stream().mapToDouble(val->val).average().orElse(0)/prop.interval;
    }
    public double getAvgReleaseTime(){
        return t2List.stream().mapToDouble(val->val).average().getAsDouble()/prop.interval;
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
    
    public double getAvgWaitTime(){
        double d1 = t1List.stream().mapToDouble(val->val).average().getAsDouble();
        double d2 = t3List.stream().mapToDouble(val->val).average().getAsDouble();
        return Math.abs((d1-d2)/prop.interval);
    }
    public void addRequest(Integer pid){
        REQUEST_Q.add(pid);
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
    public void makeRequest(){
        if(!hasToken() && !REQUEST_Q.isEmpty() && !requested){
            requested = true;
            msgCount[pid]++;
            t3 = System.currentTimeMillis();
            t3List.add(t3);
            print("Requesting token from "+holder+", current time="+time.toGMTString());
            sendTo(holder, "REQUEST");
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
        assignPrivilege();
        makeRequest();
    }
    public void assignPrivilege(){
        if(holder==pid && !insideCS && !REQUEST_Q.isEmpty()){
            holder = REQUEST_Q.poll();
            requested = false;
            if(holder==pid && done){
                REQUEST_Q.remove();
            }
            if(holder==pid && !done){
                executeCS();
            }else{
                grantToken(holder);
                if(!REQUEST_Q.isEmpty()){
                    makeRequest();
                }
            }
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
        print("Granting token to="+i);
        sendTo(i,"PRIVILEGE");
    }
    
    public void executeCS(){
        insideCS = true;
        print("Entering CS for "+prop.t3+" ms."+"[#"+execCount+"]");
        if(execCount==prop.interval){
            done = true;
            while(REQUEST_Q.remove(pid));
        }
        execCount++;
        t1 = System.currentTimeMillis();
        t1List.add(t1);
        try {
            Thread.sleep(prop.t3);
        } catch (InterruptedException ex) {
            Logger.getLogger(Raymond.class.getName()).log(Level.SEVERE, null, ex);
        }
        t2 = System.currentTimeMillis();
        t2List.add(t2);
        exitCS();
    }
    
    public void exitCS(){
        insideCS = false;
        assignPrivilege();
        makeRequest();
    }
    
}
