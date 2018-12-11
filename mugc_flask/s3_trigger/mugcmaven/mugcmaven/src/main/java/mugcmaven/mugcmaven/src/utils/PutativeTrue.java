package src.utils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

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
