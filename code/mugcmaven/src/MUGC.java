/**
*The main class of MUGC
*@author: Kun Zhao
*@version:1.0
*@date: 2/11/2014 
*Please cite: Zhao, K., Wang, X., Wong, H. C., Wohlhueter, R., Kirberger, M. P., Chen, G. and Yang, J. J. (2012), 
*Predicting Ca2+-binding sites using refined carbon clusters. Proteins, 80: 2666�2679. doi: 10.1002/prot.24149
**/

package mugcmaven.mugcmaven.src;
import java.util.*;
import java.io.*;
import mugcmaven.mugcmaven.src.fileutil.Copy;
import mugcmaven.mugcmaven.src.mugc.MugControllCarbonShell;
import mugcmaven.mugcmaven.src.parsepdb.mugcarbonlist.CenterOfMass;
import mugcmaven.mugcmaven.src.parsepdb.mugcarbonlist.CombineFiles;
import mugcmaven.mugcmaven.src.parsepdb.mugcarbonlist.MetalBindingCC;
import mugcmaven.mugcmaven.src.parsepdb.mugcarbonlist.MetalBindingM;
import mugcmaven.mugcmaven.src.parsepdb.mugcarbonlist.MetalBindingMetal;
import mugcmaven.mugcmaven.src.parsepdb.mugcarbonlist.NMRSeperate;
import mugcmaven.mugcmaven.src.utils.FileOperation;

public class MUGC {
	public static void main(String args[]) {
		
	    String currDirectory = System.getProperty("user.dir");
	    String fileName = currDirectory+"/inputdata/"+ "list.txt";
	    String NMRFileName="";
	    String[] argsMugC = new String[4];
	    List PDBID=new ArrayList();
	    List PDBIDList = new ArrayList();
	    List NMRModels=new ArrayList();
	    PDBID = FileOperation.getEntriesAsList(fileName);
	    
	    //set default parameters
	    //GridInterval for moving the calcium center at the most suitable positions.  Parameter -i
		String gridInterval = "4";
		//The lower limit of distance between carbon atoms and calcium center, e.g. observed from 2BBM.  Parameter -l
		String carbonLowerLimit = "1.74";
		//The upper limit of distance between carbon atoms and calcium center.  Parameter -u
		String carbonUpperLimit = "4.9";
	    // read parameters from user input.	
		if( args.length > 0){
			
			    if (Arrays.asList(args).indexOf("-i")!=-1){
			    	System.out.println(args[Arrays.asList(args).indexOf("-i")+1]);
			    }
				if ((Arrays.asList(args).indexOf("-l")) != -1){
					System.out.println(args[Arrays.asList(args).indexOf("-l")+1]);
				}
				if (Arrays.asList(args).indexOf("-u")!= -1){
					System.out.println(args[Arrays.asList(args).indexOf("-u")+1]);
				}
				if (Arrays.asList(args).indexOf("-help")!= -1){
					System.out.println("A Proper Usage example: java ./MugC -i 4 -l 1.74 -u 4.9");
					System.out.println("-i GridInterval for moving the calcium center at the most suitable positions. Default=4");
					System.out.println("-l The lower limit of distance between carbon atoms and calcium center. Default=1.74");
					System.out.println("-u The upper limit of distance between carbon atoms and calcium center. Default=4.9");
					System.exit(0);
				}
			
		}
		
		argsMugC[1] = gridInterval;
		argsMugC[2] = carbonLowerLimit;
		argsMugC[3] = carbonUpperLimit;	

		//Save, seperate and manipulate NMR structures. Return the total number of model in the NMR. 
		NMRModels=NMRSeperate.nMRSeperate(fileName);

		for (int j=0;j<PDBID.size();j++){
			
			//in case that only one NMR model
			if( NMRModels.isEmpty()){
				NMRFileName=currDirectory+ "/inputdata/list.txt";
			}
			
			else{				
				NMRFileName=currDirectory+ "/inputdata/"+ PDBID.get(j)+"list.txt";
			}
			
			PDBIDList = FileOperation.getEntriesAsList(NMRFileName);
			
			// Preparing the intermediate data (including the topology graph) for further processing 
			// extract Calcium from PDB file, if there is one.
			MetalBindingMetal.getMetal(NMRFileName, "CA");
			// extract the carbon atoms for the Carbon Shell.
			MetalBindingCC.getCC(NMRFileName);
			// genrate file from cc file, could be change to cc file;  
			CombineFiles.combineSurfix(NMRFileName, "cc");
			// calculate the topological graph based on adjacent matrix.
			MetalBindingM.getAdjacencyMatrix(NMRFileName,8.3);
			// calculate center of mass coordinates of each side-chain.
			CenterOfMass.getRCalpha(NMRFileName);
				
			for(int i=0; i<PDBIDList.size();i++){	
				
				argsMugC[0] = (String)(PDBIDList.get(i));
				MugControllCarbonShell runmugc = new MugControllCarbonShell();
				//System.out.println(argsMugC[0]);			
			    runmugc.Predict(argsMugC); 		    		
				    
	    	}
			
			
			//System.out.println("PDBIDList size:" +PDBIDList.size());
			//generate PDB prediction file.
			NMRSeperate.nMRmergePDB(PDBIDList,(String)PDBID.get(j));
		}
		
		System.out.println("Completed!");
	}

}


