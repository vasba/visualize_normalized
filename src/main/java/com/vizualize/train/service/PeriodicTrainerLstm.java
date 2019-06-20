package com.vizualize.train.service;

import com.vizualize.rl.TrainServiceRL;

public class PeriodicTrainerLstm extends Thread {
	
	@Override
    public void run()
    {
        while(true) {
        	String instrument = "OMXS30_5M"; 
        	int lookForwardPeriod = 8;
        	
        	try {
				TrainServiceLstm.train(instrument, lookForwardPeriod);
//				TrainServiceRL.train(instrument, lookForwardPeriod);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

}
