package mugcmaven.mugcmaven.src.utils; 

import java.util.*;


public class Dist{
    
    public static double distThreeD(double x1, double y1, double z1, double x2, double y2, double z2){
        
        double distance = Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2)+(z1-z2)*(z1-z2)); 
 
        return distance;  


    }

}