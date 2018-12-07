import java.util.*;
import java.io.*;
import java.net.*;
import src.utils.*;
import java.util.*;
import java.text.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.text.DecimalFormat;
import java.lang.*;
import java.io.BufferedWriter;

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

public class MugControllCarbonShell {

	int infor1;
	int infor2;
	int predictionCount = 0;
	int arraySize = 0;
	double gridInterval,carbonUpperLimit, carbonLowerLimit;
	private Map maxClique = new HashMap();
	private int count_maxClique=0;
	/* 
	%
	% Abstract: 
	    % Predict Calcium location and calcim-binding ligands
	    % Return tp_ind_array. If a clique of size >= 4 is a true prediction,
	    % the corresponding element in tp_ind_array is the index of the site that it
	    % predicts; if that clique is not a true prediction (either not putative or not true), the
	    % corresponding element is 0. 
	    % Find all the maximal cliques of size >= 4, where C_m is the5 adjacency
	    % matrix.  
	    % Call function "judgeA" to evaluate the found maximal cliques.A found clique is a 
	    % potential validated predicted Ca-binding site.  
	*/
	
	public void Predict(String[] args) {
		
		  String fileName = args[0];
		  gridInterval=Double.parseDouble(args[1]);
		  carbonLowerLimit=Double.parseDouble(args[2]);
		  carbonUpperLimit=Double.parseDouble(args[3]);
	      List inforFile = new ArrayList();
	      String currDirectory = System.getProperty("user.dir");
	      inforFile = FileOperation.getEntriesAsList(currDirectory+"/outputdata/"+fileName + "_info1.dat");
	      
	      List ocFile = new ArrayList();
	      ocFile = FileOperation.getEntriesAsList(currDirectory+"/outputdata/" +fileName + "_f1.dat");
          arraySize = ocFile.size()+10;
	      List mFile = new ArrayList();
	      mFile = FileOperation.getEntriesAsList(currDirectory+"/outputdata/" +fileName + "_m.dat");
	      List rFile = new ArrayList();
	      rFile = FileOperation.getEntriesAsList(currDirectory+"/outputdata/" +fileName + "_COM.pdb");      
	      List allAtomNoWater = new ArrayList();
	      allAtomNoWater = FileOperation.getEntriesAsList(currDirectory+"/outputdata/" +fileName + "_noWater.dat");
	      
	      String inforString1 = (((String)inforFile.get(0)).split(" +"))[0];
	      String inforString2 = (((String)inforFile.get(0)).split(" +"))[1];
	      
	      infor1 = Integer.parseInt(inforString1);
	      infor2 = Integer.parseInt(inforString2);
		  
	      String[] indicationChar;
	      short[] matrixRow  = new short[ocFile.size()];
	      List compsub = new ArrayList();
	      List not = new ArrayList();
	      //candidate is the index of oxygen atoms in a cluster which has >=4 
		  List candidate = new ArrayList();
		  List compsub1=new ArrayList();
		  List compsub2=new ArrayList();
		  
		  //elasped time of finding maximum clique
		  long start = System.currentTimeMillis();
		  
		  for(int ii=0; ii<mFile.size(); ii++) {		   
			   int sum = 0;
			   indicationChar = ((String)mFile.get(ii)).split(" +");
			   for(int jj = 0; jj < mFile.size(); jj++){
			       matrixRow[jj] = (short)Integer.parseInt((indicationChar[jj]));
			   }
		       for(int jj = 0; jj < mFile.size(); jj++) {
			       sum = sum + matrixRow[jj];
			   }
		       if (sum >= 4) {
			       candidate.add(Integer.toString(ii));
			   }
		  }
		  
		  //find maximum clique, and set it in the map of maxClique.
		  fmxlcA(ocFile, compsub, candidate, not, fileName, mFile.size()); // Call fmxlcA
		  
		  FileOperation.saveResults("There are in total: "+maxClique.size()+" maximum cliques found.", currDirectory+ "/predictionResults/results.dat", "a");
		  for (int i=0;i<maxClique.size();i++){
			  List maximumClique=new ArrayList();
			  maximumClique=(ArrayList)maxClique.get(i+"");
			  //System.out.println(maximumClique.toString());
		  }
		  
		  // Get elapsed time in milliseconds
		  long elapsedTimeMillis = System.currentTimeMillis()-start;		    
		  // Get elapsed time in seconds
		  float elapsedTimeSec = elapsedTimeMillis/1000F;
		  //System.out.println(elapsedTimeSec);
		  start = System.currentTimeMillis();
		  
		  //judge all possible clusters and print results.
		  for (int i=0;i<maxClique.size();i++){
			  List maximumClique=new ArrayList();
			  maximumClique=(ArrayList)maxClique.get(i+"");
			  
			  // compsub contains the index of oxygens in a maximal clique of size >= 4.
			  //Iteratively check if each possible clusters is the potential binding sites or not.
			  for (int clusterSize=4;clusterSize<=maximumClique.size();clusterSize++){
					
				  //combination generator.
				  CombinationGenerator cG=new CombinationGenerator(clusterSize,maximumClique.size());
				  int totalCombination=cG.combinations();
					
				  // exhaust all possible combinations!!
				  for(int j=0; j<totalCombination;j++){
				    	
					  int[] clusterList;
					  clusterList=cG.next();
						
					  for(int reOrder=0; reOrder<clusterSize;reOrder++){
						  
						  //System.out.print(clusterList[reOrder]);
						  //add value each subclique in a maximum clique.  
						  compsub1.add(maximumClique.get(clusterList[reOrder]));
					  }						
					  
					  //System.out.print("\n");
					  
					  //remove the repeated clusters existing in different cliques.
					  String compsub1String=compsub1.toString();					  
					  if (compsub2.contains(compsub1String)==false){
						  compsub2.add(compsub1String);
						  //System.out.println("===============compsub1String :"+ compsub1String);
						  //judge the putative binding site.
						  judgeA(ocFile, compsub1, fileName,rFile, allAtomNoWater);  
					  }
					  					  
					  compsub1.clear();
				  }
			  }
		  } // end exhausting all pockets.		  

		  // Get elapsed time in milliseconds
		  long elapsedTimeMillis2 = System.currentTimeMillis()-start;		    
		  // Get elapsed time in minutes
		  float elapsedTimeMin2 = elapsedTimeMillis2/(60*1000F);
		  //System.out.println(elapsedTimeMin2);
		  
	} //end predict ()
	
	private int fmxlcA(List ocFile, List compsub, List candidate, List not, String fileName, int oxygenNum){
		//don't need to change
        /*	% Abstract: 
		    % Find all the maximal cliques of size >= 4, where C_m is the adjacency
		    % matrix.  
		    % Call function "judgeA" to evaluate the found maximal cliques.
		% Version: 2.0
		% Author: Kun Zhao
		% Date: 2/1/2014
		% Version: 1.2
		% Author: Xue Wang
		% Date: 7/25/2007
		% Replace version 1.1
		% Author: Hai Deng 
		% Date: Sep., 2006
		*/
		int count_prd = 0;
		int jj = 0, ii = 0, k;
		List mFile = new ArrayList(arraySize);
		short[] matrixRow = new short[ocFile.size()];
		String currDirectory = System.getProperty("user.dir");
		
		for (jj =0; jj < not.size(); jj++){
		    int bound = 1;
			mFile = FileOperation.getEntriesAsList(currDirectory+"/outputdata/" +fileName + "_m.dat");
			int notPlace = Integer.parseInt((String)not.get(jj));
			String[] indicationChar =((String)mFile.get(notPlace)).split(" +");
			for(k=0;k<mFile.size();k++){
			     matrixRow[k] = (short)Integer.parseInt((indicationChar[k]));
			}
			for (k=0;k < candidate.size();k++){
				if (matrixRow[Integer.parseInt((String)candidate.get(k))] == 0){							
				    bound=0;
				    break;
				}
			}
			if (bound == 1){
	            return count_prd;    // branch and bound
			}             		
		}  
		
		//condition of maximum clique
		if(candidate.size() == 0){
			if(compsub.size() >= 4) {
				List maxCliqueList=new ArrayList();
				maxCliqueList.addAll(compsub);
				maxClique.put(count_maxClique+"",maxCliqueList);
				count_maxClique++;
			}
		}
		else {	
			for(int i=0; i < candidate.size(); i++){    // loop through all items in candidate
				
				String cand = (String)candidate.get(i);
                compsub.add(cand);
                List candidate1 = new ArrayList();
                List not1 = new ArrayList();
                mFile = FileOperation.getEntriesAsList(currDirectory+"/outputdata/" +fileName + "_m.dat");
				int candPlace = Integer.parseInt(cand);
				String[] indicationChar =((String)mFile.get(candPlace)).split(" +");
				for(k=0;k<mFile.size();k++){
				     matrixRow[k] = (short)Integer.parseInt((indicationChar[k]));
				}
				
				for(k=i+1; k < candidate.size(); k++) {       // find the adjacency list of an oxygen in candidate
					int candidatePlace = Integer.parseInt((String)(candidate.get(k)));
					if(matrixRow[candidatePlace] == 1){
						candidate1.add(candidate.get(k));
					}
				}
				for (int j =0; j< not.size(); j++){
					if (matrixRow[Integer.parseInt((String)not.get(j))]==1){
	            		not1.add(not.get(j));
					}
				}
				
				//condition for search maximum clique of 
				fmxlcA(ocFile, compsub, candidate1,not1,fileName,oxygenNum); // recursive call 
	            

				if (compsub.size() >= 1) {
	                compsub.remove(compsub.size()-1);
				}
				not.add(cand);
			}
		}
		return count_prd;
	}
	
		
	private void judgeA(List ocFile, List compsub, String fileName, List rFile, List allAtomNoWater){
		//-----------------------------need to modify----------------------------------------//
		/*% Filename: judgeA.c
		%
		% Abstract: 
		    % Return count_prd (either 1 or 0) to indicate if a maximal clique is a putative Ca-binding site.
		    % Modify tp_array to indicate if a clique is a prediction
		%
		% Version: 0.3
		% Author: Kun Zhao
		% Date: 06/06/2009
		% Version: 0.2
		% Author: Xue Wang
		% Date: 1/25/2008
		% Replace version 0.1
		% Author: Xue Wang
		% Date: Jun., 2007
		*/
 
		    int count_prd = 0, lengthLocat = 0;
			double angleVal = 0, countCharge = 0;
			int	flag = 0;   // indicate that current clique contains putative binding site
			int ii, kk, jj = 0;  
			double[] x = new double[3]; // intitialize calcium location.

			String currDirectory = System.getProperty("user.dir");

			for(ii = 0; ii<compsub.size(); ii++) {
				if(Integer.parseInt((String)(compsub.get(ii)))< infor1) {
				    lengthLocat++;
				} 
			}

			if (lengthLocat < 4){
		          flag = 2;
			}

			for(int z=0; z< compsub.size(); z++){
				String ocLine = (String)ocFile.get(Integer.parseInt((String)compsub.get(z)));
			    //System.out.print(ocLine.split(" +")[4] + " ");
			}			
		   
			for (ii = 0; ii<lengthLocat; ii++){
				String ocLine = (String)ocFile.get(Integer.parseInt((String)compsub.get(ii)));
				String resiName = (ocLine.split(" +"))[3];
				String atomName = (ocLine.split(" +"))[2];
				
		        if ((resiName.equals("ASP")|| resiName.equals("GLU"))
					&& atomName.equals("O") == false){ 
		             countCharge = countCharge + 1;
				}
		        
		        if (resiName.equals("ASN")){ 
			             countCharge = countCharge + 0.5;
					}   
			}
			
			
			if (flag == 0){	
	
				ConsecutiveResiduesFilter conseResFil=new ConsecutiveResiduesFilter();
				conseResFil.setParameters(ocFile, compsub, infor1);
				flag=conseResFil.getFlag();
				
				// Distance and angle Filters 
				// Find the max and min distance between an first shell carbon and the calcium center; return the coordinates of calcium center;
				//charge filter 
				if(countCharge < 1 && flag==0){
					
					 flag=1;
					 
				} 
			
				if (countCharge >= 1 && flag==0){
					
					 double[] p = new double[5];
					 // carbon shell charge filter. 
					 p=ChargeDistanceFilter.filter_distance(flag, ocFile, fileName, compsub, lengthLocat, carbonUpperLimit, carbonLowerLimit, infor1, gridInterval);
				     				    
				     x[0] = p[0];
					 x[1] = p[1];
		             x[2] = p[2];
					 flag = (int)p[4];
										 
				}
				
				//center of mass filter, carbon, mainchain oxygen, and atom clash filter.				
				
				if (flag==0){	
					
					flag=CenterOfMassFilter.rFilter(x, ocFile, rFile, compsub, allAtomNoWater);
					
				}			
								
			}		
			
		    // Output the predicted ligand group and calcium location 
			if (flag == 0) {
			 	 PutativeTrue.doPrint(ocFile, compsub, fileName, x);
			}			
			
			//System.out.print("\n");
			//System.out.print(flag);
			//System.out.print("\n");
		}
	
}

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

public class CenterOfMassFilter {
	
	public static int rFilter(double[] p, List ocFile, List rFile, List compsub, List allAtomNoWater){
		
		int flag=0;
		int i=0,ii=0;
		int j=0;
		int k=0, resiNum,comResiNum, resiNumPre=0; 
		double hydroPhIndex=0;
		String ocLine, atomName, resiName, chainName, comChainName, rLine, atomType;
		List comFile = new ArrayList();
		List allAtomFile = new ArrayList();
		
		
		//get the comFile which contains the center of mass and main-chain information of binding ligand.
		// also get the allatom file without water and check hydrophobic index
		while(flag==0 && i<compsub.size()){
			
			//get the binding rFile
			ocLine=(String) ocFile.get((Integer.parseInt((String)compsub.get(i))));
	    	atomName = ocLine.split(" +")[2];
	    	resiName = ocLine.split(" +")[3];
	    	resiNum =  Integer.parseInt(ocLine.split(" +")[4]);	
			chainName= ocLine.split(" +")[11];

		  //remove the binding residue from all atom.
				
                int indexBinding=0;
                while (indexBinding<allAtomNoWater.size()){

                        String allLine=(String)allAtomNoWater.get(indexBinding);
				if (Integer.parseInt(allLine.substring(23,26).trim())==resiNum && allLine.substring(17,20).equals(resiName) && allLine.substring(77,78).equals("C")){
                                allAtomNoWater.remove(indexBinding);
                                indexBinding=indexBinding-1;
                                System.out.println(indexBinding);
                        }

                        indexBinding++;

                } 


	    	//hydrophobic index
		if (resiNum!=resiNumPre){
			hydroPhIndex=hydroPhIndex+hydroIndex(resiName);
		}
		resiNumPre=resiNum;
			
			//get comFile
	    	for (int z=0;z<rFile.size();z++){
	    	
	    		rLine=(String) rFile.get(z);
	    		comResiNum=Integer.parseInt(rLine.substring(23,26).trim());
	    		comChainName=rLine.split(" +")[4];
	    		
	    		if (resiNum==comResiNum && chainName.equals(comChainName)){
	    			comFile.add(rLine);
		    		//indicate binding ligand found
		    		k=1;
	    		}
	    		
		    	if(k==1 && resiNum!=comResiNum){
		    		k=0;
		    		break;
		    	}	    			    			    		

	    	}
	    	  		    	
	    	i++;			
		} //end while
		
		if (hydroPhIndex>0){
			flag=1;
		}
		
		//System.out.println(hydroPhIndex);
		
		String atomComName, resiComName; int resiComNum;
		double mainChainCarbonX, mainChainCarbonY, mainChainCarbonZ,mainChainOxygenX,
		mainChainOxygenY,mainChainOxygenZ, distCaMChainO=0,distCaMChainC,distCaMChainR=0,
		mainChainComX,mainChainComY,mainChainComZ;
						
		//mainchain oxygen, carbon and center of mass filter.
		while(flag==0 && j<compsub.size()){
			
			// binding residue
			ocLine=(String) ocFile.get((Integer.parseInt((String)compsub.get(j))));
	    	atomName = ocLine.split(" +")[2];
	    	resiName = ocLine.split(" +")[3];
	    	resiNum =  Integer.parseInt(ocLine.split(" +")[4]);	
	    		    	
	    	//calculate the distance of main chain oxygen and calcium
	    	if (atomName.equals("O") && flag==0){
	    		
	    		//get the main-chain carbon atom coordinates.
		    	mainChainCarbonX =  Double.parseDouble(ocLine.split(" +")[5]);
		    	mainChainCarbonY =  Double.parseDouble(ocLine.split(" +")[6]);
		    	mainChainCarbonZ =  Double.parseDouble(ocLine.split(" +")[7]);
		    	
		    	//get the main-chain oxygen atom coordinates.
		    	for (ii=0;ii<comFile.size();ii++){
		    		
		    		rLine=(String) comFile.get(ii);
			    	atomComName = rLine.split(" +")[2];
			    	resiComName = rLine.split(" +")[3];
			    	resiComNum =  Integer.parseInt(rLine.substring(23,26).trim());	

		    		if (resiNum==resiComNum && atomComName.equals("O") ){
		    			
				    	mainChainOxygenX =  Double.parseDouble(rLine.split(" +")[6]);
				    	mainChainOxygenY =  Double.parseDouble(rLine.split(" +")[7]);
				    	mainChainOxygenZ =  Double.parseDouble(rLine.split(" +")[8]);
				    	
			    		distCaMChainO= Dist.distThreeD(p[0], p[1], p[2], 
		                        mainChainOxygenX,mainChainOxygenY,mainChainOxygenZ);

				    	break;
		    		}
		    		
		    	} //end for
    	
	    		distCaMChainC= Dist.distThreeD(p[0], p[1], p[2], 
                        mainChainCarbonX,mainChainCarbonY,mainChainCarbonZ);
	    		
	    		if (distCaMChainC< distCaMChainO){
	    			flag=1;
	    		}
	    	} //end if and mainchain oxygen filter.
	    	
	    	
	    	//center of mass filters and another oxygen position filter.
	    	if (atomName.equals("O")==false && flag==0){
	    		
	    		//get the main-chain carbon atom coordinates.
		    	mainChainCarbonX =  Double.parseDouble(ocLine.split(" +")[5]);
		    	mainChainCarbonY =  Double.parseDouble(ocLine.split(" +")[6]);
		    	mainChainCarbonZ =  Double.parseDouble(ocLine.split(" +")[7]);
		    	
		    	for (ii=0;ii<allAtomNoWater.size();ii++){
		    		
		    		rLine=(String) allAtomNoWater.get(ii);
			    	atomComName = rLine.split(" +")[2];
			    	resiComName = rLine.split(" +")[3];
			    	resiComNum =  Integer.parseInt(rLine.split(" +")[5]);			    		
		    		
		    		if (resiNum==resiComNum && atomComName.equals(atomName) ){
		    			
				    	mainChainComX =  Double.parseDouble(rLine.split(" +")[6]);
				    	mainChainComY =  Double.parseDouble(rLine.split(" +")[7]);
				    	mainChainComZ =  Double.parseDouble(rLine.split(" +")[8]);
				    	
			    		distCaMChainR= Dist.distThreeD(p[0], p[1], p[2], 
		                        mainChainComX,mainChainComY,mainChainComZ);
				    	distCaMChainC= Dist.distThreeD(p[0], p[1], p[2], 
		                        mainChainCarbonX,mainChainCarbonY,mainChainCarbonZ);

			    		if ((distCaMChainC< distCaMChainR || distCaMChainC<2.7) && !resiName.equals("ASP")&& !resiName.equals("GLU")){
			    			flag=1;
			    		}

				    	break;
		    		}
		    		
		    		if (resiNum==resiComNum && atomComName.equals("R") ){
		    			
				    	mainChainComX =  Double.parseDouble(rLine.split(" +")[6]);
				    	mainChainComY =  Double.parseDouble(rLine.split(" +")[7]);
				    	mainChainComZ =  Double.parseDouble(rLine.split(" +")[8]);
				    	
			    		distCaMChainR= Dist.distThreeD(p[0], p[1], p[2], 
		                        mainChainComX,mainChainComY,mainChainComZ);
				    	distCaMChainC= Dist.distThreeD(p[0], p[1], p[2], 
		                        mainChainCarbonX,mainChainCarbonY,mainChainCarbonZ);
				    	
				    	if (4.3< distCaMChainR){
				    		flag=1;
				    	}	

				    	break;
		    		}
	    		
		    	}//end for

		    	
	    	} //end if and center of mass filter
				    		    	
	    	j++;
	    	
		} //end while filter
		
		//check clash.
		if (flag==0){
						
			for (int l = 0; l < allAtomNoWater.size(); l++){
				
				//System.out.println(l);
				rLine=(String) allAtomNoWater.get(l);
		    	mainChainComX =  Double.parseDouble(rLine.split(" +")[6]);
		    	mainChainComY =  Double.parseDouble(rLine.split(" +")[7]);
		    	mainChainComZ =  Double.parseDouble(rLine.split(" +")[8]);

	    		distCaMChainC= Dist.distThreeD(p[0], p[1], p[2], 
	    				mainChainComX,mainChainComY,mainChainComZ);
		    	
		    	atomType=rLine.substring(77,78);
		    	resiNum=Integer.parseInt(rLine.substring(23,26).trim());
		    	
		    	if (atomType.equals("N") && distCaMChainC<2.55){
		    		flag=1;
		    		break;
		    	}
		    	
		    	if (atomType.equals("C") && distCaMChainC<2.7){
		    		flag=1;
		    		break;
		    	}
		    	
		    	if (atomType.equals("O")){
		    		if (distCaMChainC<1.6){
		    			flag=1;
		    			break;
		    		}		    		
		    	}
		    	
		    	
		    	if (atomType.equals("H") && distCaMChainC<2){
		    		flag=1;
		    		break;
		    	}
		    	

		    }			
		}
		
		return flag;
	}
	
	private static double hydroIndex(String resiName){
		
		double hydroIndex=0;
		if (resiName.equals("ALA")){
			hydroIndex=0.67;
		}
		if (resiName.equals("ARG")){
			hydroIndex=-2.1;
		}
		if (resiName.equals("ASN")){
			hydroIndex=-0.6;
		}
		if (resiName.equals("ASP")){
			hydroIndex=-1.2;
		}
		if (resiName.equals("CYS")){
			hydroIndex=0.38;
		}
		if (resiName.equals("GLN")){
			hydroIndex=-0.22;
		}
		if (resiName.equals("GLU")){
			hydroIndex=-0.76;
		}
		if (resiName.equals("GLY")){
			hydroIndex=0;
		}
		if (resiName.equals("HIS")){
			hydroIndex=0.64;
		}
		if (resiName.equals("ILE")){
			hydroIndex=1.9;
		}
		if (resiName.equals("LEU")){
			hydroIndex=1.9;
		}
		if (resiName.equals("LYS")){
			hydroIndex=-0.57;
		}
		if (resiName.equals("MET")){
			hydroIndex=2.4;
		}
		if (resiName.equals("PHE")){
			hydroIndex=2.3;
		}
		if (resiName.equals("PRO")){
			hydroIndex=1.2;
		}
		if (resiName.equals("SER")){
			hydroIndex=0.01;
		}
		if (resiName.equals("THR")){
			hydroIndex=0.52;
		}
		if (resiName.equals("TRP")){
			hydroIndex=2.6;
		}
		if (resiName.equals("TYR")){
			hydroIndex=1.6;
		}
		if (resiName.equals("VAL")){
			hydroIndex=1.5;
		}
		return hydroIndex;
	}
}

public class ChargeDistanceFilter {
	
	//filter method
	public static double[] filter_distance(int flag, List ocFile, String filename,
			List compsub, int lengthLocat, double carbonUpperLimit, 
			double carbonLowerLimit, int infor1, double gridInterval) {
		
		int ii = 0, index = 0, lengthCompsub = compsub.size();
		double xx =0, yy = 0, zz = 0;
		double[] x0 = new double[3];
		double max_dist=0.0, min_dist=100.0, min_ind=0.0, max_ind = 0.0, ratio_f = 0.0, distC_f; 
		double dist_carbon_oxy,dist_carbon_ca, dist1, dist2, angle1, angle2, dist_carbon_oxy2,dist_carbon_ca2, dihedralAngle=0;
		double[] outputArray = new double[5];
		double exitflag = 0.0, fval = 0.0, fval2=0;
		double[] x = new double[5];
	    double oxygenX = 0, oxygenY = 0, oxygenZ = 0, carbonX = 0, carbonY = 0, carbonZ = 0;
	    double oxygenXPlus = 0, oxygenYPlus = 0, oxygenZPlus = 0, oxygenXMinors = 0, oxygenYMinors = 0, oxygenZMinors = 0, 
	           carbonXPlus = 0, carbonYPlus = 0, 
	           carbonZPlus = 0, carbonXMinors= 0, carbonYMinors = 0, carbonZMinors = 0;
	    String ocLine="", resiName="", resiNamePlus = "", ocLinePlus="", ocLineMinors="", atomField="";
	    	    
	    while(ii < compsub.size() && flag==0){
			ocLine = (String)ocFile.get(Integer.parseInt((String)compsub.get(ii)));
			oxygenX = Double.parseDouble((ocLine.split(" +"))[5]);
			oxygenY = Double.parseDouble((ocLine.split(" +"))[6]);
			oxygenZ = Double.parseDouble((ocLine.split(" +"))[7]);
	        xx = xx + oxygenX;
	        yy = yy + oxygenY; 
	        zz = zz + oxygenZ;
	        ii++;
		}
		x0[0] = xx/lengthCompsub; x0[1] = yy/lengthCompsub; x0[2] = zz/lengthCompsub;
		
		outputArray=PositionFun.positionFun(x0, compsub, ocFile, gridInterval);
		
		x[0] = outputArray[0];
		x[1] = outputArray[1];
	    x[2] = outputArray[2];
	    x[3] = outputArray[4];
	    exitflag  = outputArray[3];
		fval = outputArray[4];
		

	    if(exitflag < 0.5){
	         flag = 1;
	    } 
		
		for(ii=0; ii< lengthCompsub; ii++){
			ocLine = (String)ocFile.get(Integer.parseInt((String)compsub.get(ii)));
			resiName = (ocLine.split(" +"))[3];
			atomField = (ocLine.split(" +"))[0];
			carbonX = Double.parseDouble((ocLine.split(" +"))[8]);
			carbonY = Double.parseDouble((ocLine.split(" +"))[9]);
			carbonZ = Double.parseDouble((ocLine.split(" +"))[10]);
			if(resiName.equals("HOH") == false && atomField.equals("HETATM") == false){
	             	fval2 = fval2+Dist.distThreeD(x[0], x[1], x[2], carbonX, carbonY, carbonZ);	
					index++;
			}
		}
		fval2 = fval2/index;
	    
	    if(exitflag > 0.5) {
			for(ii=0; ii< lengthCompsub; ii++){
				ocLine = (String)ocFile.get(Integer.parseInt((String)compsub.get(ii)));
				oxygenX = Double.parseDouble((ocLine.split(" +"))[5]);
				oxygenY = Double.parseDouble((ocLine.split(" +"))[6]);
				oxygenZ = Double.parseDouble((ocLine.split(" +"))[7]);
				carbonX = Double.parseDouble((ocLine.split(" +"))[8]);
				carbonY = Double.parseDouble((ocLine.split(" +"))[9]);
				carbonZ = Double.parseDouble((ocLine.split(" +"))[10]);
				resiName = (ocLine.split(" +"))[3];
				atomField = (ocLine.split(" +"))[0];
				if(ii == lengthLocat-1 ){
					ocLineMinors = (String)ocFile.get(Integer.parseInt((String)compsub.get(ii-1)));
					oxygenXMinors = Double.parseDouble((ocLineMinors.split(" +"))[5]);
					oxygenYMinors = Double.parseDouble((ocLineMinors.split(" +"))[6]);
					oxygenZMinors = Double.parseDouble((ocLineMinors.split(" +"))[7]);
					carbonXMinors = Double.parseDouble((ocLineMinors.split(" +"))[8]);
					carbonYMinors = Double.parseDouble((ocLineMinors.split(" +"))[9]);
					carbonZMinors = Double.parseDouble((ocLineMinors.split(" +"))[10]);
					resiNamePlus = (ocLine.split(" +"))[3];
				}else if(ii == 0 ){
					ocLinePlus = (String)ocFile.get(Integer.parseInt((String)compsub.get(ii+1)));
					oxygenXPlus = Double.parseDouble((ocLinePlus.split(" +"))[5]);
					oxygenYPlus = Double.parseDouble((ocLinePlus.split(" +"))[6]);
					oxygenZPlus = Double.parseDouble((ocLinePlus.split(" +"))[7]);
					carbonXPlus = Double.parseDouble((ocLinePlus.split(" +"))[8]);
					carbonYPlus = Double.parseDouble((ocLinePlus.split(" +"))[9]);
					carbonZPlus = Double.parseDouble((ocLinePlus.split(" +"))[10]);
					resiNamePlus = (ocLinePlus.split(" +"))[3];
				}
				
				if(ii > 0 && ii<lengthLocat-1){
					ocLinePlus = (String)ocFile.get(Integer.parseInt((String)compsub.get(ii+1)));
					ocLineMinors = (String)ocFile.get(Integer.parseInt((String)compsub.get(ii-1)));
					oxygenXPlus = Double.parseDouble((ocLinePlus.split(" +"))[5]);
					oxygenYPlus = Double.parseDouble((ocLinePlus.split(" +"))[6]);
					oxygenZPlus = Double.parseDouble((ocLinePlus.split(" +"))[7]);
					carbonXPlus = Double.parseDouble((ocLinePlus.split(" +"))[8]);
					carbonYPlus = Double.parseDouble((ocLinePlus.split(" +"))[9]);
					carbonZPlus = Double.parseDouble((ocLinePlus.split(" +"))[10]);
					resiNamePlus = (ocLinePlus.split(" +"))[3];
					oxygenXMinors = Double.parseDouble((ocLineMinors.split(" +"))[5]);
					oxygenYMinors = Double.parseDouble((ocLineMinors.split(" +"))[6]);
					oxygenZMinors = Double.parseDouble((ocLineMinors.split(" +"))[7]);
					carbonXMinors = Double.parseDouble((ocLineMinors.split(" +"))[8]);
					carbonYMinors = Double.parseDouble((ocLineMinors.split(" +"))[9]);
					carbonZMinors = Double.parseDouble((ocLineMinors.split(" +"))[10]);
				}
	            distC_f = Dist.distThreeD(x[0], x[1], x[2],oxygenX,oxygenY,oxygenZ); 
	            //
				if (max_dist<distC_f){
	                 max_dist=distC_f;
	                 max_ind=ii;               
				}                
				if(min_dist>distC_f){
	                 min_dist=distC_f;
	                 min_ind = ii;
				}
				if(Integer.parseInt((String)compsub.get(ii)) < infor1 && atomField.equals("HETATM")==false){ 
					
					//c1-c2 distance measurement.
					if (resiName.equals("ASP")==false && resiName.equals("GLU")==false ){        
	                    
						if (AngleCalculation.point3angle(x[0], x[1], x[2],oxygenX,oxygenY,oxygenZ, carbonX,carbonY,carbonZ) < 90){
	                        flag = 1;
						}
						
						
	                    dist_carbon_oxy = Dist.distThreeD(x[0], x[1], x[2],oxygenX,oxygenY,oxygenZ);       
	                    dist_carbon_ca = Dist.distThreeD(x[0], x[1], x[2],carbonX,carbonY,carbonZ);
	                    
	                    // could be changed
						if (dist_carbon_ca < dist_carbon_oxy){
	                        flag = 1;
						}
					}

					if (resiName.equals("ASP") == true || resiName.equals("GLU") == true){
	                    if ((ii == 0 && carbonX != carbonXPlus) || 
							(ii == lengthLocat-1 && carbonX != carbonXPlus) ||
							(ii > 0 && ii<lengthLocat-1 && carbonX != carbonXPlus && carbonX != carbonXMinors)){
	                       
	                       //monodenate
	                       if (AngleCalculation.point3angle(x[0], x[1], x[2], oxygenX,oxygenY,oxygenZ,
	                                                         carbonX, carbonY, carbonZ) < 70){
	                            flag = 1;
						   } 
	                       dist_carbon_oxy = Dist.distThreeD(x[0], x[1], x[2],oxygenX,oxygenY,oxygenZ);      
	                       dist_carbon_ca = Dist.distThreeD(x[0], x[1], x[2], carbonX, carbonY, carbonZ);
						}
					}
					
					if (ii < lengthCompsub-1 && flag==0){ // bidenated
	                        if  (resiName.equals("ASP") == true && resiNamePlus.equals("ASP") == true 
								    && carbonX == carbonXPlus
	                           ||(resiName.equals("GLU") == true && resiNamePlus.equals("GLU") == true 
							    	&& carbonX == carbonXPlus)) {
	                            dist1 = Dist.distThreeD(x[0],x[1],x[2],oxygenX,oxygenY,oxygenZ);
	                            dist2 = Dist.distThreeD(x[0],x[1],x[2],oxygenXPlus,oxygenYPlus,oxygenZPlus);
	                            dist_carbon_oxy = Dist.distThreeD(x[0],x[1],x[2], oxygenX,oxygenY,oxygenZ);      
	                            dist_carbon_ca = Dist.distThreeD(x[0],x[1],x[2],carbonX,carbonY,carbonZ);
	                            dist_carbon_oxy2 = Dist.distThreeD(x[0],x[1],x[2],oxygenXPlus,oxygenYPlus,oxygenZPlus);      
	                            dist_carbon_ca2 = Dist.distThreeD(x[0],x[1],x[2],carbonXPlus,carbonYPlus,carbonZPlus);
	                            angle1 = AngleCalculation.point3angle(x[0], x[1], x[2],oxygenX,oxygenY,oxygenZ, 
									                    carbonX,carbonY,carbonZ);
	                            
	                                 dihedralAngle = AngleCalculation.dihedral_angles(carbonX, carbonY,carbonZ, 
										                            oxygenXPlus,oxygenYPlus,oxygenZPlus,
										                            oxygenX, oxygenY,oxygenZ,
										                              x[0], x[1], x[2]);
	                                 /*
									 if (dihedralAngle < 130){
	                                     flag = 1;
									 }
									 */
	                                 
	                                 // Ca-C1-C2 angle: bidenate should be a straight line.
									 if ( (angle1 > 15 && angle1 < 165)){
	                                     flag = 1;
									 }
									 if ((dist_carbon_ca > dist_carbon_oxy ) ){
	                                      flag = 1;
									 }
	                    //     end;
							} //if
					}//if
				} //if
				
				//ii++;
				
			}//for
		}//if(exitflag)
	        
		
		//-----------------------------------------------------
		/*
		if (fval > 2.9){   
	          flag = 1;
		}
		if(ratio_f > 0.75) {        
	          flag = 1;
		}
		*/
		if (max_dist > carbonUpperLimit){
	          flag = 1;
		}
		if (min_dist < carbonLowerLimit){
	          flag = 1;
		}
		x[4] = (double)(flag);
	    return x;
	}

}

public class ConsecutiveResiduesFilter {
	
	private int infor1, flag;
	private List compsub = new ArrayList(); 
	private List ocFile = new ArrayList(); 
	
	public void setParameters(List ocFile_1, List compsub_1, int infor1_1){
		this.ocFile=ocFile_1;
		this.compsub=compsub_1;
		this.infor1=infor1_1;
	}
	
	
	public int getFlag(){
		flag=getflag();
		return flag;
	}
	
	
	private int getflag() {
		   int flag=0;
		   int tempR=-100, resiNum;
		   int m;
	       String ocLine = "";
	       String resiName = "";
		   for(m = 0; m < compsub.size(); m++){
			   ocLine = (String)ocFile.get(Integer.parseInt((String)compsub.get(m)));
			   resiName = (ocLine.split(" +"))[3];
			   resiNum = Integer.parseInt((ocLine.split(" +"))[4]);
			   if (Integer.parseInt((String)compsub.get(m)) < infor1 ){ 
				   if (tempR == resiNum) { // if two consecutive ligand oxygens in compsub come from the same residue, and 
		                  if (resiName.equals("ASP")== false && resiName.equals("GLU") == false 
							  && resiName.equals("ASN") == false && resiName.equals("THR")== false
							 ){  // % if the oxygens are not from ASP nor from GLU 
		                      flag=1;
		                      break;     
				           }
				   }
		           tempR = resiNum;
			   }
		   } 
		   return flag;
	}  	 
}

public class PositionFun {
	
	public static double[] positionFun(double[] x0, List compsub, List ocFile,double gridInterval){
		//find the calcium center.
			    int            ocFileSize = ocFile.size();
			    double         iii,jjj,kkk,dist_calcium_oxy, dist_calcium_carbon, angle, dist_sum, bondStrength, carbonBondStrength, angle_Ca_O_C, angle_dihedral, dist_deviation, dihedralAngle;
			    double         PG = 0, min_G = 10000, xStar = 1000, yStar = 1000, zStar = 1000, secondMax, maxValue, minValue, fval = 0, exitflag;
			    double         amount1, amount2, angle1, angle2, dist_calcium_oxy1,dist_calcium_oxy2;
			    double[]       gridPoint = new double[3];
			    double[]       fX       = new double[5];
			    int            indication_conf;        // boolean
			    int            count = 0;
				int            jj, ii, flag=0;
				int            lengthCompsub = compsub.size();
				double[]       outputArray = new double[5];    
				String         ocLine="";
			    AngleCalculation angleComp = new AngleCalculation();
			    int[]    compsubInt =  new int[compsub.size()];
			    int[]    resiNum = new int[ocFileSize];
			    double[] oxygenX = new double[ocFileSize];
			    double[] oxygenY = new double[ocFileSize];
			    double[] oxygenZ = new double[ocFileSize];
			    double[] carbonX = new double[ocFileSize];
			    double[] carbonY = new double[ocFileSize];
			    double[] carbonZ = new double[ocFileSize];
			    String[] atomField = new String[ocFileSize];
			    String[] resiName = new String[ocFileSize];
			    String[] atomName = new String[ocFileSize];
			    for(ii=0; ii <ocFile.size(); ii++){
			    	ocLine = (String)ocFile.get(ii);
			    	resiNum[ii] =  Integer.parseInt(ocLine.split(" +")[4]);
			    	oxygenX[ii] =  Double.parseDouble(ocLine.split(" +")[5]);
			    	oxygenY[ii] =  Double.parseDouble(ocLine.split(" +")[6]);
			    	oxygenZ[ii] =  Double.parseDouble(ocLine.split(" +")[7]);
			    	carbonX[ii] =  Double.parseDouble(ocLine.split(" +")[8]);
			    	carbonY[ii] =  Double.parseDouble(ocLine.split(" +")[9]);
			    	carbonZ[ii] =  Double.parseDouble(ocLine.split(" +")[10]);
			    	atomField[ii] = ocLine.split(" +")[0];
			    	resiName[ii] = ocLine.split(" +")[3];
			    	atomName[ii] = ocLine.split(" +")[2];
			    }
			    for(ii = 0;ii<compsub.size(); ii++){
			    	compsubInt[ii] = Integer.parseInt((String)compsub.get(ii));
			    }
			    /*************************************************************************************************/

			 for(iii = -1; iii <= 1; iii = iii + gridInterval){
			         for(jjj = -1; jjj <=1; jjj = jjj + gridInterval){
			              for(kkk = -1; kkk <= 1; kkk = kkk + gridInterval){
			                    
			                    gridPoint[0] = x0[0];//+ iii;     // grid point: x axis
			                    gridPoint[1] = x0[1];//+ jjj;     // grid point: y axis
			                    gridPoint[2] = x0[2];//+ kkk;     // grid point: z axis
			                    
			                    // initialization
			                    double[]  dist_max = new double[lengthCompsub];
			                  
			                    dist_sum           = 0;
			                    bondStrength       = 0;
			                    dist_deviation     = 0;
			                    carbonBondStrength = 0;
			                    angle_Ca_O_C       = 0; 
			                    angle_dihedral     = 0;
			                    flag               = 0;
			                    indication_conf    = 0;
			  
			                    /********** for the whole cluster       ***************************************************/
			                    count              = 0;             
			                    maxValue           = -1000;
			                    minValue           = 1000;
			                    secondMax          = -1000;
			                    ii=0;
			                    while (ii < lengthCompsub && flag==0) {
			                    	
			                        dist_calcium_oxy = Dist.distThreeD(gridPoint[0], gridPoint[1], gridPoint[2], 
			                                           oxygenX[compsubInt[ii]],oxygenY[compsubInt[ii]],oxygenZ[compsubInt[ii]]);  
			                        //first carbon shell
			                        if(dist_calcium_oxy > 5.2 || dist_calcium_oxy < 1.74) {
			                        	flag = 1;
			                        }
			                        else{
			                        	dist_max[count] = dist_calcium_oxy;
			                        	dist_sum = dist_sum + dist_calcium_oxy;
			                        	count = count + 1;
			                        	if(dist_calcium_oxy > maxValue) {
			                            	maxValue = dist_calcium_oxy;
			                        	}
			                        	if(dist_calcium_oxy < minValue){
			                        		minValue = dist_calcium_oxy;
			                        	}
			                        }
			                        
			                        ii++;
			                        
			                    }
	                            if(flag == 0){
			                        for(jj = 0; jj < count; jj++) {
			                             if (dist_max[jj] != maxValue && dist_max[jj] > 0) {
			                                 if( dist_max[jj] > secondMax) {
			                                     secondMax = dist_max[jj];
			                                 }
			                             }                         
			                        }
	                            }
			                    /******* for each oxygen atoms   ***************/
	                            if(flag == 0){
			                       for(ii = 0; ii < lengthCompsub; ii++) {
			                            dist_calcium_oxy = Dist.distThreeD(gridPoint[0], gridPoint[1], gridPoint[2], 
			                                             oxygenX[compsubInt[ii]], oxygenY[compsubInt[ii]], oxygenZ[compsubInt[ii]]);      // calicum-oxygen distance
			                                                                                 
			                            /*** four cases for atom not from ASP and GLU  ***/
			                            if (atomField[compsubInt[ii]].equals("HETATM") == true && resiName[compsubInt[ii]].equals("HOH") == false){
			                                bondStrength = bondStrength + 10*(dist_calcium_oxy - 3.5)*(dist_calcium_oxy - 3.5);
			                                angle = angleComp.point3angle(gridPoint[0], gridPoint[1], gridPoint[2], oxygenX[compsubInt[ii]], oxygenY[compsubInt[ii]], oxygenZ[compsubInt[ii]],
			                                                    carbonX[compsubInt[ii]], carbonY[compsubInt[ii]], carbonZ[compsubInt[ii]]);
			                                if(angle < 140){
			                                    angle_Ca_O_C = angle_Ca_O_C +(140 - angle)*(140 - angle)/400;
			                                 }                                
			                            }
			                            else if (resiName[compsubInt[ii]].equals("HOH") == true) {
			                                 if (dist_calcium_oxy < 3.3){ 
			                                        bondStrength = bondStrength + 10*(dist_calcium_oxy - 3.5)*(dist_calcium_oxy - 3.5);
			                                        if (dist_calcium_oxy < 1.3) {
			                                            bondStrength = bondStrength + 400*(dist_calcium_oxy - 3.5)*(dist_calcium_oxy - 3.5);
			                                        }       
			                                 }              
			                                 else {
			                                      indication_conf = 1;                              
			                                 }
			                            }
			                            else if (resiName[compsubInt[ii]].equals("HOH") == false && atomName[compsubInt[ii]].equals("O") == true
			                                && atomField[compsubInt[ii]].equals("ATOM") == true) {
			                                dist_calcium_carbon = Dist.distThreeD(gridPoint[0], gridPoint[1], gridPoint[2], 
			                                                              carbonX[compsubInt[ii]],carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]]);
			                                //calcium to oxygen penalty
			                                bondStrength = bondStrength + 10*(dist_calcium_oxy - 3.5)*(dist_calcium_oxy - 3.5);
			                                //calcium to carbon penalty
			                                carbonBondStrength = carbonBondStrength + 10*(dist_calcium_carbon - 4.3)*(dist_calcium_carbon - 4.3);
			                                angle = angleComp.point3angle(gridPoint[0], gridPoint[1], gridPoint[2],oxygenX[compsubInt[ii]],oxygenY[compsubInt[ii]],oxygenZ[compsubInt[ii]],
			                                                    carbonX[compsubInt[ii]],carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]]);
			                                if(angle < 155){
			                                    angle_Ca_O_C = angle_Ca_O_C +(155 - angle)*(155 - angle)/400;
			                                 }                                
			                            }
			                            else if (resiName[compsubInt[ii]].equals("ASP") == false && resiName[compsubInt[ii]].equals("GLU") == false &&
			                                resiName[compsubInt[ii]].equals("THP") == false && atomName[compsubInt[ii]].equals("O") == false) {
			                                dist_calcium_carbon = Dist.distThreeD(gridPoint[0], gridPoint[1], gridPoint[2], 
			                                                             carbonX[compsubInt[ii]],carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]]); 
			                                bondStrength = bondStrength + 10*(dist_calcium_oxy - 3.5)*(dist_calcium_oxy - 3.5);
			                                carbonBondStrength = carbonBondStrength + 10*(dist_calcium_carbon - 4.3)*(dist_calcium_carbon - 4.3);
			                                angle = angleComp.point3angle(gridPoint[0], gridPoint[1], gridPoint[2],oxygenX[compsubInt[ii]],oxygenY[compsubInt[ii]],oxygenZ[compsubInt[ii]],
			                                                    carbonX[compsubInt[ii]],carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]]);
			                                if(angle < 140){
			                                    angle_Ca_O_C = angle_Ca_O_C +(140 - angle)*(140 - angle)/400;
			                                 }                                
			                            }
			                            
			                            /***** cases for atom from ASP or GLU   ******/
			                            if (ii < lengthCompsub-1) {
			                                if ((resiName[compsubInt[ii]].equals("ASP") == true && resiName[compsubInt[ii+1]].equals("ASP") == true &&
			                                    atomName[compsubInt[ii]].equals("O") == false && carbonX[compsubInt[ii]] == carbonX[compsubInt[ii+1]])
			                                    || (resiName[compsubInt[ii]].equals("GLU") == true && resiName[compsubInt[ii+1]].equals("GLU") == true &&
			                                    atomName[compsubInt[ii]].equals("O") == false && carbonX[compsubInt[ii]] == carbonX[compsubInt[ii+1]])){
			                                
			                                    dist_calcium_carbon = Dist.distThreeD(gridPoint[0], gridPoint[1], gridPoint[2], 
			                                                              carbonX[compsubInt[ii]],carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]]);                               
			                                    dist_calcium_oxy1 = dist_calcium_oxy;
			                                    dist_calcium_oxy2 = Dist.distThreeD(gridPoint[0], gridPoint[1], gridPoint[2], 
			                                                                oxygenX[compsubInt[ii+1]],oxygenY[compsubInt[ii+1]],oxygenZ[compsubInt[ii+1]]);
			                                    angle1 = angleComp.point3angle(gridPoint[0], gridPoint[1], gridPoint[2],oxygenX[compsubInt[ii]],oxygenY[compsubInt[ii]],oxygenZ[compsubInt[ii]],
			                                                        carbonX[compsubInt[ii]],carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]]);
			                                    angle2 = angleComp.point3angle(gridPoint[0], gridPoint[1], gridPoint[2],oxygenX[compsubInt[ii+1]], oxygenY[compsubInt[ii+1]], 
			                                                         oxygenZ[compsubInt[ii+1]],carbonX[compsubInt[ii+1]],carbonY[compsubInt[ii+1]],carbonZ[compsubInt[ii+1]]);                                                        
			                                    amount1 = dist_calcium_oxy1*angle1/(dist_calcium_oxy2*angle2);
			                                    amount2 = dist_calcium_oxy2*angle2/(dist_calcium_oxy1*angle1);
			                                    dihedralAngle = angleComp.dihedral_angles(carbonX[compsubInt[ii]], carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]],
			                                                    oxygenX[compsubInt[ii]], oxygenY[compsubInt[ii]],oxygenZ[compsubInt[ii]],
			                                                    oxygenX[compsubInt[ii+1]],oxygenY[compsubInt[ii+1]],oxygenZ[compsubInt[ii+1]],
			                                                    gridPoint[0], gridPoint[1], gridPoint[2]);
			                                    if (amount1 < 1.3 && amount1 > 0.7 && amount2 < 1.3 && amount2 > 0.7) { 
			                                        bondStrength = bondStrength + 5*(dist_calcium_oxy1 - 3.6)*(dist_calcium_oxy1 - 3.6);
			                                        carbonBondStrength = carbonBondStrength + 5*(dist_calcium_carbon - 4.4)*(dist_calcium_carbon - 4.4);
			                                        angle_Ca_O_C = angle_Ca_O_C + (93 - angle1)*(93 - angle1)/300;
			                                        if (dihedralAngle < 165){
			                                            angle_dihedral = angle_dihedral + (dihedralAngle-165)*(dihedralAngle-165)/100;
			                                        }   
			                                    }
			                                    else {
			                                        indication_conf = 1;
			                                        carbonBondStrength = carbonBondStrength + 5*(dist_calcium_carbon - 3.5)*(dist_calcium_carbon - 3.5);                             
			                                        if (amount1 < 0.7 || amount2 > 1.3){  // L-atom
			                                            bondStrength = bondStrength + 5*(dist_calcium_oxy1 - 3.8)*(dist_calcium_oxy1 - 3.8);
			                                            angle_Ca_O_C = angle_Ca_O_C + (55 - angle1)*(55 - angle1)/600;
			                                        }
			                                        else{  // S-atom                 
			                                            bondStrength = bondStrength + 5*(dist_calcium_oxy1 - 3.6)*(dist_calcium_oxy1 - 3.6);
			                                            if(angle1 < 140){ 
			                                                angle_Ca_O_C = angle_Ca_O_C + (140 - angle1)*(140 - angle1)/600;
			                                            }
			                                        }
			                                        if (dihedralAngle < 165){
			                                            angle_dihedral = angle_dihedral + (dihedralAngle-165)*(dihedralAngle-165)/100;
			                                        }   
			                                    }

			                       
			                                }  
			                            }   // if < BLength
			                            if (ii >  0) {
			                                if ((resiName[compsubInt[ii]].equals("ASP") == true && resiName[compsubInt[ii-1]].equals("ASP") == true &&
			                                    atomName[compsubInt[ii]].equals("O") == false && carbonX[compsubInt[ii]] == carbonX[compsubInt[ii-1]])
			                                    || (resiName[compsubInt[ii]].equals("GLU") == true && resiName[compsubInt[ii-1]].equals("GLU") == true &&
			                                    atomName[compsubInt[ii]].equals("O") == false && carbonX[compsubInt[ii]] == carbonX[compsubInt[ii-1]])) {
			                                
			                                    dist_calcium_carbon = Dist.distThreeD(gridPoint[0], gridPoint[1], gridPoint[2], 
			                                                          carbonX[compsubInt[ii]], carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]]);                               
			                                    dist_calcium_oxy1 = dist_calcium_oxy;
			                                    dist_calcium_oxy2 = Dist.distThreeD(gridPoint[0], gridPoint[1], gridPoint[2], 
			                                                        oxygenX[compsubInt[ii-1]], oxygenY[compsubInt[ii-1]],oxygenZ[compsubInt[ii-1]]);
			                                    angle1 = angleComp.point3angle(gridPoint[0], gridPoint[1], gridPoint[2],oxygenX[compsubInt[ii]],oxygenY[compsubInt[ii]],oxygenZ[compsubInt[ii]],
			                                                        carbonX[compsubInt[ii]], carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]]);
			                                    angle2 = angleComp.point3angle(gridPoint[0], gridPoint[1], gridPoint[2],oxygenX[compsubInt[ii-1]],oxygenY[compsubInt[ii-1]], 
			                                                        oxygenZ[compsubInt[ii-1]],carbonX[compsubInt[ii-1]],carbonY[compsubInt[ii-1]],carbonZ[compsubInt[ii-1]]); 
			                                    amount1 = dist_calcium_oxy1*angle1/(dist_calcium_oxy2*angle2);
			                                    amount2 = dist_calcium_oxy2*angle2/(dist_calcium_oxy1*angle1);
			                                    dihedralAngle = angleComp.dihedral_angles(carbonX[compsubInt[ii]], carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]],
			                                                    oxygenX[compsubInt[ii]],oxygenY[compsubInt[ii]],oxygenZ[compsubInt[ii]],
			                                                    oxygenX[compsubInt[ii-1]],oxygenY[compsubInt[ii-1]],oxygenZ[compsubInt[ii-1]],
			                                                    gridPoint[0], gridPoint[1], gridPoint[2]);
			                                    if (amount1 < 1.3 && amount1 > 0.7 && amount2 < 1.3 && amount2 > 0.7) { 
			                                        bondStrength = bondStrength + 5*(dist_calcium_oxy1 - 3.5)*(dist_calcium_oxy1 - 3.5);
			                                        carbonBondStrength = carbonBondStrength + 5*(dist_calcium_carbon - 4.4)*(dist_calcium_carbon - 4.4);
			                                        angle_Ca_O_C = angle_Ca_O_C + (93 - angle1)*(93 - angle1)/300;
			                                        if (dihedralAngle < 165){
			                                            angle_dihedral = angle_dihedral + (dihedralAngle-165)*(dihedralAngle-165)/100;
			                                        }    
			                                    }
			                                    else {
			                                        indication_conf = 1;
			                                        carbonBondStrength = carbonBondStrength + 5*(dist_calcium_carbon - 3.5)*(dist_calcium_carbon - 3.5);                             
			                                        if (amount1 < 0.7 || amount2 > 1.3){  // L-atom
			                                            bondStrength = bondStrength + 5*(dist_calcium_oxy1 - 3.8)*(dist_calcium_oxy1 - 3.8);
			                                            angle_Ca_O_C = angle_Ca_O_C + 5*(55 - angle1)*(55 - angle1)/600;
			                                        }
			                                        else{  // S-atom                 
			                                            bondStrength = bondStrength + 5*(dist_calcium_oxy1 - 3.4)*(dist_calcium_oxy1 - 3.4);
			                                            if(angle1 < 140){ 
			                                                angle_Ca_O_C = angle_Ca_O_C + (140 - angle1)*(140 - angle1)/600;
			                                            }
			                                        }
			                                        if (dihedralAngle < 155){
			                                            angle_dihedral = angle_dihedral + (dihedralAngle-155)*(dihedralAngle-155)/400;
			                                        }    
			                                    }

			                      
			                                }  
			                            }   // if > 1
			                            
			                            // case of monodentate  
			                            if (ii > 0 && ii < lengthCompsub - 1){
			                                if ((resiName[compsubInt[ii]].equals("ASP") == true || resiName[compsubInt[ii]].equals("GLU") == true)
			                                     && atomName[compsubInt[ii]].equals("O") == false && carbonX[compsubInt[ii]] != carbonX[compsubInt[ii-1]]
			                                     && carbonX[compsubInt[ii]] != carbonX[compsubInt[ii+1]]){
			         
			                                     dist_calcium_carbon = Dist.distThreeD(gridPoint[0], gridPoint[1], gridPoint[2], 
			                                                           carbonX[compsubInt[ii]],carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]]);
			                                     carbonBondStrength = carbonBondStrength + 10*(dist_calcium_carbon - 4.3)*(dist_calcium_carbon - 4.3);
			                                     bondStrength = bondStrength + 10*(dist_calcium_oxy - 3.4)*(dist_calcium_oxy - 3.4);
			                                     angle = angleComp.point3angle(gridPoint[0], gridPoint[1], gridPoint[2],oxygenX[compsubInt[ii]],oxygenY[compsubInt[ii]], 
			                                                        oxygenZ[compsubInt[ii]],carbonX[compsubInt[ii]],carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]]);
			                                     if (angle < 140){
			                                        angle_Ca_O_C = angle_Ca_O_C +(140 - angle)*(140 - angle)/400;
			                                     }
			                                }
			                            }    
			                            
			                            if (ii == 0){
			                                if ((resiName[compsubInt[ii]].equals("ASP") == true || resiName[compsubInt[ii]].equals("GLU") == true)
			                                     && atomName[compsubInt[ii]].equals("O") == false && carbonX[compsubInt[ii]] != carbonX[compsubInt[ii+1]]) {
			                                     
			                                     dist_calcium_carbon = Dist.distThreeD(gridPoint[0], gridPoint[1], gridPoint[2], 
			                                                          carbonX[compsubInt[ii]], carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]]);
			                                     carbonBondStrength = carbonBondStrength + 10*(dist_calcium_carbon - 4.3)*(dist_calcium_carbon - 4.3);
			                                     bondStrength = bondStrength + 10*(dist_calcium_oxy - 3.4)*(dist_calcium_oxy - 3.4);
			                                     angle = angleComp.point3angle(gridPoint[0], gridPoint[1], gridPoint[2],oxygenX[compsubInt[ii]],oxygenY[compsubInt[ii]], 
			                                                        oxygenZ[compsubInt[ii]],carbonX[compsubInt[ii]],carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]]);
			                                     if(angle < 140){
			                                        angle_Ca_O_C = angle_Ca_O_C +(140 - angle)*(140 - angle)/400;
			                                     }
			                                }
			                            }
			                            if(ii == lengthCompsub-1){
			                  
			                                if ((resiName[compsubInt[ii]].equals("ASP") == true || resiName[compsubInt[ii]].equals("GLU") == true)
			                                     && atomName[compsubInt[ii]].equals("O") == false && carbonX[compsubInt[ii]] != carbonX[compsubInt[ii-1]]){
			      
			                                     dist_calcium_carbon = Dist.distThreeD(gridPoint[0], gridPoint[1], gridPoint[2], 
			                                                           carbonX[compsubInt[ii]], carbonY[compsubInt[ii]], carbonZ[compsubInt[ii]]);
			                                     //
			                                     carbonBondStrength = carbonBondStrength + 10*(dist_calcium_carbon - 4.3)*(dist_calcium_carbon - 4.3);
			                                     bondStrength = bondStrength + 10*(dist_calcium_oxy - 3.4)*(dist_calcium_oxy - 3.4);
			                                     angle = angleComp.point3angle(gridPoint[0], gridPoint[1], gridPoint[2],oxygenX[compsubInt[ii]],oxygenY[compsubInt[ii]], 
			                                                        oxygenZ[compsubInt[ii]], carbonX[compsubInt[ii]], carbonY[compsubInt[ii]],carbonZ[compsubInt[ii]]);
			                                     if(angle < 140){
			                                         angle_Ca_O_C = angle_Ca_O_C +(140 - angle)*(140 - angle)/400;
			                                     }
			                                }
			                            }
			                        }   // from 1 to BLength
			                        /******  calculate total penalty  *****************/
			               
			                        if (indication_conf == 1) {
			                        	//
			                            dist_deviation      = (count-1)*5*(secondMax -  minValue)*(secondMax -  minValue);
			                            dist_sum            = (count-1)*10*Math.pow((dist_sum-maxValue)/(count-1) - 2.0,2);
			                            PG                  = (bondStrength + angle_Ca_O_C + angle_dihedral + carbonBondStrength  + dist_sum + dist_deviation)/(count-1); 
			                        }
			                        else {
			                            dist_deviation      = count*5*(maxValue - minValue)*(maxValue - minValue);
			                            dist_sum            = count*10*Math.pow(dist_sum/count - 2.0, 2);
			                            PG                  = (bondStrength + angle_Ca_O_C + angle_dihedral + carbonBondStrength + dist_sum + dist_deviation)/count; 
			                        }
			                        if (PG < min_G) {
			                            min_G = PG;
			                            xStar = gridPoint[0];
			                            yStar = gridPoint[1];
			                            zStar = gridPoint[2];
			                        }
	                            } // if flag
			               }  // kkk               
			          }  // jjj
			     }  // iii
			     
			     
			     /******* get return value  *******************************/
			     if (xStar != 1000 || yStar != 1000 || zStar != 1000) {
			            fX[0] = xStar;
			            fX[1] = yStar;
			            fX[2] = zStar;
			            exitflag = 1;
			            fX[3] = exitflag;
			      }
			 
			      for (ii = 0; ii < lengthCompsub; ii++) {  
			            fval = fval + Dist.distThreeD(fX[0], fX[1], fX[2],oxygenX[compsubInt[ii]],
			                        oxygenY[compsubInt[ii]],oxygenZ[compsubInt[ii]]);
			      }
			      fX[4] = fval/count;

			     /**********   return *****************************/
				 
			     outputArray[0] = fX[0]; 
			     outputArray[1] = fX[1];
			     outputArray[2] = fX[2];
			     outputArray[3] = fX[3];
			     outputArray[4] = fX[4];
			    
				 return outputArray; 
			}   // positionFun
}

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

public class NMRSeperate {
	
	public static List nMRSeperate(String arg){
		
        List fileList = new ArrayList();
        List fileList1 = new ArrayList();
        List pdbList=new ArrayList();
        List NMRresult=new ArrayList();
        String line= "";
        String nextline="";
        int modelCount=0, linecount=0; 
	    String currDirectory = System.getProperty("user.dir");
        
        fileList1 = FileOperation.getEntriesAsList(arg); // a list of PDBIDs


        for (int j = 0; j< fileList1.size(); j++){   // iteratively process PDB files

            String PDBID = ((String)fileList1.get(j)).substring(0,4);
            String fileName = currDirectory+"/inputdata/"+ PDBID +".pdb";
            
            fileList = FileOperation.getEntriesAsList(fileName);
            
            for(int i = 0; i < fileList.size(); i++){
            	
            	pdbList.clear();
            	
            	if (i+1<fileList.size()){            		
            	
	            	linecount=0;
	            	line = (String)fileList.get(i);
	            	nextline=(String)fileList.get(i+1);
	            	if (line.startsWith("MODEL")&& (nextline.startsWith("ATOM")||nextline.startsWith("HETATM"))) {
	            		
	            		pdbList.add(line);
	            		modelCount++;
	            		
	                    while (nextline.startsWith("ENDMDL")==false){
	                    	linecount++;
	                    	nextline=(String)fileList.get(i+linecount);
	                    	pdbList.add(nextline);
	                    }
	                    
	                    
	                    String modelName=modelCount+"";
	                    if (modelName.length()==1){
	                    	modelName="MO0"+modelCount;
	                    }
	                    
	                    if (modelName.length()==2){
	                    	modelName="MO"+modelCount;
	                    }
	                    
	                    NMRresult.add(modelName);
	                    
	                    FileOperation.saveResults(pdbList, currDirectory+ "/inputdata/"+ modelName+ ".pdb", "w");
	                    FileOperation.saveResults(modelName, currDirectory+ "/inputdata/"+ PDBID+"list.txt", "a");
	                    
	                    i=i+linecount;
	                }
            	}
            }
            
            int bb=1;
            
        }// end for
        
        return NMRresult;
	}
	
	public static void nMRmergePDB(List PDBIDList, String PDBname){
		
		int maxModel=0;
		int maxM=0;
		List fileList = new ArrayList();
		List residueList1=new ArrayList();
		List residueList2=new ArrayList();
		List residueList3=new ArrayList();
		List residueList4=new ArrayList();
		List residueList5=new ArrayList();
	    String currDirectory = System.getProperty("user.dir");	    
	    int totalPocket=0;
	    int totalResi=0;
	    
		for (int k=0;k<PDBIDList.size();k++){
			
			int resultCount=0;
			//FileOperation.saveResults((String)FileOperation.getEntriesAsList(currDirectory+"/inputdata/"+ PDBIDList.get(k) +".pdb").get(0), currDirectory+ "/predictionResults/"+ PDBname+ "_site.pdb", "a");
			//fileList=FileOperation.getEntriesAsList(currDirectory+"/predictionResults/"+ PDBIDList.get(k) +"_site.pdb");
			//FileOperation.saveResults(fileList, currDirectory+ "/predictionResults/"+ PDBname+ "_site.pdb", "a");		
			FileOperation.saveResults("ENDMDL                                                                          ", currDirectory+ "/predictionResults/"+ PDBname+ "_site.pdb", "a");
			residueList1=FileOperation.getEntriesAsList(currDirectory+"/predictionResults/"+ PDBIDList.get(k) +"_site.txt");
			residueList2.addAll(residueList1);
			
            //count the max model
            Iterator it=residueList1.iterator();
            while(it.hasNext()){
                    String elem=(String)it.next();
                    if (elem.length()!=0 && elem.substring(0,1).equals(">")){
                    	resultCount++;
                    	totalPocket++;
                    }
                    if (elem.length()!=0){
                    	totalResi++;
                    }
            }
			
            if (resultCount>maxModel){
            	maxModel=resultCount;
            	maxM=k;
            }						
		}
		
		//first model and max Model
		if (maxModel>0){
			residueList5.addAll(FileOperation.getEntriesAsList(currDirectory+"/predictionResults/"+ PDBIDList.get(0) +"_site.txt"));
			residueList5.addAll(FileOperation.getEntriesAsList(currDirectory+"/predictionResults/"+ PDBIDList.get(maxM) +"_site.txt"));
		}
		else {
			residueList5.addAll(FileOperation.getEntriesAsList(currDirectory+"/predictionResults/"+ PDBIDList.get(0) +"_site.txt"));
		}
		//count for the duplicated residues.
		Iterator iter = residueList2.iterator();  		
		while(iter.hasNext()){
			String element = (String)iter.next();
			if (element.length()!=0){
				residueList3.add(element.substring(16, 25));
			}			
		}
		
		Collections.sort(residueList3);

		//remove duplicate entries.
		HashSet hashSet = new HashSet(residueList3);
		residueList4 = new ArrayList(hashSet) ;
		Collections.sort(residueList4);
        int count=0;  	
		for (int i=0;i<residueList4.size();i++){
			String element = (String)residueList4.get(i);
                        for (int j=0;j<residueList3.size();j++){
                                String element1=(String)residueList3.get(j);
                                if (element.equals(element1)){
                                        count++;
                                }
                        }

			String element2= element+"  "+ Integer.toString(count)+" "+Double.toString((double)Math.round(100*count/totalPocket)/100)+ " "+Double.toString((double)Math.round(100*count/totalResi)/100);
                        residueList4.set(i,element2);
                        count=0;

		}		
		
		//FileOperation.saveResults(residueList4, currDirectory+ "/predictionResults/"+ PDBname+ "_resi.txt", "w");
		//FileOperation.saveResults(residueList5, currDirectory+ "/predictionResults/"+ PDBname+ "_core.txt", "w");
		
	}

}

public class AngleCalculation {
	/**********
	Return: the angle among a, b and c.
	Input: the coodinates (x,y,z) of a, b and c in order.  
	************/
	public static double point3angle(double a1, double a2, double a3, double b1, double b2, double b3, double c1, double c2, double c3) {
		double ag, ab1, ab2, ab3, cb1, cb2, cb3;
	    ab1=a1-b1;
	    ab2=a2-b2;
	    ab3=a3-b3;
	    cb1=c1-b1;
	    cb2=c2-b2;
	    cb3=c3-b3;
	    ag = Math.acos((ab1*cb1+ab2*cb2+ab3*cb3)/(Math.sqrt(ab1*ab1+ab2*ab2+ab3*ab3)*Math.sqrt(cb1*cb1+cb2*cb2+cb3*cb3)))*180/3.1416;
		return ag;
	}
	 
	public static double dihedral_angles(double a1, double a2, double a3, double b1, double b2, double b3, double c1, double c2, double c3,
            double d1, double d2, double d3 ) {
	/**********
	Return: the dihedral angle between plane formed by x1,x2,x3, and plane formed by x2,x3,x4.
	Input: the coodinates (x,y,z) of a, b and c in order.  
	************/	  	  
	      double d12, d13, d14, d23, d24, d34, P, Q, angle; 
	      Dist distance = new Dist();
	      d12 = distance.distThreeD(a1,a2,a3,b1,b2,b3);
	      d13 = distance.distThreeD(a1,a2,a3,c1,c2,c3); 
	      d14 = distance.distThreeD(a1,a2,a3,d1,d2,d3); 
	      d23 = distance.distThreeD(b1,b2,b3,c1,c2,c3); 
	      d24 = distance.distThreeD(b1,b2,b3,d1,d2,d3); 
	      d34 = distance.distThreeD(c1,c2,c3,d1,d2,d3);
	      
	      P = d12*d12 * ( d23*d23 + d34*d34 - d24*d24) + d23*d23 * (-(d23*d23) + d34*d34 + d24*d24) + d13*d13 * (d23*d23 - d34*d34 + d24*d24) - 2 * d23*d23 * d14*d14;
	      Q = (d12 + d23 + d13) * ( d12 + d23 - d13) * (d12 - d23 + d13) * (-d12 + d23 + d13 ) * (d23 + d34 + d24) * ( d23 + d34 - d24 ) * (d23 - d34 + d24) * (-d23 + d34 + d24);
	      angle = Math.acos(P/Math.sqrt(Q))*180/3.1416;   
	      return angle;
	}
}

public class CombinationGenerator {

    int n, m;
    //exhaust m and take n possibility.
    int[] pre;//previous combination.

    public CombinationGenerator(int n, int m) {
     this.n = n;
     this.m = m;
    }

    /**
     * generate one combination
     * if return null,all combination are generated.
     */
    public int[] next() {
     if (pre == null) {//taken the first combination.
      pre = new int[n];
      for (int i = 0; i < pre.length; i++) {
       pre[i] = i;
      }
      int[] ret = new int[n];
      System.arraycopy(pre, 0, ret, 0, n);
      return ret;
     }
     int ni = n - 1, maxNi = m - 1;
     while (pre[ni] + 1 > maxNi) {//from right to left find all incremental space
      ni--;
      maxNi--;
      if (ni < 0)
       return null;//nothing find means the combination is exhausted.
     }
     pre[ni]++;
     while (++ni < n) {
      pre[ni] = pre[ni - 1] + 1;
     }
     int[] ret = new int[n];
     System.arraycopy(pre, 0, ret, 0, n);
     return ret;
    }
    
    public int combinations() {
          
        //choose k from n.
        int t= 1; int k=m;
        for (int i= Math.min(n, k-n), l= 1; i >= 1; i--, k--, l++) {
            t*= k; t/= l;
        }
          
        return t;
    }

}

public class Dist{
    
    public static double distThreeD(double x1, double y1, double z1, double x2, double y2, double z2){
        
        double distance = Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2)+(z1-z2)*(z1-z2)); 
 
        return distance;  


    }

}

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

public class PutativeTrue {

	private static int predictionCount=0;
	private static int correctPredictionCount=0;
	private static int correctPredictionPocketCount=0;
	private static int totalPocketCount=0;
	private static List caCoordinate=new ArrayList();
	private static List caCoordinate1=new ArrayList();
	
	public static void doPrint(List ocFile, List compsub, String PDBID, double x[]){
			
		/*  Filename: putative_true
		%
		% Abstract: 
		    % Print out predicted calcium location and associated ligand group
		% version 0.3
		% Kun Zhao
		% Version: 0.2
		% Author: Xue Wang
		% Date: 1/25/2008
		% Replace version 0.1
		% Author: Xue Wang 
		% Date: Jun., 2007
		*/
			String fileName = "";
			String fileNamePdb="";
			String currDirectory = System.getProperty("user.dir");
			++predictionCount;

			int jj = 0;
			int lengthCompsub = compsub.size();
			//System.out.println(compsub);
			//System.out.println("=========================="+compsub.size());
	        List resultList =  new ArrayList();
	        List resultListPdb =  new ArrayList();
	        List caList = new ArrayList();
	        caList = FileOperation.getEntriesAsList(currDirectory+"/outputdata/" + PDBID +"_ca.dat");
			String resultLine= "";
			DecimalFormat doubleFormat = new DecimalFormat("      ###0.000;      -###0.000");
			DecimalFormat intFormat = new DecimalFormat("      ######");
			String serialString = "", resiNumString = "";
			int serialNum=0, resiNum=0;
			String ocLine = "", atomName="", resiName="", caLine = "";
			String calciumX, calciumY, calciumZ;
			fileName = currDirectory+"/predictionResults/" + PDBID +"_site.txt";
			fileNamePdb = currDirectory+"/predictionResults/" + PDBID +"_site.pdb";
			double caX = 0.0, caY = 0.0, caZ = 0.0;
			double minDist = 10000.0, caODist = 0.0;
			String closeCa = "";
			
			for (jj = 0; jj < caList.size(); jj++) {

				 caLine = (String)caList.get(jj);
		    	 caX = Double.parseDouble(caLine.split(" +")[5]);
		    	 caY = Double.parseDouble(caLine.split(" +")[6]);
		    	 caZ = Double.parseDouble(caLine.split(" +")[7]);		    			 		    	 
		    	 
		    	 if(Dist.distThreeD(caX, caY, caZ, x[0], x[1], x[2]) < minDist){
		    		 minDist = Dist.distThreeD(caX, caY, caZ, x[0], x[1], x[2]);
		    		 
		    		 //count for selectivity curve
		    		 if (minDist<3.5){
		    			 correctPredictionCount++;
		    		 }
		    		 
		    		 closeCa = caLine.split(" +")[1];
		    	 }
		    	 
    			 if (caCoordinate1.contains(caLine)==false && Dist.distThreeD(caX, caY, caZ, x[0], x[1], x[2])<3.5){
    				 caCoordinate1.add(caLine);
    				 correctPredictionPocketCount++;
    			 }

		    	 
		    	 if (caCoordinate.contains(caLine)==false){
    				 caCoordinate.add(caLine);
    				 totalPocketCount++;
    			 }
			}
			
			for (jj = 0; jj < lengthCompsub; jj++) {
				ocLine = (String)ocFile.get(Integer.parseInt((String)compsub.get(jj)));	
		    	caODist = caODist + Dist.distThreeD(x[0], x[1], x[2],
		    			   Double.parseDouble(ocLine.split(" +")[5]),
		    			   Double.parseDouble(ocLine.split(" +")[6]),
		    			   Double.parseDouble(ocLine.split(" +")[7]));
			} caODist = caODist/lengthCompsub;
			
			
			
		    for (jj = 0; jj < lengthCompsub; jj++) {
		    	 ocLine = (String)ocFile.get(Integer.parseInt((String)compsub.get(jj)));
		    	 serialNum = Integer.parseInt(ocLine.split(" +")[1]);
		    	 resiNum = Integer.parseInt(ocLine.split(" +")[4]);
		    	 atomName = ocLine.split(" +")[2];
		    	 resiName = ocLine.split(" +")[3];
		    	 serialString = intFormat.format(serialNum);
		    	 resiNumString = intFormat.format(resiNum);
		    	 calciumX = doubleFormat.format(x[0]);
		    	 calciumY = doubleFormat.format(x[1]);
		    	 calciumZ = doubleFormat.format(x[2]);
		         DecimalFormat df = new DecimalFormat("####.##    "); 
		    	 if(jj == 0){
	                 resultLine = (">"+Integer.toString(predictionCount)+"    ").substring(0,4)
	                          + serialString.substring(serialString.length() - 6, serialString.length())
	                          + " "
	                          + (atomName + "     ").substring(0, 4)
	                          + " "
	                          + resiNumString.substring(resiNumString.length() - 4,resiNumString.length())
	                          + " "
	                          + (resiName + "    ").substring(0, 4)
	                          + calciumX.substring(calciumX.length() - 8, calciumX.length())
	                          + calciumY.substring(calciumY.length() - 8, calciumY.length())
	                          + calciumZ.substring(calciumZ.length() - 8, calciumZ.length())
	                          + " "
	                          + (closeCa + "     ").substring(0,5)
	                          + " "
	                          + df.format(minDist)
	                          + " "
	                          + df.format(caODist);
	                 resultList.add(resultLine);

	             }
	             else{
	                  resultLine = "    "
	                           +  serialString.substring(serialString.length() - 6, serialString.length())
	                           + " "
	                           + (atomName + "     ").substring(0, 4)
	                           + " "
	                           +resiNumString.substring(resiNumString.length() - 4,resiNumString.length())
	                           + " "
	                           + (resiName + "    ").substring(0, 4)
	                           + calciumX.substring(calciumX.length() - 8, calciumX.length())
	                           + calciumY.substring(calciumY.length() - 8, calciumY.length())
	                           + calciumZ.substring(calciumZ.length() - 8, calciumZ.length())
	                           + " "
	                           + (closeCa + "     ").substring(0,5)
	                           + " "
	                           + df.format(minDist)
	                           + " "
	                           + df.format(caODist);
	                  resultList.add(resultLine);
	             }		    	 
			}
		    
		    resultList.add("\n");
	        FileOperation.saveResults(resultList, fileName, "a");
			//FileOperation.saveResults(resultList, currDirectory+"/predictionResults/" + "alre_site.txt", "a");
	        String resultLine1="HETATM      CA    CA"+"           " + resultLine.substring(26, 50);
	        FileOperation.saveResults(resultLine1, fileNamePdb, "a");
		}
	
	public static int getCorrectPrediction(){
		return correctPredictionCount;
	}
	
	public static int getPredictionCount(){
		return predictionCount;
	}

	public static int getCorrectPredictionPocketCount(){
		return correctPredictionPocketCount;
	}	

	public static int getTotalPocketCount(){
		return totalPocketCount;
	}
}

public class SideChainCenterOfMass {
	
	public static String sideChainCalculation(List sideChain){
		
		double oMass=16;
		double nMass=14;
		double cMass=12;
		double sMass=32;
		double xNumerator=0, xDenominator=0;
		double yNumerator=0, yDenominator=0;
		double zNumerator=0, zDenominator=0;
		double cCount=0, nCount=0,oCount=0,sCount=0; 
		String xResult, yResult, zResult, centerOfMass;
		double[] xCoordinate= new double[sideChain.size()];
		double[] yCoordinate= new double[sideChain.size()];
		double[] zCoordinate= new double[sideChain.size()];
		
		for (int l = 0; l < sideChain.size(); l++){
			xCoordinate[l]=Double.parseDouble(((String)sideChain.get(l)).substring(30,38));
			yCoordinate[l]=Double.parseDouble(((String)sideChain.get(l)).substring(39,46));
			zCoordinate[l]=Double.parseDouble(((String)sideChain.get(l)).substring(47,54));
			String type=((String)sideChain.get(l)).substring(13,14);

			if (type.equals("C")){
				xNumerator=xNumerator + xCoordinate[l]*cMass;
				yNumerator=yNumerator + yCoordinate[l]*cMass;
				zNumerator=zNumerator + zCoordinate[l]*cMass;
				cCount++;
			}
			if (type.equals("O")){
				xNumerator=xNumerator + xCoordinate[l]*oMass;
				yNumerator=yNumerator + yCoordinate[l]*oMass;
				zNumerator=zNumerator + zCoordinate[l]*oMass;
				oCount++;
			}
			if (type.equals("N")){
				xNumerator=xNumerator + xCoordinate[l]*nMass;
				yNumerator=yNumerator + yCoordinate[l]*nMass;
				zNumerator=zNumerator + zCoordinate[l]*nMass;
				nCount++;
			}
			if (type.equals("S")){
				xNumerator=xNumerator + xCoordinate[l]*sMass;
				yNumerator=yNumerator + yCoordinate[l]*sMass;
				zNumerator=zNumerator + zCoordinate[l]*sMass;
				sCount++;
			}						
		}
		
		double xCoord=(double)((int)Math.round((xNumerator/(cCount*cMass+oCount*oMass+nCount*nMass+sCount*sMass))*1000)/1000.00);
		double yCoord=(double)((int)Math.round((yNumerator/(cCount*cMass+oCount*oMass+nCount*nMass+sCount*sMass))*1000)/1000.00);
		double zCoord=(double)((int)Math.round((zNumerator/(cCount*cMass+oCount*oMass+nCount*nMass+sCount*sMass))*1000)/1000.00);
		DecimalFormat df = new DecimalFormat("###0.000");
		xResult=df.format(xCoord)+"";
		yResult=df.format(yCoord)+"";
		zResult=df.format(zCoord)+"";
		
		StringBuffer R=new StringBuffer((String)sideChain.get(0));
		R = R.replace(30,70,"                                        ");
		R = R.replace(12,17," R   ");
		R = R.replace(77,78,"R");
		R = R.replace(38-xResult.length(),38,xResult);
		R = R.replace(46-yResult.length(),46,yResult);
		R = R.replace(54-zResult.length(),54,zResult);
		centerOfMass=R.toString();					
		return centerOfMass;
	}
	
}
