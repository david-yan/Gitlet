import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

public class GitletNode implements Serializable
{
	private GitletNode			prevCommit; //previous commit node. null for first commit
	private String				commitMessage;
	private String				timeStamp;
	private int					commitID;
	private File				folder;
	private ArrayList<String>	nameOfFiles; //names of all of the files in this commit, 
											 //including the ones not written to the folder

	public GitletNode(String message, int ID, GitletNode prev)
	{
		prevCommit = prev;
		commitMessage = message;
		timeStamp = getTimeStamp();
		commitID = ID;
		nameOfFiles = new ArrayList<String>();
		folder = new File(".gitlet/commits/" + commitID);
		folder.mkdir();
	}
	
	public String getTimeStamp()
	{
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}

	public void printLog()
	{
		System.out.println("===");
		System.out.println("Commit " + commitID);
		System.out.println(timeStamp);
		System.out.println(commitMessage);
		if (prevCommit != null)
		{
			System.out.println();
			prevCommit.printLog();
		}
	}
	
	public void print()
	{
		System.out.println("===");
		System.out.println("Commit " + commitID);
		System.out.println(timeStamp);
		System.out.println(commitMessage);
	}

	public GitletNode getPrevCommit()
	{
		return prevCommit;
	}
	public int getID()
	{
		return commitID;
	}
	public String getMessage()
	{
		return commitMessage;
	}
	/**
	 * @return list of all files associated with this commit
	 */
	public ArrayList<String> getFiles()
	{
		return nameOfFiles;
	}

	/**
	 * Add file name to the list of files. 
	 * @param fileName Name of file to add
	 */
	public void addFile(String fileName)
	{
		nameOfFiles.add(fileName);
	}
	
	/**
	 * @return File of the folder of this commit
	 */
	public File getFolder()
	{
		return folder;
	}
	
		// modified for rebase
	public ArrayList<File> getModifiedFilesForRebase(GitletNode node)
	{
		GitletNode current = this;
		ArrayList<File> toReturn = new ArrayList<File>();
		while(current != node) {
			for (File file : current.getFolder().listFiles())
				if (!toReturn.contains(file.getName()))
					toReturn.add(file);
			current = current.prevCommit;
		}
		return toReturn;
	}
	
	// modified for rebase
	public ArrayList<String> getNonModifiedFiles() {
		GitletNode current = this;
		ArrayList<String> toRtn = new ArrayList<String>();
		
		// this runs in O(n^2)
		for (String fileName : current.getFiles()) {
			for (File modifiedFile : getFolder().listFiles()) {
				if (!fileName.equals(modifiedFile.getName())) {
					toRtn.add(fileName);
				}
			}
		}
		return toRtn;
	}
	
	/**
	 * Gets the ArrayList of the names of all of the files that have been 
	 * modified since the node
	 * @param node The node to be compared with
	 * @return List of names of modified files
	 */
	public ArrayList<String> getModifiedFiles(GitletNode node)
	{
		GitletNode current = this;
		ArrayList<String> toReturn = new ArrayList<String>();
		while(current != node) {
			for (File file : current.getFolder().listFiles())
				if (!toReturn.contains(file.getName()))
					toReturn.add(file.getName());
			current = current.prevCommit;
		}
		return toReturn;
	}
	
	/**
	 * Finds and returns the file with the given name
	 * @param fileName: The name of the file to be returned
	 * @return Most recent occurrence of the file or null if the file does not exist
	 */
	public File getFile(String fileName)
	{
		String updatedName = ".gitlet/commits/" + commitID + "/" + fileName;
		updatedName = updatedName.replace("/", "\\").trim();
		File searched = searchFolders(folder.listFiles(), updatedName);
		if(searched != null)
			return searched;
		else if (prevCommit != null)
			return prevCommit.getFile(fileName);
		return null;
	}
	
	private static File searchFolders(File[] files, String fileName){
		for(File file: files){
			if(file.getPath().equals(fileName))
				return file;
			if(file.isDirectory())
				return searchFolders(file.listFiles(), fileName);
		}
		return null;
	}
}
