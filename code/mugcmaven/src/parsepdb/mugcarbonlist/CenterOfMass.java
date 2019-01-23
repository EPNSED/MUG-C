/**
*A parser to manipulate PDB files and return a standard PDB format where the side-chain was replaced by center of mass of side-chain as "R".
*@author: Kun Zhao
*@version:1.0
*@date: 2/11/2014 
**/

package mugcmaven.mugcmaven.src.parsepdb.mugcarbonlist;

import java.io.*;
import java.text.DecimalFormat;
import java.lang.*;
import mugcmaven.mugcmaven.src.utils.FileOperation;
import mugcmaven.mugcmaven.src.utils.ParsePDB;

import mugcmaven.mugcmaven.src.utils.*;
import java.util.*;

public class CenterOfMass {
	
    public static void getRCalpha(String args){


        List fileList = new ArrayList();
        List allAtom = new ArrayList();
        List resultList = new ArrayList();
        String atom="allatom";
        String carbonLine = "";
        String oxygenLine = "";
	    String currDirectory = System.getProperty("user.dir");
        
        fileList = FileOperation.getEntriesAsList(args); // a list of PDBIDs


        for (int i = 0; i< fileList.size(); i++){   // iteratively process PDB files

            String PDBID = ((String)fileList.get(i)).substring(0,4);
            String fileName = currDirectory+"/inputdata/"+ PDBID +".pdb";

            allAtom = ParsePDB.getLines(fileName, "ATOM", "Res", atom);            
            FileOperation.saveResults(simplifyModel(allAtom), currDirectory+ "/outputdata/"+ PDBID + "_COM.pdb", "w");   //save results as file
            resultList.clear();
        }
    }
    
	public static List simplifyModel(List allAtom){ 		
		
		List entries = new ArrayList();
		List sideChain= new ArrayList();
		
		//allAtom.size()
		for (int k = 0; k < allAtom.size(); k++){
			String getString = (String)allAtom.get(k);
			if (getString.regionMatches(13,"CB",0,2)!=true){
				entries.add(getString);
				if (getString.regionMatches(13,"CB",0,2)==true && getString.regionMatches(17,"GLY",0,3)==false){
					sideChain.add(getString);
				}
			}
			else{		
				int h=k;
				String scResidue=(((String)allAtom.get(h)).substring(23,26));
				while(h<allAtom.size()-1 && (getString.substring(23,26).equals(scResidue))){				
					sideChain.add((String)allAtom.get(h));
					h++;					
					scResidue=((String)allAtom.get(h)).substring(23,26);
				}
				if (h==allAtom.size()-1&&(getString.substring(23,26).equals(scResidue))){
					sideChain.add((String)allAtom.get(h));
				}
				
				entries.add(SideChainCenterOfMass.sideChainCalculation(sideChain));
				sideChain.clear();	
				
				//skip to the next residue.
				if (h<allAtom.size()-1){
					k=h-1;					
				}
				
				//deal with last residue
				if (h==allAtom.size()-1){
					break;
				}
			}
		}
		return entries;
	}

}
