import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
	@Test
	public void testBasicCheckout()
	{
		String wugFileName = TESTING_DIR + "wug.txt";
		String wugText = "This is a wug.";
		createFile(wugFileName, wugText);
		gitlet("init");
		gitlet("add", wugFileName);
		gitlet("commit", "added wug");
		writeFile(wugFileName, "This is not a wug.");
		gitlet("checkout", wugFileName);
		assertEquals(wugText, getText(wugFileName));
	}

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
	private static String getText(String fileName)
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
	private static void createFile(String fileName, String fileText)
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
	private static void writeFile(String fileName, String fileText)
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
	private static void recursiveDelete(File d) throws IOException
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

	@Test
	public void testCommit()
	{
		gitlet("init");
		String firstCommit = "commit 1";
		String commitText = "This is the first commit";
		String commitFileName = TESTING_DIR + "first_commit.txt";
		createFile(commitFileName, commitText);
		gitlet("add", commitFileName);
		gitlet("commit", firstCommit);
		String logOutput = gitlet("log");
		assertArrayEquals(new String[] {firstCommit, "initial commit"}, extractCommitMessages(logOutput));
		String secondCommit = "commit 2";
		String commit2Text = "This is the second commit";
		String commit2FileName = TESTING_DIR + "second_commit.txt";
		assertTrue(new File(COMMIT_DIR + "1//first_commit.txt").exists());
		gitlet("add", COMMIT_DIR + "1//first_commit.txt");
		writeFile(STAGING_DIR + "first_commit.txt", commit2Text);
		createFile(commit2FileName, commit2Text);
		gitlet("add", commit2FileName);
		gitlet("commit", secondCommit);
		logOutput = gitlet("log");
		assertArrayEquals(new String[] {secondCommit, firstCommit, "initial commit"}, extractCommitMessages(logOutput));
		assertTrue(new File(COMMIT_DIR + "2//first_commit.txt").exists());
		assertTrue(new File(COMMIT_DIR + "2//second_commit.txt").exists());
		assertEquals(getText(COMMIT_DIR + "2//second_commit.txt"), commit2Text);
		assertEquals(getText(COMMIT_DIR + "2//first_commit.txt"), commit2Text);
		assertEquals(getText(COMMIT_DIR + "1//first_commit.txt"), commitText);
	}
	
	@Test
	public void testRebase() {
		gitlet("init");
		String firstCommit = "commit 1";
		String commitText = "This is the first commit";
		String commitFileName = TESTING_DIR + "first_commit.txt";
		createFile(commitFileName, commitText);
		String randomFile = TESTING_DIR + "random.txt";
		createFile(randomFile, "this is random");
		gitlet("add", randomFile);
		gitlet("add", commitFileName);
		gitlet("commit", firstCommit);
		
		assertEquals("", gitlet("branch", "branch"));
		
		writeFile(commitFileName, "first commit revised");
		gitlet("add", commitFileName);
		
		gitlet("commit", "commit 2 text modified and random added");

		
		assertEquals("", gitlet("checkout", "branch"));

		String thirdCommit = "commit 3";
		String commit3Text = "This is the third commit";
		String commit3FileName = TESTING_DIR + "third_commit.txt";
		createFile(commit3FileName, commit3Text);

		gitlet("add", commit3FileName);
		gitlet("commit", thirdCommit);

		String randomFile2 = TESTING_DIR + "random2.txt";
		createFile(randomFile2, "this is random #2");
		gitlet("add", randomFile2);
		gitlet("commit", "commit# 4 random2.txt added");
		
		assertEquals("", gitlet("rebase", "master"));

	}

	@Test
	public void testBranch() {
		gitlet("init");
		String firstCommit = "commit 1";
		String commitText = "This is the first commit";
		String commitFileName = TESTING_DIR + "first_commit.txt";
		createFile(commitFileName, commitText);
		gitlet("add", commitFileName);
		gitlet("commit", firstCommit);
		String logOutput = gitlet("log");
		assertArrayEquals(new String[] { firstCommit, "initial commit" },
				extractCommitMessages(logOutput));
		// test to see if the branch name we're trying to create already exist
		assertEquals("A branch with that name already exists.",
				gitlet("branch", "master"));
		gitlet("branch", "branch1");
		String secondCommit = "commit 2";
		String commit2Text = "This is the second commit";
		String commit2FileName = TESTING_DIR + "second_commit.txt";
		createFile(commit2FileName, commit2Text);
		gitlet("add", commit2FileName);
		gitlet("commit", secondCommit);
		logOutput = gitlet("log");
		assertArrayEquals(new String[] { secondCommit, firstCommit,
				"initial commit" }, extractCommitMessages(logOutput));
		// test branch doesn't exist, should also print that file doesn't
		// exist
		assertEquals(
				"File does not exist in the most recent commit, or no such branch exists.",
				gitlet("checkout", "branch2"));
		gitlet("checkout", "branch1");
		String thirdCommit = "commit 3";
		String commit3Text = "This is the third commit";
		String commit3FileName = TESTING_DIR + "third_commit.txt";
		createFile(commit3FileName, commit3Text);
		gitlet("add", commit3FileName);
		gitlet("commit", thirdCommit);
		logOutput = gitletFast("log");
		assertArrayEquals(new String[] { thirdCommit, firstCommit,
				"initial commit" }, extractCommitMessages(logOutput));

	}
	
	@Test
	public void testRemove(){
		String fileName = TESTING_DIR + "test.txt";
		String testFile = "Hello world, testing Gitlet";
		createFile(fileName, testFile);
		gitletFast("init");
		
		outContent.reset();
		gitletFast("rm", "test.txt");
		assertEquals(outContent.toString().trim(), "No reason to remove the file.");
		
		File f = new File(GITLET_DIR + "staging/" + fileName);
		gitletFast("add", fileName);
		assertTrue(f.exists());
		gitletFast("rm", fileName);
		assertFalse(f.exists());
		gitletFast("add", fileName);
		gitletFast("commit", "added test.txt");
		f = new File(GITLET_DIR + "1/test.txt");
		assertTrue(f.exists());
		gitlet("rm", "test.txt");
		gitlet("commit", "removed test.txt");
		File commit2 = new File(GITLET_DIR + "2");
		assertEquals(commit2.list().length, 0);
		assertTrue(f.exists());
	}
	
	@Test
	public void testBranch(){
		String fileName = TESTING_DIR + "test.txt";
		String testFile = "Hello world, testing Gitlet";
		createFile(fileName, testFile);
		gitlet("init");
		gitlet("add", fileName);
		gitlet("commit", "added test.txt");
		
		writeFile(fileName, "Hey world, wassup");
		gitlet("add", fileName);
		gitlet("commit", "updated test.txt");
		gitlet("branch", "new-world");
		gitlet("checkout", "new-world");
		
		writeFile(fileName, "A whole new world...");
		gitlet("add", fileName);
		gitlet("commit", "new world begins");
		gitlet("checkout", "master");
	
		writeFile(fileName, "back to the old world...");
		gitlet("add", fileName);
		gitlet("commit", "updates on updates");
		String logContent = gitlet("log");
		assertArrayEquals(new String[] {"updates on updates", "updated test.txt", "added test.txt", "initial commit"}, extractCommitMessages(logContent));
		
	}
	
	/**
	 * Tests commit functionalities and error cases. Makes sure the staging area
	 * is empty after a commit.
	 */
	@Test
	public void testCommit(){
		String fileName = TESTING_DIR + "test.txt";
		String testFile = "Hello world, testing Gitlet";
		createFile(fileName, testFile);
		gitlet("init");
		gitlet("add", fileName);
		
		outContent.reset();
		gitlet("commit");
		assertEquals(outContent.toString().trim(), "Please enter a commit message.");
		
		gitlet("commit", "added test.txt");
		File f = new File(GITLET_DIR + "commits/1/");
		assertTrue(f.exists());
		
		File staging = new File(GITLET_DIR + "staging/");
		assertEquals(staging.list().length, 0);
		
		outContent.reset();
		gitlet("commit", "shouldn't work");
		assertEquals(outContent.toString().trim(), "No changes added to the commit.");
	}
	
	/**
	 * Tests add, making sure that the file is added to the staging folder. Also checks if an 
	 * error message is printed if there is no file with the name
	 */
	@Test
	public void testAdd(){
		String fileName = TESTING_DIR + "test.txt";
		String testFile = "Hello world, testing Gitlet";
		createFile(fileName, testFile);
		gitlet("init");
		gitlet("add", fileName);
		File f = new File(GITLET_DIR + "staging/" + fileName);
		assertTrue(f.exists());
		
		outContent.reset();
		gitlet("add", "doesnotexist.txt");
		assertEquals(outContent.toString().trim(), "File does not exist.");
	}
}
