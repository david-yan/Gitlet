import java.io.*;
import java.nio.file.Files;

import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardCopyOption.*;

public class Gitlet implements Serializable
{

	private int							numberOfCommit;
	// keep track of branches
	// key is the name of the branch
	// value is possibly the LinkedList of GitletNodes
	private HashMap<String, GitletNode>	branches;
	private File						stagingDir	= new File(".gitlet/staging");
	private File						commitDir	= new File(".gitlet/commits");
	private HashSet<String>				untrack;
	private String						currentBranch;

	public Gitlet()
	{
		File gitletDir = new File(".gitlet");
		numberOfCommit = 0;
		untrack = new HashSet<String>();
		branches = new HashMap<String, GitletNode>();
		currentBranch = "master";
		if (!gitletDir.exists())
		{
			gitletDir.mkdir();
			stagingDir.mkdir();
			commitDir.mkdir();
			commit("initial commit");
		}
	}
	public void commit(String message)
	{

		if (numberOfCommit != 0 && stagingDir.list().length == 0 && untrack.isEmpty())
		{
			System.err.println("No changes added to the commit.");
			return;
		}

		GitletNode commitNode = new GitletNode(message, numberOfCommit, branches.get(currentBranch));

		if (numberOfCommit > 0)
		{
			File newCommit = commitNode.getContents();

			try
			{

				/*
				 * put all the marked files from parent's and also any files in
				 * staging folder into contents if files from parent's have the
				 * same name as files from staging folder, select the files from
				 * staging folder since those are the updated ones check files
				 * that are marked "untrack" and do not include those in the
				 * commit
				 */
				copyToNewCommit(newCommit);
				/*
				 * at the end of commit staging folder should be empty so we
				 * move files from staging to the new commit
				 */
				moveFromStagingToNewCommit(newCommit);
				untrack.clear();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		numberOfCommit++;

		branches.put(currentBranch, commitNode);
		// System.out.println("Commit successful");
	}

	private void moveFromStagingToNewCommit(File newCommit) throws IOException
	{
		for (File file : stagingDir.listFiles())
		{
			File newCommitPath = new File(newCommit, file.getName());
			Files.move(file.toPath(), newCommitPath.toPath(), REPLACE_EXISTING);
		}
	}

	/*
	 * copy files from current commit that were unchanged or NOT marked for
	 * untracking
	 */
	private void copyToNewCommit(File newCommit) throws IOException
	{
		File pathOfCurrentCommit = branches.get(currentBranch).getContents();
		for (File file : pathOfCurrentCommit.listFiles())
		{
			if (!file.isDirectory() && !inStagingDir(stagingDir, file.getName()) && !untrack.contains(file.getName()))
			{
				File newCommitPath = new File(newCommit, file.getName());
				copyFileUsingFileChannels(file, newCommitPath);
			}
		}
	}

	public void log()
	{
		branches.get(currentBranch).printLog();
	}

	// should only add files, not folders / directory
	public void add(String fileName)
	{

		// fileName could be a path to the file
		File fileToAdd = new File(fileName);

		/*
		 * check if file exists in any directories or its subdirectries of the
		 * working directory or its in the working directory ( which is the same
		 * directory that contains the .gitlet folder )
		 */
		if (fileToAdd.isDirectory() || !fileToAdd.exists())
		{
			System.err.println("File does not exist.");
			return;
		}

		// if it was marked for "untracking", just unmark it
		if (untrack.contains(fileName))
		{
			untrack.remove(fileName);
			System.out.println("file is now unmark for \"untracking\"");
			return;
		}

		// put it in the staging folder
		try
		{

			// what if file was in a directory?
			// need to get the file name and not the path
			// use File.getname()

			File toStagingDir = new File(".gitlet/staging/" + fileToAdd.getName());
			copyFileUsingFileChannels(fileToAdd, toStagingDir);

			System.out.println("moved file to add into staging folder");

		} catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public void remove(String fileName)
	{
		// if file is not in staging folder
		// or it's not tracked by head commit

		// System.out.println(!inStagingDir(stagingDir, fileName));
		// System.out.println(!inHeadCommit(currentBranchHead.getContents(),
		// fileName));
		if (!inStagingDir(stagingDir, fileName) && !inHeadCommit(branches.get(currentBranch).getContents(), fileName))
		{
			System.err.println("No reason to remove the file.");
			return;
		}

		// if fileName is in staging folder
		// remove it from staging folder
		if (inStagingDir(stagingDir, fileName))
		{
			unstageFile(stagingDir, fileName);

			System.out.println("file unstaged");
		}

		// put it in untrack HashSet
		else if (!untrack.contains(fileName))
		{
			untrack.add(fileName);
			System.out.println("file untracked");
		} else
		{
			System.out.println("called rm but nothing happened");
		}

	}
	
	public void status()
	{
		System.out.println("=== Branches ===");
		System.out.println("*" + currentBranch);
		for(String branch: branches.keySet())
		{
			if(!branch.equals(currentBranch))
				System.out.println(branch);
		}
		System.out.println();
		
		System.out.println("=== Staged Files ===");
		for(File staged: stagingDir.listFiles())
		{
			System.out.println(staged.getPath());
		}
		System.out.println();
		
		System.out.println("=== Files Marked for Untracking");
		for(String untracked: untrack)
		{
			System.out.println(untracked);
		}
	}
	
	public void checkout(String name){
		if(branches.containsKey(name)){
			if(name.equals(currentBranch)){
				System.err.println("No need to checkout the current branch.");
			} else {
				
				currentBranch = name;
			}
		} else {
			GitletNode curr = branches.get(currentBranch);
		}
		
	}
	
	public void checkout(String name, String id){
		int commitId = (int) Integer.parseInt(id);
	}
	
	public void branch(String branchName){
		if(branches.containsKey(branchName)){
			System.err.println("A branch with that name already exists.");
		} else {
			GitletNode curr = branches.get(currentBranch);
			branches.put(branchName, curr);
		}
	}

	private static void unstageFile(File currentDir, String fileName)
	{
		for (File file : currentDir.listFiles())
		{
			if (!file.isDirectory() && file.getName().equals(fileName))
			{
				file.delete();
				return;
			}
		}
	}

	private static boolean inHeadCommit(File currentDir, String fileName)
	{
		for (File file : currentDir.listFiles())
		{
			if (!file.isDirectory() && file.getName().equals(fileName))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean inStagingDir(File currentDir, String fileName)
	{
		for (File file : currentDir.listFiles())
		{
			if (!file.isDirectory() && file.getName().equals(fileName))
			{
				return true;
			}
		}
		return false;
	}

	private static void copyFileUsingFileChannels(File source, File dest) throws IOException
	{
		FileChannel inputChannel = null;
		FileChannel outputChannel = null;
		try
		{
			inputChannel = new FileInputStream(source).getChannel();
			outputChannel = new FileOutputStream(dest).getChannel();
			outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
		} finally
		{
			inputChannel.close();
			outputChannel.close();
		}
	}

	public static void main(String[] args)
	{
		Gitlet gitlet = null;
		boolean gitletExists = false;
		try
		{
			FileInputStream fileIn = new FileInputStream(new File(".gitlet", "Gitlet.ser"));
			ObjectInputStream in = new ObjectInputStream(fileIn);
			gitlet = (Gitlet) in.readObject();
			gitletExists = true;
			in.close();
			fileIn.close();
		} catch (Exception e)
		{
		}

		if (args.length == 0)
		{
			System.err.println("Please enter a command.");
		}

		else if (args[0].equals("commit"))
		{

			if (args.length == 2 && args[1].trim().length() != 0)
			{
				gitlet.commit(args[1]);
			} else
			{
				System.err.println("Please enter a commit messsage.");
			}

		}

		else if (args[0].equals("add"))
		{
			gitlet.add(args[1]);

		}

		else if (args[0].equals("rm"))
		{
			gitlet.remove(args[1]);
		}

		else if (args[0].equals("log"))
		{
			gitlet.log();
		}

		else if (args[0].equals("init"))
		{
			if (!gitletExists)
			{
				gitlet = new Gitlet();
			} else
			{
				System.err.println("A gitlet version control system already exists in the current directory.");
			}
		} 
		
		else if (args[0].equals("status"))
		{
			gitlet.status();
		}
		
		else if (args[0].equals("branch"))
		{
			gitlet.branch(args[1]);
		}
		
		else if (args[0].equals("checkout"))
		{
			if(args.length == 2){
				gitlet.checkout(args[1]);
			} else if(args.length == 3){
				gitlet.checkout(args[1], args[2]);
			}
		}
		
		try
		{
			FileOutputStream fileOut = new FileOutputStream(new File(".gitlet//Gitlet.ser"));
		} catch (Exception e)
		{
			System.err.println("No command with that name exists.");
		}
		try
		{
			FileOutputStream fileOut = new FileOutputStream(new File(".gitlet", "Gitlet.ser"));
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(gitlet);
			out.close();
			fileOut.close();
			// System.out.println("Gitlet written in file.");
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
