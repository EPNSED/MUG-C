/** An application to create input files for the program and generate a pdb file in prediciton results folder
/*       that analyze the metal-binding site.
/* Creat files with PDBID_metalName.dat as their names, which contains the metal
/*       coordinations. 
/* For atoms of more than one possible coordinates, pick the coordinate indicated
         as "A".
/* This application uses class FileOperation in package utils
/*       and class ParsePDB in package pdbparser. 
*/

package mugcmaven.mugcmaven.src.parsepdb.mugcarbonlist;

import mugcmaven.mugcmaven.src.utils.*;

import java.util.*;

public class MetalBindingMetal{

    public static void getMetal(String args, String metal){
                   
        List fileList = new ArrayList();
        List resultList = new ArrayList(); 
        List caList = new ArrayList();
        List allAtom=new ArrayList();
        List allAtomNoWater=new ArrayList();
        
	    String currDirectory = System.getProperty("user.dir");        
        fileList = FileOperation.getEntriesAsList(args);  // a list of PDBIDs   
        //System.out.println(args);

        for (int i = 0; i< fileList.size(); i++){
            String PDBID = ((String)fileList.get(i)).substring(0,4);
            String fileName = currDirectory+"/inputdata/" + PDBID +".pdb";
            caList = ParsePDB.getLines(fileName, "HETATM",metal, metal);
            allAtom=ParsePDB.getLines(fileName, "ATOM", "Res", "allatom");
            allAtomNoWater=ParsePDB.getLines(fileName, "ATOM", "ResNoWater", "allatom");;
            
            for (int j = 0; j < caList.size(); j++) {
                try{
                    String getString = (String)caList.get(j);
                    if (getString.regionMatches(16,"A",0,1) | Character.isWhitespace(getString.charAt(16)) == true){
                        resultList.add(getString.substring(0,6)+" "+getString.substring(6,11)+
                                       " "+getString.substring(12,16)+" "+" "+" "+getString.substring(17,20)+
                                       " "+" "+" "+getString.substring(22,26)+" "+" "+" "+
                                       getString.substring(30,54));
                    }
                } catch(Exception e){
                    System.out.println("Caught UnsupportedOperationException"); 
                }           
            }      

            FileOperation.saveResults(allAtomNoWater, 
            		currDirectory+"/outputdata/" + PDBID + "_noWater.dat", "w");
            FileOperation.saveResults(allAtom, 
            		currDirectory+"/predictionResults/" + PDBID + "_site.pdb", "w");           
            FileOperation.saveResults(resultList, 
            		currDirectory+"/outputdata/" + PDBID + "_" + metal.toLowerCase() + ".dat", "w");
            resultList.clear(); 
            
        }
    }   
}