/** An application to create input CC files.
/*       that analyze the metal-binding site.
/* Create files with PDBID_cc.dat as their names, which contains the first shell carbon
/*       coordinations and the second shell Carbon coordination that is bonded to the first shell carbon.
/* For atoms of more than one possible coordinates, pick the coordinate indicated
         as "A"
/* This application uses class FileOperation in package utils
/*       and class ParsePDB in package pdbparser.
*/

package src.parsepdb.mugcarbonlist;

import src.utils.*;
import java.util.*;

public class MetalBindingCC {
	
    public static void getCC(String args){


        List fileList = new ArrayList();
        List OAndCList = new ArrayList();
        List resultList = new ArrayList();
        String atom = "O";
        String bondedAtom = "C";  // Bonded by "O";
        String firstShellCarbonLine = "";
        String secondShellCarbonLine = "";
        String oxygenLine = "";
	    String currDirectory = System.getProperty("user.dir");
        
        fileList = FileOperation.getEntriesAsList(args); // a list of PDBIDs


        for (int i = 0; i< fileList.size(); i++){   // iteratively process PDB files

            String PDBID = ((String)fileList.get(i)).substring(0,4);
            String fileName = currDirectory+"/inputdata/"+ PDBID +".pdb";

            OAndCList = ParsePDB.getBondedLines(fileName, atom, bondedAtom);

            //initialize secondShellCarbonLine
            firstShellCarbonLine= (((String)OAndCList.get(0)).substring(30,38)+ " " + 
            ((String)OAndCList.get(0)).substring(38,46)+ " " + ((String)OAndCList.get(0)).substring(46,54));   
            

            for (int j = 1; j < OAndCList.size(); j++){    // process lines in one PDB file

                String getString = (String)OAndCList.get(j);

                if (getString.regionMatches(0,"ATOM",0,4) && getString.regionMatches(13,atom,0,1) &&
                   (getString.regionMatches(16,"A",0,1) || Character.isWhitespace(getString.charAt(16)))
                   && getString.regionMatches(15,"2",0,1)!=true ) {

                    oxygenLine = getString.substring(0,6)+" "+getString.substring(6,11)+
                                   " "+getString.substring(12,16)+" "+" "+" "+getString.substring(17,20)+
                                   " "+" "+" "+getString.substring(22,26)+" "+" "+" ";
                    resultList.add(oxygenLine + firstShellCarbonLine+ " "+ secondShellCarbonLine + "   "+getString.substring(21,22));
                }
                
                if (getString.regionMatches(0,"ATOM",0,4) && getString.substring(13,15).equals("CB") &&
                (getString.regionMatches(16,"A",0,1) || Character.isWhitespace(getString.charAt(16)))){
                	secondShellCarbonLine=(((String)OAndCList.get(j-3)).substring(30,38)+ " " + 
                            ((String)OAndCList.get(j-3)).substring(38,46)+ " " + ((String)OAndCList.get(j-3)).substring(46,54));   
                }
                else{
                	secondShellCarbonLine=firstShellCarbonLine;	
                }                
                
                if (getString.regionMatches(0,"ATOM",0,4) && getString.regionMatches(13,bondedAtom,0,1) &&
                   (getString.regionMatches(16,"A",0,1) || Character.isWhitespace(getString.charAt(16)))) {
                    firstShellCarbonLine = getString.substring(30,38)+ " " + 
                    getString.substring(38,46)+ " " + getString.substring(46,54);                                 
                }
                
            }
            FileOperation.saveResults(resultList, currDirectory+ "/outputdata/"+ PDBID + "_cc.dat", "w");   //save results as file
            resultList.clear();
        }
    }
}