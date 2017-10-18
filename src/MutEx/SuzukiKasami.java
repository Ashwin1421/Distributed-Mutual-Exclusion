/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MutEx;

import Utilities.Utils;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ashwin
 */
public class SuzukiKasami {
    Integer[] RN;
    Integer[] LN;
    int N;
    int pid;
    boolean token = false;
    boolean insideCS = false;
    Map<PrintWriter, Integer> PROCESS_LIST;
    Utils prop = new Utils();
    
    public SuzukiKasami(int pid, Map<PrintWriter, Integer> PROCESS_LIST){
        this.PROCESS_LIST = PROCESS_LIST;
        this.N = PROCESS_LIST.size();
        this.pid = pid;
        this.RN = new Integer[N+1];
        this.LN = new Integer[N+1];
        Arrays.fill(RN, 0);
        Arrays.fill(LN, 0);
    }
    
    public boolean hasToken(){
        return token;
    }
    
    public void setToken(boolean token){
        this.token = token;
    }
    public void grantToken(int j){
        String msg = "GRANT_CS";
        for(PrintWriter out: PROCESS_LIST.keySet()){
            if(PROCESS_LIST.get(out).equals(j)){
                out.println(msg);
                System.out.println("SENT="+msg);
            }
        }
    }
    public void requestCS(int i){
        /**
         * Requesting to enter CS.
         * Increment RN for process i,
         * and then send request messages
         * to all other processes.
         **/
        if(!token){
            RN[i]++;
            /**
             * Sequence number.
             **/
        }
        String msg = "REQUEST_CS="+i+";"+RN[i];
        for(PrintWriter output: PROCESS_LIST.keySet()){
            output.println(msg);
            System.out.println("SENT="+msg);
        }
    }
    public void receiveCSRequest(int j, int seqNo){
        
        RN[j]=Integer.max(RN[j], seqNo);
        if(hasToken() && !insideCS && (RN[j]==(LN[j]+1))){
            grantToken(j);
            setToken(false);
        }
    }
    public void releaseCS(int index){
        /**
         * Sending a release message to other processes.
         **/
        LN[index] = RN[index];
        for(PrintWriter out: PROCESS_LIST.keySet()){
            out.println("RELEASE_CS="+index);
        }
        
    }
    
    public void executeCS(){
        
        long t = prop.t3;
        System.out.println("Executing CS for "+t+" millis.");
        try {
            for(int i=1;i<=t;i++){
                Thread.sleep(i);
                System.out.print("...");
            }
            System.out.println();
        } catch (InterruptedException ex) {
            Logger.getLogger(SuzukiKasami.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
