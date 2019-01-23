package mugcmaven.mugcmaven.src.fileutil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Copy {
	
	private static String inputFilename, outputFilename;
	
	public Copy(String inputFilename, String outputFilename){
		this.inputFilename=inputFilename;
		this.outputFilename=outputFilename;
	}
	
	public static void CopyFile() throws IOException {
		File inputFile = new File(inputFilename);
		File outputFile = new File(outputFilename);
		
		FileReader in = new FileReader(inputFile);
		FileWriter out = new FileWriter(outputFile);
		int c;
		
		while ((c = in.read()) != -1)
			out.write(c);
		
			in.close();
			out.close();
		}
}