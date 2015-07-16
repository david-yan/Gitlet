import java.io.File;

public class Gitlet
{
	public Gitlet()
	{
		
	}
	public static void main(String[] args)
	{
		if(args[0].equals("init"))
		{
			File gitletDir = new File(".gitlet");
			if (!gitletDir.exists())
			{
				gitletDir.mkdir();
				System.out.println("Gitlet directory created.");
			}
		}
	}
}
