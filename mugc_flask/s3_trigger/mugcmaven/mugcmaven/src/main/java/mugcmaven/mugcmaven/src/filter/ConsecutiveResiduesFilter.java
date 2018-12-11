package src.filter;

import java.util.ArrayList;
import java.util.List;
/**
*A filter to calculate relative position between each residues.
*@author: Kun Zhao
*@version:1.0
*@date: 2/11/2014 
**/
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
