import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

public class GitletNode implements Serializable {
	private GitletNode prevCommit;
	private String commitMessage;
	private String timeStamp;
	private int commitID;
	
	public GitletNode() {
		prevCommit = null;
		commitMessage = "initial commit";
		timeStamp = getTimeStamp();
		commitID = 1;
	}
	
	public String getTimeStamp() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}
	
	public void printLog() {
		System.out.println("===");
		System.out.println("Commit " + commitID);
		System.out.println(timeStamp);
		System.out.println(commitMessage);
		if (prevCommit != null) {
			System.out.println();
			prevCommit.printLog();
		}
	}
	
	public GitletNode(String message, int ID, GitletNode prev) {
		prevCommit = prev;
		commitMessage = message;
		timeStamp = getTimeStamp();
		commitID = ID;
	}
	
}
