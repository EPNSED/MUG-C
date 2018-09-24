/** An application to create input files for the MatLab program
/*       that analyze the calsium-binding site.
/* Creat files with PDBID_m.dat as their names, which is the agjacency matrix 0f Oxygen
/*       atoms and the Carbon coordination that is bonded to Oxygen. 
/* This application uses class FileOperation in package utils
/*       and class ParsePDB in package pdbparser. 
*/

package src.parsepdb.mugcarbonlist;

import src.utils.*;
import java.util.*;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;

public class MetalBindingM{
    public static void getAdjacencyMatrix(String args, double cutOff){

        double threshold = cutOff;

        List fileList = new ArrayList();
        List oxyList = new ArrayList();      
        String oxyLine = "";
        int sizeOxyList; 
        double x = 0.0;
        double y = 0.0;
        double z = 0.0; 
        Dist dist = new Dist();
        String currDirectory = System.getProperty("user.dir");  
        fileList = FileOperation.getEntriesAsList(args); // a list of PDBIDs   
               

        for (int i = 0; i< fileList.size(); i++){   // iteratively process PDB files

            int countATOM = 0;
            int countHETATM = 0;
            String PDBID = ((String)fileList.get(i)).substring(0,4);
            String fileName = currDirectory + "/outputdata/"+ PDBID +"_f1.dat";
            
            oxyList = FileOperation.getEntriesAsList(fileName);
            sizeOxyList = oxyList.size();
            double[][] coordMatrix = new double[sizeOxyList][3];
            int indicationM = 0; 
            

            for (int j = 0; j < sizeOxyList; j++){    // get coordinate for each oxy atom

                oxyLine = (String)oxyList.get(j);
		        try {
		            x = Double.parseDouble(oxyLine.substring(33,42));
                    y = Double.parseDouble(oxyLine.substring(42,51));
                    z = Double.parseDouble(oxyLine.substring(51,59)); 
                 } catch(Exception e){System.out.println("Error!");}
                coordMatrix[j][0] = x;
                coordMatrix[j][1] = y;
                coordMatrix[j][2] = z;
                if ((oxyLine.startsWith("ATOM") || oxyLine.startsWith("HETATM")) && oxyLine.substring(20,23).contentEquals(new StringBuffer("HOH")) == false) {
                    countATOM = countATOM + 1;
                }      
                if (oxyLine.startsWith("HETATM") && oxyLine.substring(20,23).contentEquals(new StringBuffer("HOH")) == true) {
                    countHETATM = countHETATM + 1;
                }
                                 
            }
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(currDirectory+ "/outputdata/"+ PDBID + "_m.dat"));
                BufferedWriter bw2 = new BufferedWriter(new FileWriter(currDirectory+ "/outputdata/"+ PDBID + "_info1.dat"));
                            
                for (int j = 0; j < sizeOxyList; j++) {   // get indication matrix               
                    for (int k = 0; k < sizeOxyList; k++) {
                     
                        double distance = dist.distThreeD(coordMatrix[j][0],coordMatrix[j][1],coordMatrix[j][2],
                                                      coordMatrix[k][0],coordMatrix[k][1],coordMatrix[k][2]);                              
                        if (distance < threshold) {
                            indicationM = 1;                         
                        } 
                        else {
                            indicationM = 0;                        
                        }       
                        bw.write(Integer.toString(indicationM));
                        bw.write(" ");           
                    }
                    bw.write("\n"); 
                }
                       
                bw2.write(countATOM + " " + countHETATM + "\n");              
                bw.close();
                bw2.close();
            } catch(IOException e){System.out.println("Error!");} 
        }      
    }
}
