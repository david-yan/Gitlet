import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Class that provides JUnit tests for Gitlet, as well as a couple of utility
 * methods.
 * 
 * 
 */
public class GitletTest
{
	private static final String	GITLET_DIR		= ".gitlet/";
	private static final String	TESTING_DIR		= "test_files/";
	private static final String COMMIT_DIR = ".gitlet//commits//";
	private static final String STAGING_DIR = ".gitlet/staging/";

	/* matches either unix/mac or windows line separators */
	private static final String	LINE_SEPARATOR	= "\r\n|[\r\n]";

	/**
	 * Deletes existing gitlet system, resets the folder that stores files used
	 * in testing.
	 * 
	 * This method runs before every @Test method. This is important to enforce
	 * that all tests are independent and do not interact with one another.
	 */
	@Before
	public void setUp()
	{
		File f = new File(GITLET_DIR);
		if (f.exists())
		{
			try
			{
				recursiveDelete(f);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		f = new File(TESTING_DIR);
		if (f.exists())
		{
			try
			{
				recursiveDelete(f);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		f.mkdirs();
	}

	private final ByteArrayOutputStream	outContent	= new ByteArrayOutputStream();

	@Before
	public void setUpStreams()
	{
		System.setOut(new PrintStream(outContent));
	}

	@After
	public void cleanUpStreams()
	{
		System.setOut(null);
	}

	/**
	 * Tests that init creates a .gitlet directory. Does NOT test that init
	 * creates an initial commit, which is the other functionality of init.
	 */
	@Test
	public void testBasicInitialize()
	{
		gitlet("init");
		File f = new File(GITLET_DIR);
		assertTrue(f.exists());

		assertTrue(new File(COMMIT_DIR + 0).exists());

		gitlet("init");
		assertEquals("A gitlet version control system already exists in the current directory.", outContent.toString().trim());
	}

	/**
	 * Tests that checking out a file name will restore the version of the file
	 * from the previous commit. Involves init, add, commit, and checkout.
	 */
//	@Test
//	public void testBasicCheckout()
//	{
//		String wugFileName = TESTING_DIR + "wug.txt";
//		String wugText = "This is a wug.";
//		createFile(wugFileName, wugText);
//		gitlet("init");
//		gitlet("add", wugFileName);
//		gitlet("commit", "added wug");
//		writeFile(wugFileName, "This is not a wug.");
//		gitlet("checkout", wugFileName);
//		assertEquals(wugText, getText(wugFileName));
//	}

	/**
	 * Tests that log after one commit conforms to the format in the spec.
	 * Involves init, add, commit, and log.
	 */
	@Test
	public void testBasicLog()
	{
		gitlet("init");
		String commitMessage1 = "initial commit";

		String wugFileName = TESTING_DIR + "wug.txt";
		String wugText = "This is a wug.";
		createFile(wugFileName, wugText);
		gitlet("add", wugFileName);
		String commitMessage2 = "added wug";
		gitlet("commit", commitMessage2);

		String logContent = gitlet("log");
		assertArrayEquals(new String[] { commitMessage2, commitMessage1 }, extractCommitMessages(logContent));
	}

	/**
	 * Calls a gitlet command using the terminal.
	 * 
	 * Warning: Gitlet will not print out anything _while_ it runs through this
	 * command, though it will print out things at the end of this command. It
	 * will also return this as a string.
	 * 
	 * The '...' syntax allows you to pass in an arbitrary number of String
	 * arguments, which are packaged into a String[].
	 */
	private static String gitlet(String... args)
	{

		String[] commandLineArgs = new String[args.length + 2];
		commandLineArgs[0] = "java";
		commandLineArgs[1] = "Gitlet";
		for (int i = 0; i < args.length; i++)
		{
			commandLineArgs[i + 2] = args[i];
		}
		String results = command(commandLineArgs);
		System.out.println(results);
		return results.trim();
	}

	/**
	 * Another convenience method for calling Gitlet's main. This calls Gitlet's
	 * main directly, rather than through the terminal. This is slightly
	 * cheating the concept of end-to-end tests. But, this allows you to
	 * actually use the debugger during the tests, which you might find helpful.
	 * It's also a lot faster.
	 * 
	 * Warning: Like the other version of this method, Gitlet will not print out
	 * anything _while_ it runs through this command, though it will print out
	 * things at the end of this command. It will also return what it prints as
	 * a string.
	 */
	private static String gitletFast(String... args)
	{
		PrintStream originalOut = System.out;
		ByteArrayOutputStream printingResults = new ByteArrayOutputStream();
		try
		{
			/*
			 * Below we change System.out, so that when you call
			 * System.out.println(), it won't print to the screen, but will
			 * instead be added to the printingResults object.
			 */
			System.setOut(new PrintStream(printingResults));
			Gitlet.main(args);
		} finally
		{
			/*
			 * Restores System.out (So you can print normally).
			 */
			System.setOut(originalOut);
		}
		System.out.println(printingResults.toString());
		return printingResults.toString().trim();
	}

	/**
	 * Returns the text from a standard text file.
	 */
	public static String getText(String fileName)
	{
		try
		{
			byte[] encoded = Files.readAllBytes(Paths.get(fileName));
			return new String(encoded, StandardCharsets.UTF_8);
		} catch (IOException e)
		{
			return "";
		}
	}
	
	/**
	 * Creates a new file with the given fileName and gives it the text
	 * fileText.
	 */
	public static void createFile(String fileName, String fileText)
	{
		File f = new File(fileName);
		if (!f.exists())
		{
			try
			{
				f.createNewFile();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		writeFile(fileName, fileText);
	}

	/**
	 * Replaces all text in the existing file with the given text.
	 */
	public static void writeFile(String fileName, String fileText)
	{
		FileWriter fw = null;
		try
		{
			File f = new File(fileName);
			fw = new FileWriter(f, false);
			fw.write(fileText);
		} catch (IOException e)
		{
			e.printStackTrace();
		} finally
		{
			try
			{
				fw.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Deletes the file and all files inside it, if it is a directory.
	 * 
	 * @throws IOException
	 */
	public static void recursiveDelete(File d) throws IOException
	{
		if (d.isDirectory())
		{
			for (File f : d.listFiles())
			{
				recursiveDelete(f);
			}
		}
		if (!d.delete())
		{
			throw new IOException("Failed to delete file " + d.getPath());
		}
	}

	/**
	 * Returns an array of commit messages associated with what log has printed
	 * out.
	 */
	private static String[] extractCommitMessages(String logOutput)
	{
		String[] logChunks = logOutput.split("===");
		int numMessages = logChunks.length - 1;
		String[] messages = new String[numMessages];
		for (int i = 0; i < numMessages; i++)
		{
			String[] logLines = logChunks[i + 1].split(LINE_SEPARATOR);
			messages[i] = logLines[3];
		}
		return messages;
	}

	/**
	 * Executes the given command on the terminal, and return what it prints out
	 * as a string.
	 * 
	 * Example: If you want to call the terminal command `java Gitlet add
	 * wug.txt` you would call the method like so: `command("java", "Gitlet",
	 * "add", "wug.txt");` The `...` syntax allows you to pass in however many
	 * strings you want.
	 */
	private static String command(String... args)
	{
		try
		{
			StringBuilder results = new StringBuilder();
			Process p = Runtime.getRuntime().exec(args);
			p.waitFor();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));)
			{
				String line = null;
				while ((line = br.readLine()) != null)
				{
					results.append(line).append(System.lineSeparator());
				}
				return results.toString();
			}
		} catch (IOException e)
		{
			return e.getMessage();
		} catch (InterruptedException e)
		{
			return e.getMessage();
		}
	}

//	@Test
//	public void testCommit()
//	{
//		gitlet("init");
//		String firstCommit = "commit 1";
//		String commitText = "This is the first commit";
//		String commitFileName = TESTING_DIR + "first_commit.txt";
//		createFile(commitFileName, commitText);
//		gitlet("add", commitFileName);
//		gitlet("commit", firstCommit);
//		String logOutput = gitlet("log");
//		assertArrayEquals(new String[] {firstCommit, "initial commit"}, extractCommitMessages(logOutput));
//		String secondCommit = "commit 2";
//		String commit2Text = "This is the second commit";
//		String commit2FileName = TESTING_DIR + "second_commit.txt";
//		assertTrue(new File(COMMIT_DIR + "1//first_commit.txt").exists());
//		gitlet("add", COMMIT_DIR + "1//first_commit.txt");
//		writeFile(STAGING_DIR + "first_commit.txt", commit2Text);
//		createFile(commit2FileName, commit2Text);
//		gitlet("add", commit2FileName);
//		gitlet("commit", secondCommit);
//		logOutput = gitlet("log");
//		assertArrayEquals(new String[] {secondCommit, firstCommit, "initial commit"}, extractCommitMessages(logOutput));
//		assertTrue(new File(COMMIT_DIR + "2//first_commit.txt").exists());
//		assertTrue(new File(COMMIT_DIR + "2//second_commit.txt").exists());
//		assertEquals(getText(COMMIT_DIR + "2//second_commit.txt"), commit2Text);
//		assertEquals(getText(COMMIT_DIR + "2//first_commit.txt"), commit2Text);
//		assertEquals(getText(COMMIT_DIR + "1//first_commit.txt"), commitText);
//	}
//	
//	//@Test
//	public void testFileSystem()
//	{
//		Gitlet gitlet = new Gitlet();
//		String firstCommit = "commit 1";
//		String commitText = "This is the first commit";
//		String commitFileName = TESTING_DIR + "first_commit.txt";
//		createFile(commitFileName, commitText);
//		gitlet.add(commitFileName);
//		gitlet.commit(firstCommit);
//		gitlet.add(COMMIT_DIR + "1//first_commit.txt");
//		String commit2Text = "This is the second commit";
//		writeFile(STAGING_DIR + "first_commit.txt", commit2Text);
//		gitlet.commit("commit 2");
//		String commit3FileName = TESTING_DIR + "second_commit.txt";
//		String commit3Text = "This is the third commit";
//		createFile(commit3FileName, commit3Text);
//		gitlet.add(commit3FileName);
//		gitlet.commit("commit 3");
//		assertTrue(gitlet.getBranches().containsKey("master"));
//		assertEquals(gitlet.getBranches().get("master").getMessage(), "commit 3");
//		assertEquals(gitlet.getBranches().get("master").getPrevCommit().getMessage(), "commit 2");
//		assertEquals(gitlet.getBranches().get("master").getPrevCommit().getPrevCommit().getMessage(), "commit 1");
//		assertTrue(gitlet.getBranches().get("master").getFile("second_commit.txt").exists());
//		assertTrue(gitlet.getBranches().get("master").getFile("first_commit.txt").exists());
//		assertEquals(commit2Text, getText(gitlet.getBranches().get("master").getFile("first_commit.txt").getPath()));
//	}
	
	@Test
	public void testBranch()
	{
		Gitlet gitlet = new Gitlet();
		String firstCommit = "commit 1";
		String commitText = "This is the first commit";
		String commitFileName = TESTING_DIR + "first_commit.txt";
		createFile(commitFileName, commitText);
		gitlet.add(commitFileName);
		gitlet.commit(firstCommit);
		gitlet.branch("branch");
		assertEquals(gitlet.getCurrentBranch(), "master");
		assertTrue(gitlet.getBranches().containsKey("branch"));
		assertEquals(gitlet.getBranches().get("master"), gitlet.getBranches().get("branch"));
		try
		{
			gitlet.checkout("branch");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		assertEquals(gitlet.getCurrentBranch(), "branch");
		assertTrue(new File("first_commit.txt").exists());
		String commit2Text = "This is the second commit";
		createFile(TESTING_DIR + "second_commit.txt", commit2Text);
		gitlet.add(TESTING_DIR + "second_commit.txt");
		gitlet.commit("commit 2");
		
	}
}