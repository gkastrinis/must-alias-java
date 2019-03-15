package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class InputReader {

	public static Set<String[]> readFile(String filename) throws IOException {
		Set<String[]> result = new HashSet<>();
		BufferedReader br = new BufferedReader(new FileReader(filename));
		String line = br.readLine();
		while (line != null) {
			result.add(line.split("\t"));
			line = br.readLine();
		}
		br.close();
		return result;
	}
}
