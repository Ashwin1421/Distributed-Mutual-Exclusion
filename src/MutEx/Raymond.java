/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MutEx;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 *
 * @author Ashwin
 */
public class Raymond {
    public int HOLDER;
    public boolean USING = false;
    public Queue<Integer> REQUEST_Q = new LinkedList<Integer>();
    public Map<PrintWriter, Integer> PROCESS_LIST;
    public boolean ASKED = false;
    
    public Raymond(int pid, Map<PrintWriter, Integer> PROCESS_LIST){
        this.HOLDER = pid;
        this.PROCESS_LIST = PROCESS_LIST;
    }
    
    public void setToken(boolean USING){
        this.USING = USING;
    }
    
    public boolean isUsing(){
        return this.USING;
    }
    
    public void requestCS(int pid){
        if(HOLDER!=pid && !REQUEST_Q.isEmpty() && !ASKED){
            
        }
    }
    public void receiveCSRequest(int pid, int seqNo){
        
    }
}
