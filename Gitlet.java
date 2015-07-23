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
	private HashSet<String>								untrack;
	private HashMap<String, GitletNode> 						tableOfCommitID;
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
		tableOfCommitID = new HashMap<String, GitletNode>();
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
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		// adds the node into HashSet ID
		// in order for reset to access each node in constant time
		tableOfCommitID.put(Integer.toString(numberOfCommit), commitNode);
		numberOfCommit++;

		branches.put(currentBranch, commitNode);
		if (!commits.containsKey(commitNode.getMessage()))
			commits.put(commitNode.getMessage(), new LinkedList<GitletNode>());
		commits.get(commitNode.getMessage()).add(commitNode);
		// System.out.println("Commit successful");
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
		for (File file : stagingDir.listFiles())
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
			System.out.println("No reason to remove the file.");
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
		for (File staged : stagingDir.listFiles())
		{
			System.out.println(staged.getPath());
		}
		System.out.println();

		System.out.println("=== Files Marked for Untracking");
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
		} else
		{
			GitletNode curr = branches.get(currentBranch);
			branches.put(branchName, curr);
		}
	}
	
	public void checkout(String name) throws IOException{
		if(branches.containsKey(name)){
			if(name.equals(currentBranch)){
				System.out.println("No need to checkout the current branch.");
			} else {
				GitletNode curr = branches.get(name);
				for(String file: curr.getFiles()){
					File requestedFile = curr.getFile(file);
					Files.copy(requestedFile.toPath(), new File("").toPath(), REPLACE_EXISTING);
				}
				currentBranch = name;
			}
		} else {
			GitletNode curr = branches.get(currentBranch);		
			File temp = new File(name);
			File requestedFile = curr.getFile(temp.getName());
			if(requestedFile == null){
				System.out.println("File does not exist in the most recent commit, or no such branch exists.");
			} else {
				Files.copy(requestedFile.toPath(), temp.toPath(), REPLACE_EXISTING);
			}
		}
		
	}
	
	public void checkout(String name, String id) throws IOException{
		GitletNode curr = tableOfCommitID.get(id); 
		File temp = new File(name);
		if(curr == null){
			System.out.println("No commit with that id exists.");
		} else {
			File requestedFile = curr.getFile(temp.getName());
			if(requestedFile == null){
				System.out.println("File does not exist in the most recent commit, or no such branch exists.");
			} else {
				Files.copy(requestedFile.toPath(), temp.toPath(), REPLACE_EXISTING);
			}
		}
	}
	
	/**
	 * remove the branch with the given branch name
	 * 
	 * @param branchName
	 *            to check if the branch exist
	 */
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
	public void reset(String commitID) throws IOException {
		if (!tableOfCommitID.containsKey(commitID)) {
			System.out.println("No commit with that id exists.");
			return;
		}

		// corresponding commit node of the given commit ID
		GitletNode toReset = tableOfCommitID.get(commitID);

		// need to get contents of node
		// then check out each file tracked by the node
		for (String fileName : toReset.getFiles()) {
			checkout(fileName, commitID);
		}

		// then move current branch's head to point to node
		branches.put(currentBranch, toReset);
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
			System.out.println("Please enter a command.");
		} else if (args[0].equals("commit"))
		{

			if (args.length == 2 && args[1].trim().length() != 0)
			{
				gitlet.commit(args[1]);
			} else
			{
				System.out.println("Please enter a commit messsage.");
			}

		} else if (args[0].equals("add"))
		{
			gitlet.add(args[1]);

		} else if (args[0].equals("rm"))
		{
			gitlet.remove(args[1]);
		} else if (args[0].equals("log"))
		{
			gitlet.log();
		} else if (args[0].equals("init"))
		{
			if (gitlet == null)
				gitlet = new Gitlet();
			else
				System.out.println("A gitlet version control system already exists in the current directory.");
		} else if (args[0].equals("merge"))
			gitlet.merge(args[1]);
			
		else if (args[0].equals("checkout"))
		{
			if(args.length == 2){
				try {
					gitlet.checkout(args[1]);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if(args.length == 3){
				try {
					gitlet.checkout(args[1], args[2]);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else if (args[0].equals("reset")) {
			try {
				gitlet.reset(args[1]);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else
			System.out.println("No command with that name exists.");
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
