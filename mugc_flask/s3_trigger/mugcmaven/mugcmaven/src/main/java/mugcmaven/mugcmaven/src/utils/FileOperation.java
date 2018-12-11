package src.utils;

/* From http://java.sun.com/docs/books/tutorial/index.html */
/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 */
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.*;
import java.util.*;

/**
 * Delete a file from within Java
 * 
 * @author Ian F. Darwin, http://www.darwinsys.com/
 * @version $Id: Delete.java,v 1.5 2004/02/09 03:33:47 ian Exp $
 */

public class FileOperation {
	

	
  public static void copy(String fileDirName, String fileDir, String fileName) throws IOException {
    File inputFile = new File(fileDirName);
    File outputFile = new File(fileDir+fileName);

    FileReader in = new FileReader(inputFile);
    FileWriter out = new FileWriter(outputFile);
    int c;

    while ((c = in.read()) != -1)
      out.write(c);
      in.close();
      out.close();
  }
  
  public static void changeName(String fileDirName, String newFileDirName) throws IOException {
	    File inputFile = new File(fileDirName);
	    File outputFile = new File(newFileDirName);

	    FileReader in = new FileReader(inputFile);
	    FileWriter out = new FileWriter(outputFile);
	    int c;
	    
	    while ((c = in.read()) != -1)
	      out.write(c);
	      in.close();
	      out.close();
	  }
  
  
  public static void delete (String folder){
	    // Construct a File object for the backup created by editing
	    // this source file. The file probably already exists.
	    // My editor creates backups by putting ~ at the end of the name.
	    File inputFolder = new File(folder);
        String[] fileNameArray;
	    // Quick, now, delete it immediately:
	    fileNameArray = inputFolder.list();
	    if (fileNameArray != null) {
	    	for(int i = 0; i < fileNameArray.length; i++){
	    		File fileName = new File(folder + File.separator + fileNameArray[i]);
	    		fileName.delete();
	    	}
	    }
	}
  
	public static InputStream getInputStream(String fileName) {
		try {
			File file = new File(fileName);
			return new FileInputStream(file);
		} catch (Exception e){
			return null;
		}
	}
	
	public static List getEntriesAsList(String fileName){

        // Return list entries. 
        // An element of that list is one line of the file fileName.
 
		List entries = new ArrayList();
		try {
			String line;
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			while ((line = br.readLine()) != null){
				entries.add(line);
			}
                        br.close();
		} catch(Exception e){}
		return entries;
	}
        
	public static void saveResults(List entryNames, String fileName, String option){

        // Write list entryNames in file fileName.
        // An element in entryNames is one line in fileName. 
        if (option.equals("w")){
		    try {
		    	BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
			    for(int i = 0; i < entryNames.size(); i++){
                            bw.write((String)entryNames.get(i) + "\n");                            
			    }
			    bw.close();
		    } catch(Exception e){}
        }
        if (option.equals("a")){
        	try {
		    	BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true));
			    for(int i = 0; i < entryNames.size(); i++){
                            bw.write((String)entryNames.get(i) + "\n");                            
			    }
			    bw.close();
		    } catch(Exception e){}
        }
	}

	public static void saveResults(String entryNames, String fileName, String option){

        // Write list entryNames in file fileName.
        // An element in entryNames is one line in fileName. 
        if (option.equals("w")){
		    try {
		    	BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
                bw.write(entryNames + "\n");                            
			    bw.close();
		    } catch(Exception e){}
        }
        if (option.equals("a")){
        	try {
		    	BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true));
                bw.write(entryNames + "\n");                            
			    bw.close();
		    } catch(Exception e){}
        }
	}
		
    public static Process windowsCommand(String command){

        try {
            return Runtime.getRuntime().exec(command);                         
           } catch(Exception e){e.printStackTrace();
        }
           return null;
      }

       public static void concatenateFiles(String outFileName, List fileList){
           List outFileContent = new ArrayList();
           List file = new ArrayList();
           for(int i = 0; i < fileList.size(); i++){
               String fileName = (String)(fileList.get(i));
                   
               file = getEntriesAsList(fileName);
               System.out.println(fileName);
               outFileContent.addAll(file);   
           }
           saveResults(outFileContent, outFileName, "w");              
       }	
}