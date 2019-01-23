package com.vizualize.serialize;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

public class SerializableUtils {
	
	public static void saveSerializable(Serializable object, String filePath) throws Exception {
		try (
				OutputStream file = new FileOutputStream(filePath);
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutput output = new ObjectOutputStream(buffer);
				){
			output.writeObject(object);
		}  
	}
	
	public static Serializable loadSerializable(String filePath) throws Exception {
		try(
			      InputStream file = new FileInputStream(filePath);
			      InputStream buffer = new BufferedInputStream(file);
			      ObjectInputStream input = new ObjectInputStream(buffer);
			    ){
			      //deserialize
			      return (Serializable) input.readObject();
			    }
	}

}
