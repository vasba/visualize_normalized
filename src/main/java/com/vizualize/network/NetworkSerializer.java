package com.vizualize.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Serializable;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import com.vizualize.serialize.SerializableUtils;

public class NetworkSerializer {
    
    public static final String modelsLocation = "~/hackathon_workspace/modelsTA";
    
    public static void saveModel(MultiLayerNetwork network, String modelName,
            String dateStr) throws Exception {
                
        File directory = new File(modelsLocation + "/" + modelName);
        if (! directory.exists()){
            directory.mkdirs();
        }
        File modelFile = new File(directory, modelName + ".zip");
        ModelSerializer.writeModel(network, modelFile, true);
        File metaFile = new File(directory, modelName + "Meta.txt");
        BufferedWriter writer = new BufferedWriter(new FileWriter(metaFile));
        writer.write(dateStr);
         
        writer.close();
    }
    
    public static MultiLayerNetwork loadNetwork(String modelName ) throws Exception {
        File directory = new File(modelsLocation + "/" + modelName);
        File modelFile = new File(directory, modelName + ".zip");
        if (modelFile.exists()) {
            return ModelSerializer.restoreMultiLayerNetwork(modelFile);
        } else {
            return null;
        }
    }
    
    public static String lastTrainedDate(String modelName ) throws Exception {
        File directory = new File(modelsLocation + "/" + modelName);
        File metaFile = new File(directory, modelName + "Meta.txt");
        
        if (metaFile.exists()) {
        BufferedReader br = new BufferedReader(new FileReader(metaFile));
        String res = br.readLine();
        br.close();
        return res;
        } else {
            return null;
        }
    }
    
    public static void saveTransform(String modelName, Serializable serializable) throws Exception {
    	File directory = new File(modelsLocation + "/" + modelName);
    	File transformFile = new File(directory, modelName + "transform.txt");
    	if (! directory.exists()){
            directory.mkdirs();
        }
    	
    	SerializableUtils.saveSerializable(serializable, transformFile.getAbsolutePath());
    }
    
    public static Serializable loadtransform(String modelName) throws Exception {
    	File directory = new File(modelsLocation + "/" + modelName);
    	File transformFile = new File(directory, modelName + "transform.txt");
    	return SerializableUtils.loadSerializable(transformFile.getAbsolutePath());
    }
}
