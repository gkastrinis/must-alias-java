package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class OutputWriter {

	public static void writeFile(String filename, String data) throws IOException {
		File file = new File(filename);
		file.getParentFile().mkdirs();
		BufferedWriter br = new BufferedWriter(new FileWriter(file));
		br.write(data);
		br.close();
	}
	
	public static BufferedWriter openOutputFile(String filename) throws IOException {
		File file = new File(filename);
		file.getParentFile().mkdirs();
		BufferedWriter br = new BufferedWriter(new FileWriter(file));
		return br;
	}
	
	public static void closeOutputFile(BufferedWriter br) throws IOException {
		br.close();
	}
}
