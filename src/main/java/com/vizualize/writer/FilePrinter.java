package com.vizualize.writer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;

public class FilePrinter {

	public static void write(String filePath, String content, boolean append) {
		File file = new File(filePath);
		
		try {
			FileWriter outfile = new FileWriter(file, append);
			outfile.write(content);
			outfile.flush();
			outfile.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
