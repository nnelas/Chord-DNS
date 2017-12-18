import java.io.DataOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class NodeController implements Runnable {

	String inputCommand;
	String fileName;
	String initialNodeIPAddress;
	String requestToIPAddress;
	NodeResponseObject response;
	String trailPath;
	MyFile file;
	String destIPAddress;
	boolean clockwiseRotationFlag;

	/*
	 * Constructor for sending request to bootstrapServer
	 * all initial requests for node joining pass through this constructor.
	 * initialNodeIPAddress is bootstrap IP.
	 * 
	 */

	public NodeController(String inputCommand, String initialNodeIPAddress){
		this.inputCommand = inputCommand;
		this.initialNodeIPAddress = initialNodeIPAddress;
	}

	/*
	 * constructor to pass node join or leave request to successor IP Address.
	 * And also handles update process of prodecessor. (here in this case requestToIPAddress is prodecessor node IP Address).
	 *  Node Leave process also uses this constructor to pass successor and prodecessor IP Address to vice versa.
	 */
	public NodeController(String inputCommand, String initialNodeIPAddress, String requestToIPAddress){
		this.inputCommand = inputCommand;
		this.initialNodeIPAddress = initialNodeIPAddress;
		this.requestToIPAddress = requestToIPAddress;
	}


	/*
	 * constructor to pass file search  and insert request to successor or prodecessor IP Address.
	 */
	public NodeController(String inputCommand, String initialNodeIPAddress, String requestToIPAddress,String fileName, String
			trailPath,boolean clockwiseRotationFlag){
		this.inputCommand = inputCommand;
		this.initialNodeIPAddress = initialNodeIPAddress;
		this.requestToIPAddress = requestToIPAddress;
		this.fileName = fileName;
		this.trailPath = trailPath;
		this.clockwiseRotationFlag = clockwiseRotationFlag;
	}

	/*
	 * Constructor for successor node with required ID space to send contents to initiating IP Address Node.
	 * 
	 */
	public NodeController(String inputCommand, String initialNodeIPAddress, NodeResponseObject response) {
		this.inputCommand = inputCommand;
		this.initialNodeIPAddress = initialNodeIPAddress;
		this.response = response;
	}


	/*
	 * sending reponse to server which initiated file search or insert.
	 * In case of insert, the inititated server should send the file to this server which has the file id-space.
	 */
	public NodeController(String inputCommand, String initialNodeIPAddress, String fileName,
			String trailPath) {

		this.inputCommand = inputCommand;
		this.initialNodeIPAddress = initialNodeIPAddress;
		this.fileName = fileName;
		this.trailPath = trailPath;	
	}

	/*
	 * destIPAddress is the dest node IP Address while file should be inserted.
	 * used by server to insert file.(forward file command uses this constructor).
	 */
	public NodeController(String inputCommand, String destIPAddress, MyFile file) {
		this.inputCommand = inputCommand;
		this.destIPAddress = destIPAddress;
		this.file = file;
	}

	@Override
	public void run() {

		try {

			Socket nodeSocket = null;

			/*
			 * Join node by sending request to bootstrap server.
			 */
			if(inputCommand.equalsIgnoreCase(Input.JOIN.toString()) )
			{
				try {

					//forward request to successor.
					if(requestToIPAddress != null)
						nodeSocket = new Socket(requestToIPAddress, 1234);		
					else{
						try{
							nodeSocket = new Socket(PeerServer.bootStrapServerIP, 1234);
						}
						catch(Exception e){
							System.out.println("Bootstrap server is not currently running, so Node can't join. Start Bootstrap server!!!");
						}
					}

					DataOutputStream dos = new DataOutputStream(nodeSocket.getOutputStream());

					System.out.println(inputCommand+ " "+initialNodeIPAddress);

					dos.writeBytes(inputCommand+ " "+initialNodeIPAddress+ '\n');

				}catch (Exception e){
					System.out.println("Exception while joining "+initialNodeIPAddress+", "+e);
				}
				nodeSocket.close();
			}

			//sends the response object to new joining node.
			else if(inputCommand.equalsIgnoreCase(Input.JOIN_RESP.toString()) )
			{
				try {

					//response details to be sent to initiating client(server).
					if(response != null)
						nodeSocket = new Socket(initialNodeIPAddress, 3000);		

					OutputStream os = nodeSocket.getOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(os);
					oos.writeObject(response);

				}catch (Exception e){
					System.out.println("Exception while passing response object to  "+initialNodeIPAddress+e);
					e.printStackTrace();
				}
				nodeSocket.close();
			}

			//for search, file is null. all other objects are set and send to server which initiated file search.
			else if(inputCommand.equalsIgnoreCase(Input.SEARCH_RESP.toString()) )
			{

				try {
					nodeSocket = new Socket(initialNodeIPAddress, 3000);		

					OutputStream os = nodeSocket.getOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(os);

					FileResponseObject responseObject = new FileResponseObject();

					MyFile f = new MyFile();
					f.setFileName(fileName);

					responseObject.setFile(f );
					responseObject.setSourceIPAddress(PeerServer.nodeIP );
					responseObject.setCommand(Input.SEARCH_RESP.toString() );
					responseObject.setTrailPath(trailPath);

					boolean present = false;
					if(Utils.SERVER_DEST.exists()){

						String[] files = Utils.SERVER_DEST.list();

						for(String str : files){

							if(str.equals(fileName)){
								responseObject.setResult("Success");
								present = true;
								break;
							}
						}
					}
					
					if(!present)
						responseObject.setResult("Failure");

					oos.writeObject(responseObject);	

				}catch (Exception e){
					System.out.println("Exception while sending search file response object  to server :  ");
					e.printStackTrace();
				}
				nodeSocket.close();
			}

			else if(inputCommand.equalsIgnoreCase(Input.INSERT_RESP.toString()) )
			{

				try {
					nodeSocket = new Socket(initialNodeIPAddress, 3000);		

					OutputStream os = nodeSocket.getOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(os);

					FileResponseObject responseObject = new FileResponseObject();
					responseObject.setSourceIPAddress(PeerServer.nodeIP );
					responseObject.setCommand(Input.INSERT_RESP.toString() );
					responseObject.setTrailPath(trailPath);
					responseObject.setResult("Success");

					MyFile f = new MyFile();
					f.setFileName(fileName);
					f.setLines(new ArrayList<String>());

					responseObject.setFile(f);

					oos.writeObject(responseObject);

				}catch (Exception e){
					System.out.println("Exception while sending insert file response object  to server : ");
					e.printStackTrace();
				}
				nodeSocket.close();
			}

			//Leave node by sending request to bootstrap server.
			else if(inputCommand.equalsIgnoreCase(Input.LEAVE.toString())){

				try {

					nodeSocket = new Socket(initialNodeIPAddress, 1234);		

					DataOutputStream dos = new DataOutputStream(nodeSocket.getOutputStream());

					dos.writeBytes(inputCommand+ " "+requestToIPAddress+ '\n');

				}catch (Exception e){
					System.out.println("Exception while leaving "+initialNodeIPAddress+", "+e);
				}
				nodeSocket.close();
			}

			//Leave node by sending request to bootstrap server.
			else if(inputCommand.equalsIgnoreCase(Input.LEAVE_RESP.toString())){

				try {

					//response details to be sent to initiating client(server).
					if(response != null)
						nodeSocket = new Socket(initialNodeIPAddress, 3000);		

					OutputStream os = nodeSocket.getOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(os);
					oos.writeObject(response);

				}catch (Exception e){
					System.out.println("Exception while passing leave response object to  "+requestToIPAddress+e);
					e.printStackTrace();
				}
				nodeSocket.close();

			}


			/*
			 * Send request to successor or prodecessor and search file from peers.
			 */
			else if(inputCommand.equalsIgnoreCase(Input.FILE_FORWARD.toString() ) )
			{

				try {

					nodeSocket = new Socket(destIPAddress, 3000);		

					OutputStream os = nodeSocket.getOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(os);

					FileResponseObject finalInsertProcessObject = new FileResponseObject();
					finalInsertProcessObject.setSourceIPAddress(PeerServer.nodeIP );
					finalInsertProcessObject.setCommand(Input.FILE_FORWARD.toString() );
					finalInsertProcessObject.setResult("Success");

					finalInsertProcessObject.setFile(file);

					oos.writeObject(finalInsertProcessObject);

				}catch (Exception e){
					System.out.println("Exception while sending insert file response object  to server : ");
					e.printStackTrace();
				}

				nodeSocket.close();
			}

			/*
			 * Send request to successor or prodecessor and search file from peers.
			 */
			else if(inputCommand.equalsIgnoreCase(Input.SEARCH.toString() ) )
			{
				try{

					nodeSocket = new Socket(requestToIPAddress, 1234);		

					DataOutputStream dos =  new DataOutputStream(nodeSocket.getOutputStream());

					dos.writeBytes(inputCommand+" "+initialNodeIPAddress+" "+fileName+" "+trailPath+" "+clockwiseRotationFlag+ '\n');

				}catch (Exception e){
					System.out.println("Exception while searching file from peers "+e);
				}

				nodeSocket.close();
			}

			//response details to be sent to prodecessor to update.
			else if(inputCommand.equalsIgnoreCase(Input.UPDATE_PRODECESSOR.toString()) )
			{

				try {

					nodeSocket = new Socket(requestToIPAddress, 1234);

					DataOutputStream dos = new DataOutputStream(nodeSocket.getOutputStream());

					dos.writeBytes(inputCommand+ " "+initialNodeIPAddress+ '\n');

				}catch (Exception e){
					System.out.println("Exception while updating  successor in prodecessor node "+initialNodeIPAddress+e);
					e.printStackTrace();
				}

				nodeSocket.close();
			}

			/*
			 * insert file into respective peer by passing request to successor or prodecessor.
			 */
			else if(inputCommand.equalsIgnoreCase(Input.INSERT.toString()) )
			{
				System.out.println("Forwarding insert request to : "+requestToIPAddress);
				try{
					nodeSocket = new Socket(requestToIPAddress, 1234);		

					DataOutputStream dos = new DataOutputStream(nodeSocket.getOutputStream());

					//System.out.println(str);
					dos.writeBytes(inputCommand+" "+initialNodeIPAddress+" "+fileName+" "+trailPath+" "+clockwiseRotationFlag+ '\n');

				}catch (Exception e){
					System.out.println("Exception while inserting file into peers "+e);
					e.printStackTrace();
				}
				nodeSocket.close();
			}

		}

		catch(Exception e){
			e.printStackTrace();
		}
	}
}


