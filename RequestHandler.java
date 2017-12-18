import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;


public class RequestHandler implements Runnable {

	private InetSocketAddress boundPort = null;
	private final int port = 1234;
	private ServerSocket requestHandlerSocket;
	private PeerServer btstrpServer;

	public RequestHandler(PeerServer btstrpServer) {
		this.btstrpServer = btstrpServer;
	}

	@Override
	public void run() {

		try {

			initServerSocket();

			while(true) {

				Socket connectionSocket = requestHandlerSocket.accept();
				BufferedReader br = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

				String str;

				while( (str =br.readLine() ) != null){

					String[] inputString = str.split("\\s");
					String cmd = inputString[0];
					String ipAddress = inputString[1];

					String trailPath = null;
					String fileName = null;
					boolean clockwiseRotationFlag = false;

					if(inputString.length > 2){
						fileName = inputString[2];
						trailPath = inputString[3];
						clockwiseRotationFlag = new Boolean(inputString[4]);
					}


					//request from node to join.
					if(cmd.equalsIgnoreCase("join")){

						// identifier of node to be inserted.
						int nodeIdentifier = Utils.getHashCodesForIPAddress(ipAddress);
						System.out.println("IPAddress requesting to join : "+ipAddress+ " nodeIdentifier : "+nodeIdentifier);

						//new node belongs to this  nodes idSpace
						if(Utils.checkifNodeorFileIdentifierWithinLimit( nodeIdentifier, btstrpServer.node.getIdSpaceLowerLimit(),
								btstrpServer.node.getIdSpaceUpperLimit() ) ){

							System.out.println("Node Identifer "+nodeIdentifier+" belongs to this id-space, so sending response object ");

							//create a response object to send to join requesting server.
							NodeResponseObject response = new NodeResponseObject();

							response.setCommand(Input.JOIN_RESP.toString());
							response.setNodeIdentifier( btstrpServer.node.getNodeIdentifier());
							response.setProdecessorIPAddress( btstrpServer.node.getProdecessorIPAddress());
							response.setSuccessorIPAddress( btstrpServer.node.getSuccessorIPAddress());
							response.setIpAddress(PeerServer.nodeIP);

							List<MyFile> reqdFiles = new LinkedList<MyFile>();

							for(MyFile f :  btstrpServer.node.getFiles()){

								int fileIdentifier = Utils.getHashCodesForFile(f.getFileName());

								if( Utils.checkifNodeorFileIdentifierWithinLimit(fileIdentifier, btstrpServer.node.getIdSpaceLowerLimit(), 
										nodeIdentifier) ){
									reqdFiles.add(f);	
								}

							}
							response.setReqdfiles(reqdFiles);
							response.setResult("Success");

							//passing join response object to initiating node.
							Thread nodeController = new Thread(new NodeController(response.getCommand(), ipAddress, response));
							nodeController.start();

							//removing files from local node list.
							List<MyFile> files = btstrpServer.node.getFiles();
							files.removeAll(reqdFiles);

							//performing local changes in the node like changing prodecessor,changing lower limit id space etc..
							btstrpServer.performAllLocalChangesForNodeJoin(ipAddress,files);

							for(MyFile f :  reqdFiles){
								//removing that file from local directory before sending it to newly joined node.
								File[] dirFiles =  Utils.SERVER_DEST.listFiles();

								for(File file : dirFiles){
									if(file.getName().equals(f.getFileName())){
										file.delete();
									}
								}
							}
						}

						else {

							System.out.println("Node Identifer "+nodeIdentifier+" does not belong to this id-space, so forwarding to adjacent node ");

							//passing node join request to successor node.
							Thread nodeController = new Thread(new NodeController(cmd, ipAddress, 
									btstrpServer.node.getSuccessorIPAddress()));
							nodeController.start();
						}

					}
					//update prodecessor nodes successor.
					else if(cmd.equalsIgnoreCase("update_prodecessor")){
						System.out.println("Request from " +ipAddress+ " to make itself its successor");
						btstrpServer.node.setSuccessorIPAddress(ipAddress);

						btstrpServer.view();
					}

					//request from node to leave.
					else if(cmd.equalsIgnoreCase("leave")){

						System.out.println("Sending response object to successor so that it can leave the chord!!!");
						//node to be removed belongs to this idSpace
						ServerNode node = btstrpServer.node;

						//create a response object to send to join requesting server.
						NodeResponseObject response = new NodeResponseObject();

						response.setCommand(Input.LEAVE_RESP.toString());
						response.setNodeIdentifier( node.getNodeIdentifier());
						response.setProdecessorIPAddress( node.getProdecessorIPAddress());
						response.setSuccessorIPAddress( node.getSuccessorIPAddress());
						response.setIpAddress(PeerServer.nodeIP);

						List<MyFile> reqdFiles = new LinkedList<MyFile>();

						//adding files to successor node's response object.
						for(MyFile f :  node.getFiles()){
							reqdFiles.add(f);
						}

						response.setReqdfiles(reqdFiles);
						response.setResult("Success");

						//passing update to successor.
						Thread nodeController = new Thread(new NodeController(response.getCommand().toString(),
								ipAddress, response) ) ;
						nodeController.start();

						//since its leaving it deletes itw own directory.

						if(Utils.SERVER_DEST.exists())
						{
							File[] files = Utils.SERVER_DEST.listFiles();

							for(File f : files)
								f.delete();

							Utils.SERVER_DEST.delete();
						}
					}

					// request from node to search a file f.
					else if(cmd.equalsIgnoreCase("search")){

						System.out.println("IPAddress "+ipAddress+" requesting to search file : "+fileName);

						// identifier of file to be inserted.
						int fileIdentifier = Utils.getHashCodesForFile(fileName);


						//new file belongs to this  nodes idSpace
						if(Utils.checkifNodeorFileIdentifierWithinLimit(fileIdentifier, btstrpServer.node.getIdSpaceLowerLimit(), 
								btstrpServer.node.getIdSpaceUpperLimit() )){

							System.out.println("File Identifer "+fileIdentifier+" belongs to this id-space, so sending response ");

							//passing file trail path and response to node which initiated file search.
							Thread nodeController = new Thread(new NodeController(Input.SEARCH_RESP.toString(), 
									ipAddress,fileName, trailPath));
							nodeController.start();
						}
						else{

							System.out.println("File Identifer "+fileIdentifier+" does not belong to this id-space, "
									+ "so forwarding to adjacent node ");

							//passing file trail path and search request to successor or prodecessor node.

							//proceed towards successor node.
							if(clockwiseRotationFlag){
								trailPath = trailPath + Utils.getHashCodesForIPAddress(btstrpServer.node.getSuccessorIPAddress()) +"-";

								Thread nodeController = new Thread(new NodeController(cmd, ipAddress, 
										btstrpServer.node.getSuccessorIPAddress(), fileName, trailPath, clockwiseRotationFlag));
								nodeController.start();
							}
							//proceed towards prodecessor node.
							else {

								trailPath = trailPath + Utils.getHashCodesForIPAddress(btstrpServer.node.getProdecessorIPAddress()) +"-";

								Thread nodeController = new Thread(new NodeController(cmd, ipAddress, 
										btstrpServer.node.getProdecessorIPAddress(), fileName, trailPath, clockwiseRotationFlag));
								nodeController.start();
							}
						}
					}

					//request from node to insert a file f.
					else if(cmd.equalsIgnoreCase("insert")){

						System.out.println("IPAddress "+ipAddress+" requesting to insert file : "+fileName);

						// identifier of file to be inserted.
						int fileIdentifier = Utils.getHashCodesForFile(fileName);

						//new file belongs to this  nodes idSpace
						if(Utils.checkifNodeorFileIdentifierWithinLimit(fileIdentifier, btstrpServer.node.getIdSpaceLowerLimit(), 
								btstrpServer.node.getIdSpaceUpperLimit())){

							System.out.println("File Identifer "+fileIdentifier+" belongs to this id-space, so sending response object ");

							//passing file trail path and response to node which initiated file insert.
							Thread nodeController = new Thread(new NodeController(Input.INSERT_RESP.toString(),
									ipAddress,fileName, trailPath));
							nodeController.start();
						}
						else{

							System.out.println("File Identifer "+fileIdentifier+" does not belong to this id-space, "
									+ "so forwarding to adjacent node ");

							//passing file trail path and insert request to successor node.
							if(clockwiseRotationFlag){
								trailPath = trailPath + Utils.getHashCodesForIPAddress(btstrpServer.node.getSuccessorIPAddress())+"-";

								Thread nodeController = new Thread(new NodeController(cmd, ipAddress, 
										btstrpServer.node.getSuccessorIPAddress(), fileName, trailPath, clockwiseRotationFlag));
								nodeController.start();
							}else {
								trailPath = trailPath + Utils.getHashCodesForIPAddress(btstrpServer.node.getProdecessorIPAddress())+"-";

								Thread nodeController = new Thread(new NodeController(cmd, ipAddress, 
										btstrpServer.node.getProdecessorIPAddress(), fileName, trailPath, clockwiseRotationFlag));
								nodeController.start();
							}
						}

					}
				}

			}	
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	/**
	 * Method which initialized and bounds a server socket to a port.
	 * @return void.
	 */
	private void initServerSocket()
	{
		boundPort = new InetSocketAddress(port);
		try
		{
			requestHandlerSocket = new ServerSocket(port);

			if (requestHandlerSocket.isBound())
			{
				System.out.println("RequestHandler bound to data port " + requestHandlerSocket.getLocalPort() + " and is ready for receiving...");
			}
		}
		catch (Exception e)
		{
			System.out.println("Unable to initiate socket.");
		}

	}


}


