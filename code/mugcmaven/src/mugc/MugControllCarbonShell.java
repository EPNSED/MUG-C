/**
*@author: Kun Zhao
*@version:1.0
*@date: 2/11/2014 
**/

package mugcmaven.mugcmaven.src.mugc;
import mugcmaven.mugcmaven.src.utils.*;
import java.util.*;
import java.text.*;
import java.io.*;
import mugcmaven.mugcmaven.src.fileutil.Copy;
import mugcmaven.mugcmaven.src.filter.*;

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
