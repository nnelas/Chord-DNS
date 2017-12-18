import java.io.Serializable;
import java.util.List;


public class NodeResponseObject implements Serializable{

	private static final long serialVersionUID = 494480654314891255L;
	private String command;
	private String result;
	private String ipAddress;
	private int nodeIdentifier;
	private String successorIPAddress;
	private String prodecessorIPAddress;
	private List<MyFile> reqdfiles;
	
	
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public int getNodeIdentifier() {
		return nodeIdentifier;
	}
	public void setNodeIdentifier(int nodeIdentifier) {
		this.nodeIdentifier = nodeIdentifier;
	}
	public String getSuccessorIPAddress() {
		return successorIPAddress;
	}
	public void setSuccessorIPAddress(String successorIPAddress) {
		this.successorIPAddress = successorIPAddress;
	}
	public String getProdecessorIPAddress() {
		return prodecessorIPAddress;
	}
	public void setProdecessorIPAddress(String prodecessorIPAddress) {
		this.prodecessorIPAddress = prodecessorIPAddress;
	}
	public List<MyFile> getReqdfiles() {
		return reqdfiles;
	}
	public void setReqdfiles(List<MyFile> reqdfiles) {
		this.reqdfiles = reqdfiles;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
}
