import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class PeerServer {

	public ServerNode node;
	public static String bootStrapServerIP;
	public static Map<Integer, String> neighbourIPMap;
	public static String nodeIP;

	/**
	 * Constructor.
	 */
	public PeerServer(){

		neighbourIPMap = new HashMap<Integer, String>();
		this.node = new ServerNode();
	}
	/**
	 * Main method.
	 * @param args
	 */
	public static void main(String[] args) throws FileNotFoundException {

		PeerServer btstrpServer = new PeerServer();

		//starting request handler in each server.
		Thread handler = new Thread( new RequestHandler(btstrpServer));
		handler.start();

		//starting request handler in each server. It gets objects to update local server and initiates changes in prodecessor node.
		Thread respnseHandler = new Thread( new ResponseHandler(btstrpServer));
		respnseHandler.start();

		bootStrapServerIP = args[0];

		nodeIP = args[1];

		Scanner scan = new Scanner(System.in);

		String command = null;

		while(scan.hasNext()){
			System.out.println("Enter command [join, leave, view, insert fileName, search fileName] :");
			command = scan.nextLine();

			String[] commands = command.split("\\s");

			String cmd = commands[0];
			String fileName = null;

			if ( ( (commands[0].equals("insert") || commands[0].equals("search")) && commands.length == 1 ) )
				continue;

			if(commands.length > 1)
				fileName = commands[1];

			btstrpServer.processCommand(cmd, fileName);
		}
	}

	/*
	 * Method to process the input command.
	 * @param command
	 */
	private void processCommand(String cmd, String fileName) throws FileNotFoundException {

		switch(cmd){

		case "insert": 
			if(Utils.nodeActive){
				File f = new File(fileName);

				if(!f.exists()){
					System.out.println("No such file in the directory ");
					System.out.println("File Insert : Failure");
					System.out.println("------------------------");
					break;
				}
				insertFile(cmd, fileName);
			}
			else
				System.out.println("First you should join the node into the Chord");
			break;

		case "search":
			if(Utils.nodeActive)
				searchFile(cmd, fileName);
			else
				System.out.println("First you should join the node into the Chord");
			break;

		case "view":
			if(Utils.nodeActive)
				view();
			else
				System.out.println("First you should join the node into the Chord");
			break;

		case "join":
			join(cmd);
			Utils.nodeActive = true;
			break;

		case "leave":
			if(Utils.nodeActive)
				leave(cmd);
			else
				System.out.println("First you should join the node into the Chord");
			break;

		default: 
			System.out.println("No such command, Please enter proper command");
			break;		

		}

	}

	/*
	 * Method to remove a node from chord.
	 * @param command
	 */
	private void leave(String command) {
		requestAdjacentServerIPsToLeave(command, nodeIP);
	}

	/*
	 * Method to initiate node leaving process.
	 */
	private void requestAdjacentServerIPsToLeave(String command, String nodeIP) {

		System.out.println("Requesting Successor Server to remove it from Chord");
		Thread nodeController = new Thread(new NodeController(command, nodeIP, node.getSuccessorIPAddress() ));
		nodeController.start();

		try {
			nodeController.join();
			Thread.currentThread().sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("------------Server Shutting down!!!------------");
		System.exit(1);

	}

	/*
	 * Method which handles Node Join.
	 * @param inputString
	 * @return result.
	 */
	private void join(String cmd) {

		System.out.println("--------------------");
		// bootstrap server.
		if(nodeIP.equals(bootStrapServerIP)){


			int identifier = Utils.getHashCodesForIPAddress(nodeIP);

			node.setNodeIdentifier(identifier);
			node.setIdSpaceLowerLimit( (identifier + 1) % Utils.TOTAL_ID_SPACE);
			node.setIdSpaceUpperLimit(identifier % Utils.TOTAL_ID_SPACE);
			node.setSuccessorIPAddress(nodeIP);
			node.setProdecessorIPAddress(nodeIP);

			neighbourIPMap.put(identifier, nodeIP);

			System.out.println("Bootstrap :"+nodeIP +", Node identifier : "+Utils.getHashCodesForIPAddress(nodeIP) +", "
					+ " Successor IPAddress : "+nodeIP+", Prodecessor IPAddress : "+nodeIP);
			System.out.println("--------------------");
			view();
		}

		//not bootstrap server and new node should request bootstrap server to join.
		else{
			requestBootStrapServerIPToJoin(cmd, nodeIP);
		}
	}

	private void requestBootStrapServerIPToJoin(String cmd, String nodeIP) {

		System.out.println("Requesting bootstrap Server to join Chord");
		Thread nodeController = new Thread(new NodeController(cmd, nodeIP));
		nodeController.start();

	}

	/*
	 * Search file 
	 */
	private void searchFile(String command, String fileName) {

		int fileIdentifier = Utils.getHashCodesForFile(fileName);

		System.out.println("Requesting Server to search file in Chord servers with file identifier : "+fileIdentifier);

		//check local id space.
		if(Utils.checkifNodeorFileIdentifierWithinLimit(fileIdentifier, node.getIdSpaceLowerLimit(), 
				node.getIdSpaceUpperLimit())) {

			System.out.println("----------SEARCH STATUS----------");
			System.out.println("Search Trail Path : "+node.getNodeIdentifier());
			System.out.println("Insert File Result : Success");
			System.out.println("--------------------------------");
		}
		else {


			boolean clockwiseRotationFlag = Utils.checkIfDirectionIsClockwise(fileIdentifier, node.getIdSpaceLowerLimit(), 
					node.getIdSpaceUpperLimit());

			String trailPath = node.getNodeIdentifier()+"-";

			//move clockwise.
			if(clockwiseRotationFlag) {

				System.out.println("Forwarding request to node " +node.getSuccessorIPAddress()+"  to search file : "+fileName);
				requestServerToSearchFile(command, nodeIP, node.getSuccessorIPAddress(), fileName,trailPath,clockwiseRotationFlag);		
			}
			//move anti-clockwise.
			else{

				System.out.println("Forwarding request to node " +node.getProdecessorIPAddress()+"  to search file : "+fileName);
				requestServerToSearchFile(command, nodeIP, node.getProdecessorIPAddress(), fileName,trailPath,clockwiseRotationFlag);	

			}
		}

	}

	/*
	 * Method to intiate thread to search file.
	 */
	private void requestServerToSearchFile(String command,
			String currentIPAddress, String adjacentIPAddress, String fileName, String trailPath, boolean clockwiseRotationFlag) {

		trailPath = trailPath + Utils.getHashCodesForIPAddress(adjacentIPAddress)+"-";
		Thread nodeController = new Thread(new NodeController(command, currentIPAddress, adjacentIPAddress,
				fileName,trailPath, clockwiseRotationFlag));
		nodeController.start();
	}

	/*
	 * View descriptions of that node.
	 */
	public void view() {

		System.out.println("---------NODE DETAILS-----------");

		System.out.println("Current Node IP Address : "+nodeIP+ ", Node Identifier : "+ node.getNodeIdentifier() );
		System.out.println("Prodecessor  IP Address : "+node.getProdecessorIPAddress()+ " , Node Identifier : "+Utils.getHashCodesForIPAddress(node.getProdecessorIPAddress()) );
		System.out.println("Successor IP Address : "+node.getSuccessorIPAddress()+ " , Node Identifier : "+Utils.getHashCodesForIPAddress(node.getSuccessorIPAddress()) );
		System.out.println("ID - Space : "+node.getIdSpaceLowerLimit() + " - "+node.getIdSpaceUpperLimit());

		if(Utils.SERVER_DEST.exists()){

			File[] files = Utils.SERVER_DEST.listFiles();

			for(File f : files){
				if(!f.getName().contains(".nfs"))
					System.out.println(f.getName());
			}

		}

		System.out.println("---------------------------------");

	}

	/*
	 * Insert file into particular node. 
	 * insert into this node or initiate request to successor or prodecessor.
	 */
	private void insertFile(String command, String fileName) throws FileNotFoundException {

		int fileIdentifier = Utils.getHashCodesForFile(fileName);
		MyFile myFile = new MyFile();
		String content;
		myFile.setFileName(fileName);

		System.out.println("Requesting Server to insert file into Chord servers with file identifier : "+fileIdentifier);
		//check local id space.
		if(Utils.checkifNodeorFileIdentifierWithinLimit(fileIdentifier, node.getIdSpaceLowerLimit(), 
				node.getIdSpaceUpperLimit())) {

			//creating a directory locally and writing the file in it.
			if (!Utils.SERVER_DEST.exists()) {

				if (Utils.SERVER_DEST.mkdir()) {
					System.out.println("Directory is created!!!");
				} 

				else {
					System.out.println("Directory creation failure!!");
				}
			}

			File file = new File(Utils.SERVER_DEST, fileName);
			System.out.println("New File : "+fileName+ " added to directory : "+
					Utils.SERVER_DEST);

			//reading the file.
			BufferedReader br = null;
			File f = new File(fileName);
			FileReader reader;
			try {
				reader = new FileReader(f);
				br = new BufferedReader(reader);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}


			FileWriter fw;
			BufferedWriter bw;

			//writing to a file in local directory.
			List<String> lines = new ArrayList<String>();
			try {

				fw = new FileWriter(file);
				bw = new BufferedWriter(fw);

				String str;
				while( (str = br.readLine()) != null ) {

					bw.write(str+"\n");
					bw.flush();
					lines.add(str);
				}
				fw.close();
				bw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			content = new Scanner(new File(fileName)).useDelimiter("\\A").next();

			myFile.setContent(content);
			myFile.setLines(lines);
			node.getFiles().add(myFile);
			System.out.println("Insert Trail Path : "+node.getNodeIdentifier());
			System.out.println("Insert File Result : Success");
		} 

		else {

			boolean clockwiseRotationFlag = Utils.checkIfDirectionIsClockwise(fileIdentifier, node.getIdSpaceLowerLimit(), 
					node.getIdSpaceUpperLimit());
			String trailPath = node.getNodeIdentifier()+"-";

			//move clockwise.
			if(clockwiseRotationFlag){
				System.out.println("Forwarding request to adjacent node " +node.getSuccessorIPAddress()+"  to insert file : "+fileName);
				requestToInsertFile(command, nodeIP, node.getSuccessorIPAddress(),fileName,trailPath, clockwiseRotationFlag);	
			}
			//move anti clockwise.
			else {
				System.out.println("Forwarding request to adjacent node " +node.getProdecessorIPAddress()+"  to insert file : "+fileName);
				requestToInsertFile(command, nodeIP, node.getProdecessorIPAddress(),fileName,trailPath,clockwiseRotationFlag);

			}
		}
	}

	private void requestToInsertFile(String command,
			String currentIPAddress, String adjacentIPAddress, String fileName, String trailPath, boolean clockwiseRotationFlag) {

		trailPath = trailPath + Utils.getHashCodesForIPAddress(adjacentIPAddress)+"-";

		Thread nodeController = new Thread(
				new NodeController(command, currentIPAddress, adjacentIPAddress, fileName,trailPath, clockwiseRotationFlag));
		nodeController.start();

	}

	/*
	 * This is the successor node which holds the idSpace where new node can be inserted.
	 * initiated by request handler to perform changes in local node while sending response to request server.
	 */
	public void performAllLocalChangesForNodeJoin(String prodecessorIP, List<MyFile> remfiles) {

		int prodecessorId = Utils.getHashCodesForIPAddress(prodecessorIP);
		node.setIdSpaceLowerLimit( (prodecessorId + 1 ) % Utils.TOTAL_ID_SPACE );
		
		node.setProdecessorIPAddress(prodecessorIP);
		node.setNodeIdentifier(Utils.getHashCodesForIPAddress(PeerServer.nodeIP) );
		node.setFiles(remfiles);

	}


	/*
	 * print the search result for file.
	 */
	public void printSearchInfo(FileResponseObject fileReponseObj) {

		System.out.println("----------SEARCH STATUS----------");
		
		if(fileReponseObj.getResult().equalsIgnoreCase("Success")){

			System.out.println("Search file : "+fileReponseObj.getFile().getFileName());

			System.out.println("Peer which has the File : "+fileReponseObj.getSourceIPAddress()+ 
					", node id : "+Utils.getHashCodesForIPAddress(fileReponseObj.getSourceIPAddress()));

			System.out.println("Trail Path : "+fileReponseObj.getTrailPath());

			System.out.println("DNS Search Result : "+fileReponseObj.getFile().getContent());

			System.out.println("Search Result: "+fileReponseObj.result);
		}else{
			System.out.println("Search file : "+fileReponseObj.getFile().getFileName());
			System.out.println("Reason : No such file present in the Chord servers " );
			System.out.println("Search Result: "+fileReponseObj.result);
		}
		System.out.println("--------------------------------");
	}

	/*
	 * print in insert initiation server and remote server whether the file is inserted or not.
	 */
	public void printFileInsertInfo(FileResponseObject fileReponseObj) {

		System.out.println("---------INSERT STATUS-----------");
		System.out.println("File : "+fileReponseObj.getFile().getFileName()+ " inserted");

		if(fileReponseObj.getTrailPath() != null)
			System.out.println("File insert trail path : "+fileReponseObj.getTrailPath());

		System.out.println("File insertion result : "+fileReponseObj.getResult());
		System.out.println("---------------------------------");
	}


}


