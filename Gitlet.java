import java.io.*;

public class Gitlet implements Serializable {
	public Gitlet() {
		File gitletDir = new File(".gitlet");
		if (!gitletDir.exists()) {
			gitletDir.mkdir();
			this.commit("initial commit");
		}
	}

	public void commit(String message) {
		System.out.println("test");
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
			in.close();
			fileIn.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		}
		if (args[0].equals("init")) {
			if (!gitletExists) {
				gitlet = new Gitlet();
			} else {
				System.err
						.println("A gitlet version control system already exists in the current directory.");
				return;
			}
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