import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;


public class GitletNodeTest
{

	@Test
	public void testGetContents() throws IOException
	{
		GitletNode node1 = new GitletNode("1", 0, null);
		Gitlet.copyFileUsingFileChannels(File.createTempFile("file1", ".txt"), new File(node1.getFolder(), "file1.txt"));
		GitletNode node2 = new GitletNode("2", 1, node1);
		Gitlet.copyFileUsingFileChannels(File.createTempFile("file2", ".txt"), new File(node2.getFolder(), "file2.txt"));
		node2.addFile("file1.txt");
		assertTrue(new File(node1.getFolder(), "file1.txt").exists());
		assertTrue(new File(node2.getFolder(), "file2.txt").exists());
	}

}
