package com.vizualize;

import static spark.Spark.get;
import static spark.Spark.port;

import org.eclipse.jetty.util.log.Log;

import com.vizualize.controllers.BuySellController;
import com.vizualize.train.service.PeriodicTrainer;


public class JavaSparkApp {
    
    public static void main(String[] args) {
    	
//    	PropertyConfigurator.configure("conf/log4j.properties")
//    	Logger.getRootLogger().setLevel(Level.OFF);
//    	Logger.getLogger("org").setLevel(Level.OFF);
//    	Log.getLogger(HashedSession.class).setDebugEnabled(false);
    	Log.getLogger("org.eclipse.jetty.server.session").setDebugEnabled(false);
//    	Logger.getLogger("akka").setLevel(Level.OFF)
        port(4569);
        
        get("/lastOrder", BuySellController.getLastOrder);
        
        Thread t = new PeriodicTrainer();
        t.start();
    }

}
