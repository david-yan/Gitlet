import java.io.*;
import java.nio.file.Files;

public class Gitlet implements Serializable
{
	public Gitlet()
	{
		File gitletDir = new File(".gilet");
		if (!gitletDir.exists())
		{
			gitletDir.mkdir();
			this.commit("initial commit");
		}
		new File(".gitlet//staging").mkdir();
		new File(".gitlet//old_files").mkdir();
	}
	public void commit(String message)
	{
		File staging = new File(".gitlet//staging");
		File destination = new File(".gitlet//oldFiles//backup1");
	}
	public static void main(String[] args)
	{
		Gitlet gitlet = null;
		boolean gitletExists = false;
		try
		{
			FileInputStream fileIn = new FileInputStream(new File(".gitlet//Gitlet.ser"));
			ObjectInputStream in = new ObjectInputStream(fileIn);
			gitlet = (Gitlet) in.readObject();
			gitletExists = true;
			in.close();
			fileIn.close();
		} catch (Exception e){}
		if (args[0].equals("init"))
		{
			if (!gitletExists)
			{
				gitlet = new Gitlet();
			} else
				System.err.println("A gitlet version control system already exists in the current directory.");
		}
		try
		{
			FileOutputStream fileOut = new FileOutputStream(new File(".gitlet//Gitlet.ser"));
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(gitlet);
			out.close();
			fileOut.close();
			System.out.println("Gitlet written in file.");
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
