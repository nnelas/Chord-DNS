import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;


public class ServerNode implements Serializable {

	private static final long serialVersionUID = -3550972966941438906L;
	private int nodeIdentifier;
	private String successorIPAddress;
	private String prodecessorIPAddress;
	private int idSpaceLowerLimit;
	private int idSpaceUpperLimit;
	private List<MyFile> files;
	
	public ServerNode(){
		this.files = new LinkedList<MyFile>();
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
	public int getIdSpaceLowerLimit() {
		return idSpaceLowerLimit;
	}
	public void setIdSpaceLowerLimit(int idSpaceLowerLimit) {
		this.idSpaceLowerLimit = idSpaceLowerLimit;
	}
	public int getIdSpaceUpperLimit() {
		return idSpaceUpperLimit;
	}
	public void setIdSpaceUpperLimit(int idSpaceUpperLimit) {
		this.idSpaceUpperLimit = idSpaceUpperLimit;
	}
	public List<MyFile> getFiles() {
		return files;
	}
	public void setFiles(List<MyFile> files) {
		this.files = files;
	}
	public int getNodeIdentifier() {
		return nodeIdentifier;
	}
	public void setNodeIdentifier(int nodeIdentifier) {
		this.nodeIdentifier = nodeIdentifier;
	}

	@Override
	public String toString() {
		return "ServerNode [nodeIdentifier=" + nodeIdentifier
				+ ", successorIPAddress=" + successorIPAddress
				+ ", prodecessorIPAddress=" + prodecessorIPAddress
				+ ", idSpaceLowerLimit=" + idSpaceLowerLimit
				+ ", idSpaceUpperLimit=" + idSpaceUpperLimit + ", files="
				+ files + "]";
	}
	
}
