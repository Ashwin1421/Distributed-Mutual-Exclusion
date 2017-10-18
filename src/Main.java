/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import Utilities.Utils;
import Coordinator.Coordinator;
import Process.Process;
import java.util.Arrays;

/**
 *
 * @author Ashwin
 */
public class Main {
    
    public static void main(String[] args){
            
        Utils ut = new Utils();
        String option="";
        if(args.length == 1){
            option = args[0];
            if(option.equalsIgnoreCase("-c")){
                new Coordinator(1205,1).start();
            }
        }else{
            new Process(ut.Coordinator,1205).start();
        }    
    }
    
}
