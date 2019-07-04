package com.vizualize;

import com.vizualize.train.service.TrainServiceLstm;

public class MainTrain {

	public static void main(String[] args) {
		String instrument = "OMXS30_5M"; 
		int lookForwardPeriod = 50;

		try {
			TrainServiceLstm.train(instrument, lookForwardPeriod);
			//			TrainServiceRL.train(instrument, lookForwardPeriod);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
