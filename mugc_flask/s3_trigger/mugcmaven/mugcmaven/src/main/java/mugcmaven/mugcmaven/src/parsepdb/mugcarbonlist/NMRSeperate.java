/** An application to save, separate, and manipulate multiple structures, e.g. NMR structures 
 * MD simulation trajectories.  
*/

package mugcmaven.mugcmaven.src.parsepdb.mugcarbonlist;

import java.util.*;
import mugcmaven.mugcmaven.src.utils.FileOperation;
import mugcmaven.mugcmaven.src.utils.ParsePDB;

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
