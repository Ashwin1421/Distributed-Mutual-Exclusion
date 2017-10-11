package Utilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Ashwin
 */
public class Utils {
    Properties config = new Properties();
    InputStream configInput = null;
    public String Coordinator;
    public int N;
    public int t1;
    public int t2;
    public int t3;
    
    public Utils(){
        configInput = getClass().getClassLoader().getResourceAsStream("config.properties");
        if(configInput!=null){
            try {
                config.load(configInput);
            } catch (IOException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            this.Coordinator = config.getProperty("coordinator");
            this.N = Integer.parseInt(config.getProperty("n"));
            this.t1 = Integer.parseInt(config.getProperty("t1"));
            this.t2 = Integer.parseInt(config.getProperty("t2"));
            this.t3 = Integer.parseInt(config.getProperty("t3"));
            
            
        }else{
            System.out.println("Config File doesn't exist!");
        }
    }
    
    public Integer[] getNBList(Integer pid){
        String[] nblist = config.getProperty(pid.toString()).split(",");
        Integer[] NB_LIST = new Integer[nblist.length];
        int i=0;
        for(String nb: nblist){
            NB_LIST[i++] = Integer.parseInt(nb);
        }
        return NB_LIST;
    }
    
}
