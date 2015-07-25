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
	private final File								STAGING_DIR	= new File(".gitlet/staging");
	private final File								COMMIT_DIR	= new File(".gitlet/commits");
	private HashSet<String>							untrack;
	private String									currentBranch;
	public boolean									isConflicting;								// specific
																								// for
																								// merge
	private HashMap<String, GitletNode>				tableOfCommitID;

	public Gitlet()
	{
		File gitletDir = new File(".gitlet");
		numberOfCommit = 0;
		untrack = new HashSet<String>();
		branches = new HashMap<String, GitletNode>();
		commits = new HashMap<String, LinkedList<GitletNode>>();
		tableOfCommitID = new HashMap<String, GitletNode>();
		currentBranch = "master";
		branches.put(currentBranch, null);
		isConflicting = false;
		if (!gitletDir.exists())
		{
			gitletDir.mkdir();
			STAGING_DIR.mkdir();
			COMMIT_DIR.mkdir();
			commit("initial commit");
		}
	}
	public void commit(String message)
	{
		// check if there is anything to commit
		if (numberOfCommit != 0 && STAGING_DIR.list().length == 0 && untrack.isEmpty())
		{
			System.out.println("No changes added to the commit.");
			return;
		}

		isConflicting = false;

		// make new GitletNode
		GitletNode commitNode = new GitletNode(message, numberOfCommit, branches.get(currentBranch));

		if (numberOfCommit > 0)
		{
			try
			{
				/*
				 * add reference of previous files that are not going to be
				 * removed to the new GitletNode folder
				 */
				copyToNewCommit(commitNode);
				/*
				 * at the end of commit staging folder should be empty so we
				 * move files from staging to the new commit
				 */
				moveFromStagingToNewCommit(commitNode);
				untrack.clear();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		numberOfCommit++;

		branches.put(currentBranch, commitNode);
		if (!commits.containsKey(commitNode.getMessage()))
			commits.put(commitNode.getMessage(), new LinkedList<GitletNode>());
		commits.get(commitNode.getMessage()).add(commitNode);
		tableOfCommitID.put(Integer.toString(commitNode.getID()), commitNode);
	}

	/**
	 * Copy all files from staging folder to the new commit folder and adds the
	 * file name to the list of files in node
	 * 
	 * @param node
	 *            GitletNode to add everything to
	 * @throws IOException
	 */
	private void moveFromStagingToNewCommit(GitletNode node) throws IOException
	{
		File newCommit = node.getFolder();
		for (File file : STAGING_DIR.listFiles())
		{
			node.addFile(file.getName());
			File newCommitPath = new File(newCommit, file.getName());
			Files.move(file.toPath(), newCommitPath.toPath(), REPLACE_EXISTING);
		}
	}

	/**
	 * adds the names of all the files that will not be modified into the list
	 * of files in node, excluding the ones that have been untracked
	 * 
	 * @param node
	 *            node to add file names to
	 */
	private void copyToNewCommit(GitletNode node)
	{
		ArrayList<String> files = branches.get(currentBranch).getFiles();
		for (String fileName : files)
			if (!inStagingDir(STAGING_DIR, fileName) && !untrack.contains(fileName))
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
			node.print();
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
			System.out.println("File does not exist.");
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

			File toStagingDir = new File(".gitlet/staging/" + fileToAdd.getPath());
			toStagingDir.getParentFile().mkdirs();
			copyFileUsingFileChannels(fileToAdd, toStagingDir);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public void remove(String fileName)
	{
		// if file is not in staging folder
		// or it's not tracked by head commit

		if (!inStagingDir(STAGING_DIR, fileName) && !inHeadCommit(branches.get(currentBranch).getFolder(), fileName))
		{
			System.out.println("No reason to remove the file.");
			return;
		}

		// if fileName is in staging folder
		// remove it from staging folder
		if (inStagingDir(STAGING_DIR, fileName))
		{
			unstageFile(STAGING_DIR, fileName);

			System.out.println("file unstaged");
		}

		// put it in untrack HashSet
		else if (!untrack.contains(fileName))
		{
			untrack.add(fileName);
			System.out.println("file untracked");
		}
		else
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
		// check to see if branch exists
		if (!branches.containsKey(branchName))
		{
			System.out.println("A branch with that name does not exist.");
			return;
		}
		// cannot merge with same branch
		if (currentBranch.equals(branchName))
		{
			System.out.println("Cannot merge a branch with itself.");
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
				}
				catch (IOException e)
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

	public void status()
	{
		System.out.println("=== Branches ===");
		System.out.println("*" + currentBranch);
		for (String branch : branches.keySet())
		{
			if (!branch.equals(currentBranch))
				System.out.println(branch);
		}
		System.out.println();

		System.out.println("=== Staged Files ===");
		for (File staged : STAGING_DIR.listFiles())
		{
			System.out.println(staged.getPath());
		}
		System.out.println();

		System.out.println("=== Files Marked for Untracking ===");
		for (String untracked : untrack)
		{
			System.out.println(untracked);
		}
	}

	public void branch(String branchName)
	{
		if (branches.containsKey(branchName))
		{
			System.out.println("A branch with that name already exists.");
		}
		else
		{
			GitletNode curr = branches.get(currentBranch);
			branches.put(branchName, curr);
		}
	}

	public void removeBranch(String branchName)
	{
		if (!branches.containsKey(branchName))
		{
			System.out.println("A branch with that name does not exist.");
		}

		else if (currentBranch.equals(branchName))
		{
			System.out.println("Cannot remove the current branch.");
		}

		else
		{
			branches.remove(branchName);
		}
	}

	public void checkout(String name) throws IOException
	{
		if (branches.containsKey(name))
		{
			if(name.equals(currentBranch)){
				System.out.println("No need to checkout the current branch.");
			} else {
				GitletNode curr = branches.get(name);
				for(String file: curr.getFiles()){
					File requestedFile = curr.getFile(file);
					File toWorkingDir = new File(requestedFile.getName());
					copyFileUsingFileChannels(requestedFile, toWorkingDir);
				}
				currentBranch = name;
			}
		}
		else
		{
			GitletNode curr = branches.get(currentBranch);		
			File toWorkingDir = new File(name);
			File requestedFile = curr.getFile(".gitlet/" + curr.getID() + "/" + name);
			if(requestedFile == null){
				System.out.println("File does not exist in the most recent commit, or no such branch exists.");
			} else {
				copyFileUsingFileChannels(requestedFile, toWorkingDir);
			}
		}

	}

	public void checkout(String id, String name) throws IOException
	{
		GitletNode curr = tableOfCommitID.get(id);
		File toWorkingDir = new File(name);
		if (curr == null)
		{
			System.out.println("No commit with that id exists.");
		}
		else
		{
			File requestedFile = curr.getFile(".gitlet/" + id + "/" + name);
			if (requestedFile == null)
			{
				System.out.println("File does not exist in the most recent commit, or no such branch exists.");
			}
			else
			{
				copyFileUsingFileChannels(requestedFile, toWorkingDir);
			}
		}
	}

	/**
	 * checks out all the files tracked by the commit corresponding to the given
	 * commit ID and set the current branch's head to point to that commit node
	 * 
	 * @param commitID
	 *            to find the corresponding commit node
	 * @throws IOException
	 *             because checkout throws IOException but no exception would be
	 *             thrown because each file exists
	 */
	public void reset(String commitID) throws IOException
	{
		if (!tableOfCommitID.containsKey(commitID))
		{
			System.out.println("No commit with that id exists.");
			return;
		}

		// corresponding commit node of the given commit ID
		GitletNode toReset = tableOfCommitID.get(commitID);

		// need to get contents of node
		// then check out each file tracked by the node
		for (String fileName : toReset.getFiles())
		{
			checkout(fileName, commitID);
		}

		// then move current branch's head to point to node
		branches.put(currentBranch, toReset);
	}

	/*****************************************************************************/
	/**
	 * The next methods are for testing purpose ONLY
	 */

	public HashMap<String, GitletNode> getBranches()
	{
		return branches;
	}

	public HashMap<String, LinkedList<GitletNode>> getCommits()
	{
		return commits;
	}

	public HashSet<String> getUntrack()
	{
		return untrack;
	}
	public String getCurrentBranch()
	{
		return currentBranch;
	}

	/*****************************************************************************/
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
		}
		catch (Exception e)
		{}

		if (args.length == 0)
			System.out.println("Please enter a command.");
		else if (args[0].equals("commit"))
		{

			if (args.length == 2 && args[1].trim().length() != 0)
			{
				gitlet.commit(args[1]);
			}
			else
			{
				System.out.println("Please enter a commit message.");
			}

		}
		else if (args[0].equals("add"))
			gitlet.add(args[1]);
		else if (args[0].equals("rm"))
			gitlet.remove(args[1]);
		else if (args[0].equals("log"))
			gitlet.log();
		else if (args[0].equals("init"))
		{
			if (gitlet == null)
				gitlet = new Gitlet();
			else
				System.out.println("A gitlet version control system already exists in the current directory.");
		}
		else if (args[0].equals("merge"))
			gitlet.merge(args[1]);
		else if (args[0].equals("branch"))
			gitlet.branch(args[1]);
		else if (args[0].equals("status"))
			gitlet.status();
		else if (args[0].equals("rm-branch"))
			gitlet.removeBranch(args[1]);
		else if (args[0].equals("checkout"))
		{
			if (args.length == 2)
			{
				try
				{
					gitlet.checkout(args[1]);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			else if (args.length == 3)
			{
				try
				{
					gitlet.checkout(args[1], args[2]);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		else
			System.out.println("No command with that name exists.");
		try
		{
			FileOutputStream fileOut = new FileOutputStream(new File(".gitlet", "Gitlet.ser"));
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(gitlet);
			out.close();
			fileOut.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
