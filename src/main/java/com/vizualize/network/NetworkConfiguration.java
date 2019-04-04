package com.vizualize.network;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class NetworkConfiguration {
    
    //Random number generator seed, for reproducability
    public static final int seed = 12345;
  //Number of iterations per minibatch
    public static final int iterations = 1;
    //Network learning rate
    public static double learningRate = 0.0001;
    
    /** Returns the network configuration
     */
    public static MultiLayerConfiguration getDeepDenseLayerNetworkConfiguration(int numHiddenNodes, int numInputs, int numOutputs) {
        return new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations)
//                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(learningRate)                
                .weightInit(WeightInit.XAVIER)
                .updater(new Nesterovs(0.9))
//                .l2(0.001)            
//                .regularization(true)
//                .dropOut(0.2)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes)
                        .activation(Activation.IDENTITY).build())
                .layer(1, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
                        .activation(Activation.IDENTITY).build())    
//                .layer(2, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
//                        .activation(Activation.LEAKYRELU).build()) 
//                .layer(3, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
//                        .activation(Activation.LEAKYRELU).build()) 
//                .layer(4, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
//                        .activation(Activation.LEAKYRELU).build()) 
//                .layer(5, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
//                        .activation(Activation.LEAKYRELU).build()) 
//                .layer(6, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
//                        .activation(Activation.LEAKYRELU).build())
                .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MEAN_ABSOLUTE_ERROR)
                        .activation(Activation.IDENTITY)
                        .nIn(numHiddenNodes).nOut(numOutputs).build())
                
//                .layer(0, new OutputLayer.Builder(LossFunctions.LossFunction.MEAN_ABSOLUTE_ERROR)
//    					.activation(Activation.IDENTITY)
//    					.nIn(numInputs).nOut(numOutputs).build())
                .pretrain(false).backprop(true).build();
    }

    /** Returns the network configuration
     */
    public static MultiLayerConfiguration getDeepDenseLayerNetworkConfigurationClassification(int numHiddenNodes, int numInputs, int numOutputs) {
        return new NeuralNetConfiguration.Builder()
                .seed(seed)
                .learningRate(learningRate)
                .weightInit(WeightInit.XAVIER)
                .updater(new Nesterovs(0.9))
                .list()
                .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes)
                        .activation(Activation.IDENTITY).build())
                .layer(1, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
                        .activation(Activation.IDENTITY).build())
//                .layer(2, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
//                        .activation(Activation.RELU).build())
//                .layer(3, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
//                        .activation(Activation.RELU).build())
                .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .activation(Activation.SOFTMAX)
                        .nIn(numHiddenNodes).nOut(numOutputs).build())
                .pretrain(false).backprop(true).build();
    }
    
}
