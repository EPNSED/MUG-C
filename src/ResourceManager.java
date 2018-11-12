/**
*The resource manager for MUGC
*@author: Melchizedek Mashiku
*@version:1.0
*@date: 9/11/2018 
*Please cite: Zhao, K., Wang, X., Wong, H. C., Wohlhueter, R., Kirberger, M. P., Chen, G. and Yang, J. J. (2012), 
*Predicting Ca2+-binding sites using refined carbon clusters. Proteins, 80: 2666�2679. doi: 10.1002/prot.24149
**/

package src;
import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.Charset;

public class ResourceManager {
	public static void main(String[] args) throws Exception {
        String pdbURL = args[0];
        String pdbID = args[1];
        System.out.println(pdbURL);
        String currDirectory = System.getProperty("user.dir");
	    String targetDirectory = currDirectory+"/inputdata/"+ pdbID+".pdb";
        download(pdbURL, targetDirectory);
        writeList(pdbID, currDirectory);
    }
    /**
    * This method downloads the PDB File to the input directory 
    *
    * @param  url  an absolute URL to the PDB Rest api to download the PDB file
    * @param  targetDir the target location; input folder
    *
    */
    public static void download(String url, String targetDir) throws Exception {
        try (InputStream in = URI.create(url).toURL().openStream()) {
            Files.copy(in, Paths.get(targetDir));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    /**
    * This method writes the PDB ID to the list.text file in the input directory 
    *
    * @param  pdbID_arg  an absolute URL to the PDB Rest api to download the PDB file
    * @param  currDir the current location
    *
    */
    public static void writeList(String pdbID_arg, String currDir){
        String pdbID = pdbID_arg;
        String targetDir = currDir +"/inputdata/"+ "list.txt";
        try {
            FileWriter writer = new FileWriter(targetDir, true);
            writer.write(pdbID);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}