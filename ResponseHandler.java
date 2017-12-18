import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;


public class ResponseHandler implements Runnable {

	private InetSocketAddress boundPort = null;
	private final int port =  3000;
	private ServerSocket requestHandlerSocket;
	private PeerServer btstrpServer;

	public ResponseHandler(PeerServer btstrpServer) {
		this.btstrpServer = btstrpServer;
	}

	@Override
	public void run() {

		try {

			initServerSocket();

			while(true) {

				Socket connectionSocket;
				ObjectInputStream ois;
				InputStream inputStream;


				try {
					connectionSocket = requestHandlerSocket.accept();
					inputStream = connectionSocket.getInputStream();
					ois = new ObjectInputStream(inputStream);

					Object obj = ois.readObject();

					//handling two different response objects from peers
					if(obj instanceof NodeResponseObject){

						NodeResponseObject response = (NodeResponseObject) obj;

						//response from node join and resetting values in new node to join to chord.
						if(response.getCommand().equalsIgnoreCase("join_resp")){

							NodeResponseObject responseObject = (NodeResponseObject) response;

							ServerNode node = btstrpServer.node;
							int nodeIdentifier = Utils.getHashCodesForIPAddress(PeerServer.nodeIP);

							//setting this node as successor.
							int prodecessorNodeId = Utils.getHashCodesForIPAddress(responseObject.getProdecessorIPAddress());

							node.getFiles().addAll(responseObject.getReqdfiles());
							
							//writing to local directory.
							for(MyFile myFile : responseObject.getReqdfiles() )
								writeFile(myFile);
							
							node.setIdSpaceUpperLimit( (nodeIdentifier) % Utils.TOTAL_ID_SPACE) ;
							node.setIdSpaceLowerLimit( (prodecessorNodeId + 1) % Utils.TOTAL_ID_SPACE);
							node.setNodeIdentifier(nodeIdentifier);
							node.setProdecessorIPAddress(responseObject.getProdecessorIPAddress());

							//successor will be the node which send response.
							node.setSuccessorIPAddress(responseObject.getIpAddress());

							//System.out.println(node);

							System.out.println("Updating Prodecessor : "+node.getProdecessorIPAddress());

							//passing update to prodecessor node to update its successor.
							Thread nodeController = new Thread(new NodeController(Input.UPDATE_PRODECESSOR.toString(),
									PeerServer.nodeIP, node.getProdecessorIPAddress()) ) ;
							nodeController.start();

							System.out.println("Join Result : "+response.getResult());
							btstrpServer.view();
						}

						else if(response.getCommand().equalsIgnoreCase("leave_resp") ){

							System.out.println("Making changes in successor node as its prodecessor recently left ");
							NodeResponseObject responseObject = (NodeResponseObject) response;

							ServerNode node = btstrpServer.node;
							int nodeIdentifier = Utils.getHashCodesForIPAddress(PeerServer.nodeIP);

							//setting this node as successor.
							int prodecessorNodeId = Utils.getHashCodesForIPAddress(responseObject.getProdecessorIPAddress());

							node.getFiles().addAll(responseObject.getReqdfiles());
							node.setIdSpaceUpperLimit( (nodeIdentifier) % Utils.TOTAL_ID_SPACE) ;
							node.setIdSpaceLowerLimit( (prodecessorNodeId + 1) % Utils.TOTAL_ID_SPACE);
							node.setNodeIdentifier(nodeIdentifier);
							node.setProdecessorIPAddress(responseObject.getProdecessorIPAddress());

							System.out.println("Writing "+responseObject.getReqdfiles().size()+" files from old prodecessor server files to its own directoy ");
							
							//writing all files from leaving node to local directory.
							if(Utils.SERVER_DEST.exists())
							{
								for ( MyFile myFile : responseObject.getReqdfiles() ){
									writeFile(myFile);
								}
							}
							else{

									if (Utils.SERVER_DEST.mkdir()) {
										System.out.println("Directory "+Utils.SERVER_DEST+"  is created!!!");
										
										for(MyFile myFile : responseObject.getReqdfiles()){
											writeFile(myFile);
										}
									} 
									else {
										System.out.println("Directory creation failure!!");
									}
								}
							


							System.out.println("Updating Prodecessor's  Successor value : "+response.getProdecessorIPAddress());

							//passing update to prodecessor node to update its successor.
							Thread nodeController = new Thread(new NodeController(Input.UPDATE_PRODECESSOR.toString(),
									PeerServer.nodeIP, response.getProdecessorIPAddress()) ) ;
							nodeController.start();

						}
					}
			

					// file response Object.
					else if(obj instanceof FileResponseObject){

						FileResponseObject fileResponseObj = (FileResponseObject) obj;

						//response from node join and resetting values in new node to join to chord.
						if(fileResponseObj.getCommand().equalsIgnoreCase("insert_resp")){

							String destIPAddress = fileResponseObj.getSourceIPAddress();
							String fileName = fileResponseObj.getFile().getFileName();
							List<String> lines = fileResponseObj.getFile().getLines();

							System.out.println("Inserting file "+fileName
									+" into Node : "+destIPAddress+ " Node ID : "
									+ Utils.getHashCodesForIPAddress(destIPAddress));


							BufferedReader br = null;

							File f = new File(fileName);
							FileReader reader = new FileReader(f);
							br = new BufferedReader(reader);

							String str = null;

							while( ( str = br.readLine()) != null ){
								lines.add(str);
							}

							String content;
							content = new Scanner(new File(fileName)).useDelimiter("\\A").next();

							MyFile file = new MyFile();
							file.setFileName(fileName);
							file.setLines(lines);
							file.setContent(content);


							Thread nodeController = new Thread(new NodeController(Input.FILE_FORWARD.toString(),
									destIPAddress, file) );
							nodeController.start();

							btstrpServer.printFileInsertInfo(fileResponseObj);

						}

						//response from node join and resetting values in new node to join to chord.
						else if(fileResponseObj.getCommand().equalsIgnoreCase("search_resp")){
							btstrpServer.printSearchInfo(fileResponseObj);
						}


						// get the file from node and creates a file and write to it. Insertion of file will be complete.
						else if(fileResponseObj.getCommand().equalsIgnoreCase("file_forward")){

							System.out.println("Receiving Forwarded file from "+fileResponseObj.getSourceIPAddress());

							if (!Utils.SERVER_DEST.exists()) {

								if (Utils.SERVER_DEST.mkdir()) {
									System.out.println("Directory is created!!!");
								} 

								else {
									System.out.println("Directory creation failure!!");
								}
							}

							File file = new File(Utils.SERVER_DEST, fileResponseObj.getFile().getFileName());
							System.out.println("New File : "+fileResponseObj.getFile().getFileName()+ " added to directory : "+
									Utils.SERVER_DEST);
							FileWriter fw = new FileWriter(file);

							BufferedWriter bw = new BufferedWriter(fw);

							for(String str : fileResponseObj.getFile().getLines()) {

								try {

									bw.write(str+"\n");
									bw.flush();

								} catch (Exception e) {
									e.printStackTrace();
								}
							}

							System.out.println("File inserted by : "+fileResponseObj.getSourceIPAddress());
							btstrpServer.node.getFiles().add(fileResponseObj.getFile());
							btstrpServer.printFileInsertInfo(fileResponseObj);
						}
					}

				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}	
		} catch (IOException e) {
			e.printStackTrace();
		}


}

	/**
	 * method which initialized and bounds a server socket to a port.
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
				System.out.println("ResponseHandler bound to data port " + requestHandlerSocket.getLocalPort() + " and is ready for receiving...");
			}
		}
		catch (Exception e)
		{
			System.out.println("Unable to initiate socket.");
		}

	}

	/**
	 * write file to directory.
	 */

	public void writeFile(MyFile f){

		if (!Utils.SERVER_DEST.exists()) {

			if (Utils.SERVER_DEST.mkdir()) {
				System.out.println("Directory is created!!!");
			} 

			else {
				System.out.println("Directory creation failure!!");
			}
		}
		
		File file = new File(Utils.SERVER_DEST, f.getFileName());
		System.out.println("New File : "+f.getFileName()+ " added to directory : "+
				Utils.SERVER_DEST);

		FileWriter fw;
		BufferedWriter bw;
		
		try {
			fw = new FileWriter(file);
			bw = new BufferedWriter(fw);
			
			for(String str : f.getLines()) {
				
					bw.write(str+"\n");
					bw.flush();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
}
	
	



