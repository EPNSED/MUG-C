package mugcmaven.mugcmaven.src.filter;

/**
*A filter and position function to calculate the calcium centers in the identified clusters.
*@author: Kun Zhao
*@version:1.0
*@date: 2/11/2014 
**/

import java.util.ArrayList;
import java.util.List;
import mugcmaven.mugcmaven.src.utils.AngleCalculation;
import mugcmaven.mugcmaven.src.utils.Dist;

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
