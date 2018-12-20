package mugcmaven.mugcmaven;


import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

import jdk.nashorn.internal.parser.JSONParser;

import com.amazonaws.services.lambda.runtime.Context;

import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;


public class Handler{
    public void handleRequest(JSONObject json, Context context)
		throws IOException {
		try
		{
            //TODO implement handler
            System.out.println("MugcJava Handler has started");
			context.getLogger().log("Input: " + json);
			String pdbinfo = (String) json.get("pdbinfo"); // Type casting :)
			String[] data = pdbinfo.split(" ");
			String pdbURL = data[0];
			String pdbID = data[1];
			System.out.println(pdbURL);
            String currDirectory = "/tmp/";
            System.out.println(currDirectory);
            new File("c:\\tmp\\inputdata").mkdirs();
            System.out.println(new File("c:\\tmp\\outputdata").mkdirs());
			String targetDirectory = currDirectory+"inputdata/"+ pdbID+".pdb";
			try{
				download(pdbURL, targetDirectory);
			}catch(Exception ex){
				System.out.println(ex.getMessage());
			}
			writeList(pdbID, currDirectory);
			Process pro = Runtime.getRuntime().exec("java -cp src/MUGC -i 4 -l 1.74 -u 4.9");
			try{
			pro.waitFor();
			}catch(InterruptedException ex){
				System.out.println(ex.getMessage());
			}
		}catch(IOException ex)
		{
			System.out.println(ex.getMessage());
		}
	}
	/**
    * This method downloads the PDB File to the input directory 
    *
    * @param  url  an absolute URL to the PDB Rest api to download the PDB file
    * @param  targetDir the target location; input folder
    *
    */
    public static void download(String url, String targetDir) throws Exception {
        try (InputStream in = URI.create(url).toURL().openStream()) {
            Files.copy(in, Paths.get(targetDir));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    /**
    * This method writes the PDB ID to the list.text file in the input directory 
    *
    * @param  pdbID_arg  an absolute URL to the PDB Rest api to download the PDB file
    * @param  currDir the current location
    *
    */
    public static void writeList(String pdbID_arg, String currDir){
        String pdbID = pdbID_arg;
        String targetDir = currDir +"/inputdata/"+ "list.txt";
        try {
            FileWriter writer = new FileWriter(targetDir, true);
            writer.write(pdbID);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
