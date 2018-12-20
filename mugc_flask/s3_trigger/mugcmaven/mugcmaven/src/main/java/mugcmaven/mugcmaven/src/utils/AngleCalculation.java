package mugcmaven.mugcmaven.src.utils;

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
