package Utilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
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
    public String Algorithm;
    public int N;
    public int t1;
    public int t2;
    public int t3;
    public int interval;
    
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
            this.Algorithm = config.getProperty("algorithm");
            this.interval = Integer.parseInt(config.getProperty("interval"));
        }else{
            System.out.println("Config File doesn't exist!");
        }
    }
    
    public Integer getParent(Integer pid){
        if(config.getProperty(pid.toString()).equalsIgnoreCase("null")){
            return null;
        }
        Integer parentNode = Integer.parseInt(config.getProperty(pid.toString()));
        return parentNode;
    }
    
    public Queue<Integer> getChildren(Integer pid){
        Queue<Integer> children = new LinkedList<>();
        for(int id=2;id<=N;id++){
            if(getParent(id).equals(pid) && getParent(id)!=null){
                children.add(id);
            }
        }
        return children;
    }
}
