import java.io.*;

public class Test implements Serializable
{
	private String	message;
	public Test()
	{
		message = "DEFAULT";
		createDirectory();
	}
	public Test(String message)
	{
		this.message = message;
		createDirectory();
	}
	public String message()
	{
		return message;
	}
	private void createDirectory()
	{
		File file = new File("test");
		if (!file.exists())
		{
			try
			{
				file.mkdir();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	public static void main(String[] args)
	{
		if (args[0].equals("fetch"))
		{
			try
			{
				Test test;
				FileInputStream fileIn = new FileInputStream(new File("test//Test.ser"));
				ObjectInputStream in = new ObjectInputStream(fileIn);
				test = (Test) in.readObject();
				in.close();
				fileIn.close();
				System.out.println(test.message);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		if (args[0].equals("init"))
		{
			try
			{
				Test test = new Test(args[1]);
				FileOutputStream fileOut = new FileOutputStream(new File("test//Test.ser"));
				ObjectOutputStream out = new ObjectOutputStream(fileOut);
				out.writeObject(test);
				out.close();
				fileOut.close();
				System.out.println("Test written in file.");
			} catch (Exception e)
			{
				e.printStackTrace();;
			}
		}
		if (args[0].equals("test"))
		{
			new File("test//staging").mkdir();
		}
	}
}
