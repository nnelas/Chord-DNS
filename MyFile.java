import java.io.Serializable;
import java.util.List;

public class MyFile implements Serializable {

	private static final long serialVersionUID = 8672760694051365494L;
	private String fileName;
	private List<String> lines;
	private String content;
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public List<String> getLines() {
		return lines;
	}
	public void setLines(List<String> lines) {
		this.lines = lines;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getContent(){
		return content;
	}
}
