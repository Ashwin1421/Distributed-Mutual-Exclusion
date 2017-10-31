/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Message;

import java.io.Serializable;
import java.util.Map;
import java.util.Queue;

/**
 *
 * @author Ashwin
 */
public class Message implements Serializable{
    String text=null;
    Integer pid=null;
    Map<String, Integer> hostNames;
    Integer sendpid= null;
    Integer seqNo;
    Queue<Integer> Q;
    Integer[] LN;
    
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
    
    public Integer getPid(){
        return pid;
    }
    
    public void addPID(Integer pid){
        this.sendpid = pid;
    }
    
    public void addMap(Map<String, Integer> map){
        this.hostNames = map;
    }
    
    public Integer receivepid(){
        return sendpid;
    }
    
    public Map<String,Integer> getHostnames(){
        return hostNames;
    }
    
    public void setseqno(Integer seqNo){
        this.seqNo = seqNo;
    }
    
    public Integer getseqno(){
        return seqNo;
    }
    
    public void setQ(Queue<Integer> Q){
        this.Q = Q;
    }
    
    public Queue<Integer> getQ(){
        return Q;
    }
    
    public void setLN(Integer[] LN){
        this.LN = LN;
    }
    
    public Integer[] getLN() {
        return LN;
    }
    @Override
    public String toString(){
        return "Text="+text+",pid="+pid;
    }
}
