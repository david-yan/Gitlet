import java.io.*;

public class Gitlet implements Serializable {

	private int numberOfCommit;
	private GitletNode currentBranchHead;

	public Gitlet() {
		File gitletDir = new File(".gitlet");
		if (!gitletDir.exists()) {
			gitletDir.mkdir();
			this.commit();
		}
		numberOfCommit = 1;
		numberOfCommit++;
	}

	public void commit(String message) {
		GitletNode commitNode = new GitletNode(message, numberOfCommit, currentBranchHead);
	}

	public void commit() {
		GitletNode initialCommit = new GitletNode();
		// try {
		// FileOutputStream commitToFile = new FileOutputStream(new File(
		// "commit#1.ser"));
		// ObjectOutputStream out = new ObjectOutputStream(commitToFile);
		// out.writeObject(initialCommit);
		// out.close();
		// commitToFile.close();
		// System.out.println("GitletNode written in file.");
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		currentBranchHead = initialCommit;
	}

	public void log() {
		currentBranchHead.printLog();
	}

	public static void main(String[] args) {
		Gitlet gitlet = null;
		boolean gitletExists = false;
		try {
			FileInputStream fileIn = new FileInputStream(new File(".gitlet",
					"Gitlet.ser"));
			ObjectInputStream in = new ObjectInputStream(fileIn);
			gitlet = (Gitlet) in.readObject();
			gitletExists = true;
//			System.out.println("in here");
			in.close();
			fileIn.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		}

		if (args.length == 0) {
			System.err.println("Please enter a command.");
		}

		else if (args[0].equals("log")) {
//			System.out.println(gitlet.currentBranchHead);
			gitlet.log();
		}

		else if (args[0].equals("init")) {
			if (!gitletExists) {
				gitlet = new Gitlet();
//				System.out.println(gitlet.currentBranchHead.getTimeStamp());
			} else {
				System.err
						.println("A gitlet version control system already exists in the current directory.");
			}
		}

		else {
			System.err.println("No command with that name exists.");
		}

		try {
			FileOutputStream fileOut = new FileOutputStream(new File(".gitlet",
					"Gitlet.ser"));
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(gitlet);
			out.close();
			fileOut.close();
			System.out.println("Gitlet written in file.");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}