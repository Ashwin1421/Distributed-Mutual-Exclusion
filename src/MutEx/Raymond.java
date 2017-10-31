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
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ashwin
 */
public class Raymond {
    public Integer HOLDER;
    Integer pid;
    boolean USING = false;
    boolean ASKED = false;
    static Queue<Integer> REQUEST_Q = new LinkedList<Integer>();
    Map<ObjectOutputStream, Integer> PROCESS_LIST;
    Utils prop = new Utils();
    long synchDelay=0;
    long t1=0,t2=0;
    static int execCount = 1;
    Date time = new Date();
    
    public Raymond(Integer pid, Map<ObjectOutputStream, Integer> PROCESS_LIST){
        this.pid = pid;
        this.PROCESS_LIST = PROCESS_LIST;
        if(prop.getParent(pid)!=null){
            this.HOLDER = prop.getParent(pid);
        }else{
            this.HOLDER = pid;
            this.USING = true;
        }
    }
    
    public void setToken(){
        this.USING = true;
        this.HOLDER = pid;
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
        return HOLDER;
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
    
    public boolean isUsing(){
        return USING;
    }
    
    public int getCurrentExecCount(){
        return execCount;
    }
    
    
    public void removeRequest(int i){
        REQUEST_Q.remove(i);
    }
    
    public void print(String s){
        System.out.println("[Raymond-MutEx]$:"+s);
    }
    
    public void requestCS(int pid){
        if(!USING && REQUEST_Q.isEmpty()){
            Message sendMsg = new Message(pid);
            sendMsg.setText("REQUEST");
            print("Requesting token from="+HOLDER+", time="+time.toGMTString());
            REQUEST_Q.add(pid);
            sendTo(HOLDER, sendMsg);
        }
    }
    public void receiveCSRequest(int j){
        REQUEST_Q.add(j);
        if(!USING && HOLDER!=pid && REQUEST_Q.size()==1){
            print("Forwarding request to="+HOLDER);
            Message sendMsg = new Message(pid);
            sendMsg.setText("REQUEST");
            sendTo(HOLDER, sendMsg);
        }
        if(USING && HOLDER==pid){
            grantToken(REQUEST_Q.peek());
        }
       
    }

    public void sendRequest(Integer i){
        Message sendMsg = new Message(pid);
        sendMsg.setText("REQUEST");
        sendTo(i, sendMsg);
    }
    
    public void grantToken(Integer i){
        REQUEST_Q.remove();
        USING=false;
        HOLDER=i;
        Message sendMsg = new Message(pid);
        sendMsg.setText("PRIVILEGE");
        print("Granting token to="+i);
        sendTo(i,sendMsg);
    }
    
    public void executeCS(){
        USING = true;
        HOLDER = pid;
        print("Entering CS for "+prop.t3+" ms."+"["+execCount+"]");
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
