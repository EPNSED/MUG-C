/** An application to create input files for the MatLab program
/*       that analyze the metal-binding site.
/* Creat files with PDBID_f.dat as their names, which contains the Oxygen
/*       coordinations and the Carbon coordination that is bonded to Oxygen. 
/* For atoms of more than one possible coordinates, pick the coordinate indicated
         as "A"
/* This application uses class FileOperation in package utils
/*       and class ParsePDB in package pdbparser. 
*/

package src.parsepdb.mugcarbonlist;

import java.util.*;
import java.io.*;

import src.utils.*;

public class CombineFiles{
    public static void combineSurfix(String args, String fileType){
        String inputLine;
        String[] filePrefixes;

        inputLine = fileType;

        filePrefixes = inputLine.split(" ", -1);
        List fileList = new ArrayList();
        fileList = FileOperation.getEntriesAsList(args); // a list of PDBIDs
        String currDirectory = System.getProperty("user.dir");
        List finalResult = new ArrayList();
        

        for(int i = 0; i < fileList.size(); i++){
            String PDBID = ((String)fileList.get(i)).substring(0,4);
            String outFileName = currDirectory+"/outputdata/"+PDBID+"_out.dat";
            List inputList = new ArrayList();
            for (int j = 0; j < filePrefixes.length; j++){
	        inputList.add(currDirectory+ "/outputdata/"+PDBID +"_" + filePrefixes[j] + ".dat");
            } 
            
            FileOperation.concatenateFiles(outFileName, inputList);
        } 


        for(int i = 0; i < fileList.size(); i++){
            String PDBID = ((String)fileList.get(i)).substring(0,4);
            String outFileName =currDirectory+ "/outputdata/"+PDBID+"_out.dat";
	        List inputFile = new ArrayList();
            inputFile = FileOperation.getEntriesAsList(currDirectory+"/outputdata/"+PDBID+"_out.dat");
            
            for (int j = 0; j < inputFile.size(); j++){
                String line = (String)(inputFile.get(j));
                String sub = line.substring(14,15);
                if (sub.contentEquals(new StringBuffer("N"))|sub.contentEquals(new StringBuffer("S"))){
                    line = line + "   0.000    0.000    0.000";
                }
                String sub2 = line.substring(0,6);
                if (sub2.contentEquals(new StringBuffer("HETATM"))){
                    line = line + "   0.000    0.000    0.000";
                }
                finalResult.add(line);
            }
            FileOperation.saveResults(finalResult,currDirectory+"/outputdata/" +PDBID+"_f1.dat", "w");
            finalResult.clear();
            File file = new File(outFileName);
            file.delete();
        }        
    }
}
