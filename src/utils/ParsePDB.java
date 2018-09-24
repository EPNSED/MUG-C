/* This class is designed to parse out the useful lines in the PDB file
A PDB parser tool which take only parse out perticular lines in PDB without modifying or manipulating them.
*/

package src.utils;
import java.util.*;

public class ParsePDB {

	public static List getLines (String PDBFile, String field, String subfield,String atom){

        // Return a list, the element of which is a line in the file with name PDBFile
        // that contains the specified atom in a specified field.

            List returnList = new ArrayList();
            List fileList = new ArrayList();
            String line = "";
            int i = 0;
            
            if (atom!="allatom"){
            	if (atom.length() == 0 | atom.length() >= 4){
                	System.out.println("error!");
            	}
            }
            fileList = FileOperation.getEntriesAsList(PDBFile);

            for(i = 0; i < fileList.size(); i++){

                line = (String)fileList.get(i);

                if (field == "HETATM" && subfield != "HOH" && line.startsWith(field) && line.substring(17,20).trim().contentEquals(new StringBuffer(atom))) {
                    returnList.add(line);
                }
                if (field == "ATOM" && subfield == "Res" && line.startsWith(field) && line.substring(13,17).startsWith(atom)) {
                    returnList.add(line);
                }
                if (field == "HETATM" && subfield == "HOH" && line.startsWith(field) && line.substring(12,14).startsWith(atom) &&
                    line.substring(17,20).startsWith(subfield)) {
                    returnList.add(line);
                }
                if (subfield == "HET"){
                    if (field == "HETATM" && subfield != "HOH" && line.startsWith(field) && line.substring(12,14).startsWith(atom) &&
                        line.substring(17,20).contentEquals(new StringBuffer("HOH")) == false) {
                        returnList.add(line);
                    }
                }

                //get only all atom residues without water
                if (field == "ATOM" && subfield == "ResNoWater" && (line.startsWith("ATOM")||line.startsWith("HETATM")) && atom=="allatom") {
                    if (line.substring(17,20).contentEquals(new StringBuffer("HOH")) == false){
                    	returnList.add(line);
                    }                    
                }
                
                //get all atom residues
                if (field == "ATOM" && subfield == "Res" && (line.startsWith("ATOM")||line.startsWith("HETATM") )&& atom=="allatom" ) {
                    returnList.add(line);
                }
            }
            return returnList;
	}

        public static List getBondedLines(String PDBFile, String atom, String bondedAtom){

        // Return a list, the element of which is a line in the file with name PDBFile
        // that contains the specified atom and the atom bonded to it.

            List returnList = new ArrayList();
            List carbonList = new ArrayList();
            List oxygenList = new ArrayList();
            List fileList = new ArrayList();
            String line = "";
            int i = 0;

            if (atom.length() == 0 || atom.length() > 1 || bondedAtom.length() == 0 || bondedAtom.length() > 1){

                System.out.println("error!");
            }

            atom = atom.toUpperCase();
            bondedAtom = bondedAtom.toUpperCase();
            String temp = "";

            FileOperation fileOpera = new FileOperation();
            fileList = fileOpera.getEntriesAsList(PDBFile);  // contain the lines in PDB file

            for(i = 0; i < fileList.size(); i++){

                line = (String)fileList.get(i);   // The ith line in PDB file

                if (line.regionMatches(0,"ATOM",0,4) && line.regionMatches(13,bondedAtom,0,1)) {
                    carbonList.add(line);
                    if (temp.contentEquals(new StringBuffer("O")) && oxygenList.isEmpty() == false){
                        for (int k = 0; k < oxygenList.size(); k++) {
                            returnList.add((String)oxygenList.get(k));
                        }
                    }
                    oxygenList.clear();
                    temp = line.substring(13,14);
                }

		if (line.regionMatches(0,"ATOM",0,4) && line.regionMatches(13,atom,0,1)) {
                    oxygenList.add(line);
                    if (temp.contentEquals(new StringBuffer("C")) && carbonList.isEmpty() == false) {
                        for (int k = 0; k < carbonList.size(); k++){
                            returnList.add((String)carbonList.get(k));
                        }
                    }
                    carbonList.clear();
                    temp = line.substring(13,14);
                }

                if((line.regionMatches(0,"TER",0,4) || line.regionMatches(0,"HETATM",0,6) ) && temp.length()!= 0) {
                    for (int k = 0; k < oxygenList.size(); k++){
                        returnList.add((String)oxygenList.get(k));
                    }
                    oxygenList.clear();
                    temp = "";
                }
            }
            return returnList;
	}
}




























