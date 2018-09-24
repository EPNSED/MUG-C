package src.filter;

import java.util.ArrayList;
import java.util.List;

import src.utils.Dist;
import src.utils.FileOperation;
import src.utils.ParsePDB;

/**
 * Filter of center of mass.
 * @author Kun Zhao
 * data:02/14/10
 */

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
				
				System.out.println(l);
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