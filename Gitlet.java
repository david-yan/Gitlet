import java.io.*;
import java.nio.file.Files;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardCopyOption.*;

public class Gitlet implements Serializable
{

	private int										numberOfCommit;
	// keep track of branches
	// key is the name of the branch
	// value is most recent GitletNode of branch
	private HashMap<String, GitletNode>				branches;
	// HashMap of all commits
	// key is commit message
	// value is bucket of all commits that have the same message
	private HashMap<String, LinkedList<GitletNode>>	commits;
	private File									stagingDir	= new File(".gitlet/staging");
	private File									commitDir	= new File(".gitlet/commits");
	private HashSet<String>							untrack;
	private String									currentBranch;
	public boolean									isConflicting;								// specific
																								// for
																								// merge

	public Gitlet()
	{
		File gitletDir = new File(".gitlet");
		numberOfCommit = 0;
		untrack = new HashSet<String>();
		branches = new HashMap<String, GitletNode>();
		commits = new HashMap<String, LinkedList<GitletNode>>();
		currentBranch = "master";
		isConflicting = false;
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
		// check if there is anything to commit
		if (numberOfCommit != 0 && stagingDir.list().length == 0 && untrack.isEmpty())
		{
			System.err.println("No changes added to the commit.");
			return;
		}
		
		isConflicting = false;

		//make new GitletNode
		GitletNode commitNode = new GitletNode(message, numberOfCommit, branches.get(currentBranch));

		if (numberOfCommit > 0)
		{
			try
			{
				/*
				 * add reference of previous files that are not going to be removed to
				 * the new GitletNode folder
				 */
				copyToNewCommit(commitNode);
				/*
				 * at the end of commit staging folder should be empty so we
				 * move files from staging to the new commit
				 */
				moveFromStagingToNewCommit(commitNode);
				untrack.clear();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		numberOfCommit++;

		branches.put(currentBranch, commitNode);
		if (!commits.containsKey(commitNode.getMessage()))
			commits.put(commitNode.getMessage(), new LinkedList<GitletNode>());
		commits.get(commitNode.getMessage()).add(commitNode);
		// System.out.println("Commit successful");
	}

	/**
	 * Copy all files from staging folder to the new commit folder and adds
	 * the file name to the list of files in node
	 * @param node GitletNode to add everything to
	 * @throws IOException 
	 */
	private void moveFromStagingToNewCommit(GitletNode node) throws IOException
	{
		File newCommit = node.getFolder();
		for (File file : stagingDir.listFiles())
		{
			node.addFile(file.getName());
			File newCommitPath = new File(newCommit, file.getName());
			Files.move(file.toPath(), newCommitPath.toPath(), REPLACE_EXISTING);
		}
	}

	/**
	 * adds the names of all the files that will not be modified into the list of
	 * files in node, excluding the ones that have been untracked
	 * @param node node to add file names to
	 */
	private void copyToNewCommit(GitletNode node)
	{
		ArrayList<String> files = branches.get(currentBranch).getFiles();
		for (String fileName : files)
			if (!inStagingDir(stagingDir, fileName) && untrack.contains(fileName))
				node.addFile(fileName);
	}

	public void find(String commitMessage)
	{
		LinkedList<GitletNode> nodes = commits.get(commitMessage);
		if (nodes == null)
		{
			System.out.println("Found no commit with that message.");
			return;
		}
		for (GitletNode node : nodes)
			System.out.println(node.getID());
	}
	
	public void log()
	{
		branches.get(currentBranch).printLog();
	}
	
	public void global_log()
	{
		for (GitletNode node : branches.values())
			node.printLog();
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
		if (!inStagingDir(stagingDir, fileName) && !inHeadCommit(branches.get(currentBranch).getFolder(), fileName))
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

	public static void copyFileUsingFileChannels(File source, File dest) throws IOException
	{
		FileChannel inputChannel = null;
		FileChannel outputChannel = null;
		inputChannel = new FileInputStream(source).getChannel();
		outputChannel = new FileOutputStream(dest).getChannel();
		outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
		inputChannel.close();
		outputChannel.close();
	}

	public void merge(String branchName)
	{
		//check to see if branch exists
		if (!branches.containsKey(branchName))
		{
			System.err.println("A branch with that name does not exist.");
			return;
		}
		//cannot merge with same branch
		if (currentBranch.equals(branchName))
		{
			System.err.println("Cannot merge a branch with itself.");
			return;
		}
		GitletNode splitPoint = getSplitPoint(currentBranch, branchName);
		ArrayList<String> modifiedHere = branches.get(currentBranch).getModifiedFiles(splitPoint);
		ArrayList<String> modifiedThere = branches.get(branchName).getModifiedFiles(splitPoint);
		for (String s : modifiedThere)
			if (!modifiedHere.contains(s))
				add(branches.get(branchName).getFile(s).getAbsolutePath());
			else
			{
				isConflicting = true;
				File toStage = new File("/.gitlet/staging/", s + ".conflicting");
				try
				{
					copyFileUsingFileChannels(branches.get(branchName).getFile(s), toStage);
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		if (!isConflicting)
			commit("Merged " + currentBranch + " with " + branchName);	
	}

	private GitletNode getSplitPoint(String branch1, String branch2)
	{
		GitletNode node1 = branches.get(branch1);
		GitletNode node2 = branches.get(branch2);
		while (node1 != node2)
		{
			if (node1.getID() < node2.getID())
				node2 = node2.getPrevCommit();
			else
				node1 = node1.getPrevCommit();
		}
		return node1;
	}

	public static void main(String[] args)
	{
		Gitlet gitlet = null;
		try
		{
			FileInputStream fileIn = new FileInputStream(new File(".gitlet", "Gitlet.ser"));
			ObjectInputStream in = new ObjectInputStream(fileIn);
			gitlet = (Gitlet) in.readObject();
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
			if (gitlet == null)
				gitlet = new Gitlet();
			else
				System.err.println("A gitlet version control system already exists in the current directory.");
		}
		else
			System.err.println("No command with that name exists.");
		try
		{
			FileOutputStream fileOut = new FileOutputStream(new File(".gitlet", "Gitlet.ser"));
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(gitlet);
			out.close();
			fileOut.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
