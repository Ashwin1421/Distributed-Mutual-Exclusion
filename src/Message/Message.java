/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Message;

import java.io.Serializable;
import java.util.Map;

/**
 *
 * @author Ashwin
 */
public class Message implements Serializable{
    String text=null;
    Integer pid=null;
    Integer seqno=null;
    Map<String, Integer> hostNames;
    Integer sendpid= null;
    
    public Message(){}
    
    public Message(Integer pid){
        this.pid = pid;
    }
    
    public void setText(String text){
        this.text =text;
    }
    
    public String getText(){
        return text;
    }
    
    public void setownpid(Integer pid){
        this.pid = pid;
    }
    
    public Integer getownpid(){
        return pid;
    }
    
    public void setseqno(Integer seqno){
        this.seqno = seqno;
    }
    
    public Integer getseqno() {
        return seqno;
    }
    
    public void addPID(Integer pid){
        this.sendpid = pid;
    }
    
    public void addMap(Map map){
        this.hostNames = map;
    }
    
    public Integer receivepid(){
        return sendpid;
    }
    
    public Map<String,Integer> getHostnames(){
        return hostNames;
    }
    
    @Override
    public String toString(){
        return "TEXT="+text+",PID="+pid+",SEQ="+seqno;
    }
}
