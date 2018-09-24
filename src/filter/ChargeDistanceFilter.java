package src.filter;

/**
*A filter to calculate charge in the clusters and calculate the distance and angle constrains of the binding pockets.
*@author: Kun Zhao
*@version:1.0
*@date: 2/11/2014 
**/
import java.util.ArrayList;
import java.util.List;
import src.utils.AngleCalculation;
import src.utils.Dist;
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
