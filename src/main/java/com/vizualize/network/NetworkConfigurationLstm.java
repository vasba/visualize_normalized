package com.vizualize.network;

import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

public class NetworkConfigurationLstm {
	
	static int tbpttLength = 5;
	public static double learningRate = 0.0001;

	public static MultiLayerConfiguration getLstmConfiguration(int nIn, int nOut, int lstmLayerSize) {
		return new NeuralNetConfiguration.Builder()
				.seed(12345)
				.l2(0.0001)
				.learningRate(learningRate)
				.weightInit(WeightInit.XAVIER)
				.updater(new Adam())
				.list()
				.layer(0, new LSTM.Builder().nIn(nIn).nOut(lstmLayerSize)
						.activation(Activation.TANH).build())
//				.layer(1, new LSTM.Builder().nIn(lstmLayerSize).nOut(lstmLayerSize)
//						.activation(Activation.TANH).build())
				.layer(1, new RnnOutputLayer.Builder(LossFunction.MCXENT).activation(Activation.SOFTMAX)        //MCXENT + softmax for classification
						.nIn(lstmLayerSize).nOut(nOut).build())
//				.backpropType(BackpropType.TruncatedBPTT).tBPTTForwardLength(tbpttLength).tBPTTBackwardLength(tbpttLength)
				.build();
	}
}
