import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

public class GitletNode implements Serializable
{
	private GitletNode			prevCommit;	// previous commit node. null
												// for first commit
	private String				commitMessage;
	private String				timeStamp;
	private int					commitID;
	private File				folder;
	private ArrayList<String>	nameOfFiles;	// names of all of the files in
												// this commit,
												// including the ones not
												// written to the folder

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
	 * 
	 * @param fileName
	 *            Name of file to add
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
		while (current != node)
		{
			for (File file : current.getFolder().listFiles())
				if (!toReturn.contains(file.getName()))
					toReturn.add(file);
			// fixed
			current = current.prevCommit;
		}
		return toReturn;
	}

	/**
	 * Gets the ArrayList of the names of all of the files that have been
	 * modified since the node
	 * 
	 * @param node
	 *            The node to be compared with
	 * @return List of names of modified files
	 */
	public LinkedList<String> getModifiedFiles(GitletNode node)
	{
		GitletNode current = this;
		LinkedList<String> toReturn = new LinkedList<String>();
		while (current != node)
		{
			for (String fileName : nameOfFiles)
			{
				File file = new File(folder, fileName);
				if (file.exists() && !toReturn.contains(fileName))
					toReturn.add(fileName);
			}
			// fixed
			current = current.prevCommit;
		}
		return toReturn;
	}

	/**
	 * Finds and returns the file with the given name
	 * 
	 * @param fileName
	 *            : The name of the file to be returned
	 * @return Most recent occurrence of the file or null if the file does not
	 *         exist
	 */
	public File getFile(String fileName)
	{
		if (!nameOfFiles.contains(fileName))
			return null;
		File file = new File(".gitlet/commits/" + commitID + "/" + fileName);
		if (file.exists())
			return file;
		return prevCommit.getFile(fileName);
	}
}
