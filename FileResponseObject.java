import java.io.Serializable;

public class FileResponseObject implements Serializable {
	
	private static final long serialVersionUID = -6028808701580753017L;
	String command;
	String sourceIPAddress;
	String trailPath;
	MyFile file;
	String result;
	
	public String getSourceIPAddress() {
		return sourceIPAddress;
	}
	public void setSourceIPAddress(String sourceIPAddress) {
		this.sourceIPAddress = sourceIPAddress;
	}
	public String getTrailPath() {
		return trailPath;
	}
	public void setTrailPath(String trailPath) {
		this.trailPath = trailPath;
	}
	public MyFile getFile() {
		return file;
	}
	public void setFile(MyFile file) {
		this.file = file;
	}
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
}
