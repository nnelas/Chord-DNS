import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class Utils {

	public static final int TOTAL_ID_SPACE = 1024;
	public static  File SERVER_DEST;
	public static boolean nodeActive = false;

	static {

		try {
			SERVER_DEST = new File("Server_"+InetAddress.getLocalHost().getHostName()+"_dest");
			
			//deleting all files in respective directory during startup.
			if(SERVER_DEST.exists()){
			
				File[] files = Utils.SERVER_DEST.listFiles();
				
				for(File f : files)
					f.delete();
			}
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}
	/*
	 * returns the hash code for the input File Name.
	 * @param fileName
	 * @return hash Value.
	 */
	public static int getHashCodesForFile(String fileName) {

		int hashVal = 0;

		for (int l = 0; l < fileName.length(); l++)
			hashVal = (31 * hashVal + fileName.charAt(l)) % TOTAL_ID_SPACE;

		return hashVal;
	}

	/*
	 * returns the hash code for the input ip Address.
	 * @param fileName
	 * @return hash Value.
	 */
	public static int getHashCodesForIPAddress(String ipAddress) {

		int hashVal = 0;

		for (int l = 0; l < ipAddress.length(); l++)
			hashVal = (31 * hashVal + ipAddress.charAt(l)) % TOTAL_ID_SPACE;

		return hashVal;
	}

	public static boolean checkifNodeorFileIdentifierWithinLimit(int identifier,
			int lb, int ub) {

		//for normal case.
		if(ub > lb){

			if(identifier >= lb && identifier <= ub)
				return true;
		}
		else if(ub < lb){
			
			int lowerDiff = lb - identifier;
			int upperDiff = ub - identifier;
			
			lowerDiff = lowerDiff <= 0 ? 1 : 0;
			upperDiff = upperDiff <= 0 ? 1 : 0;
			
			if( (lowerDiff ^ upperDiff) == 0)
				return true;
			else
				return false;
		}
		return false;
	}

	public static boolean checkIfDirectionIsClockwise(int fileIdentifier,
			int idSpaceLowerLimit, int idSpaceUpperLimit) {
		
		int successor;
		int prodecessor;
		
		if(fileIdentifier >= idSpaceLowerLimit && fileIdentifier >= idSpaceUpperLimit){
			
			successor = fileIdentifier - idSpaceUpperLimit;
		    prodecessor = (TOTAL_ID_SPACE - fileIdentifier) + idSpaceLowerLimit;
			
			if(successor <= prodecessor)
				return true;
			else
				return false;
			
		}else{
			
			successor = (TOTAL_ID_SPACE - fileIdentifier) +  fileIdentifier;
		    prodecessor = idSpaceLowerLimit - fileIdentifier;
		   
		    if(successor <= prodecessor)
				return true;
			else
				return false;
		}
		
	}
}
