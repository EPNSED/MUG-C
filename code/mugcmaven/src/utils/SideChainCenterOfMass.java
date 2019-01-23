package mugcmaven.mugcmaven.src.utils;
import java.text.DecimalFormat;
import java.util.List;

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
