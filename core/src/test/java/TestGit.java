import com.vmware.config.section.CommitConfig;
import com.vmware.util.commandline.scm.FileChange;
import com.vmware.util.commandline.Git;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.RuntimeIOException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vmware.util.commandline.scm.FileChangeType.deleted;
import static com.vmware.util.commandline.scm.FileChangeType.modified;
import static com.vmware.util.commandline.scm.FileChangeType.renamed;
import static java.lang.String.format;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class TestGit {

    private static File workingDirectory;
    private static File testFile;

    private Git git;

    @BeforeClass
    public static void initGitRepo() {
        workingDirectory = createTempDirectory();
        Git git = new Git(workingDirectory);
        git.initRepo();

        testFile = new File(workingDirectory.getAbsolutePath() + File.separator + "testFile.java");
        try {
            testFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }

        git.addAllFiles();
        git.commitWithAllFileChanges("Test commit", false);

        IOUtils.write(testFile, "Sample data");
        git.commitWithAllFileChanges("Second Test commit", false);
    }

    @Before
    public void setWorkingDirectory() {
        git = new Git(workingDirectory);
    }

    @After
    public void resetGitRepo() {
        git.reset("HEAD");
    }

    @Test
    public void parseLastCommit() {
        Map<String, String> values = git.getLastCommitInfo();
        String summary = values.get(Git.SUMMARY);
        assertTrue(StringUtils.isNotEmpty(summary));
    }

    @Test
    public void diffBetweenLastCommit() {
        String first = "";
        String second = "second";
        assertEquals(Stream.of(first, second).filter(StringUtils::isNotBlank).collect(Collectors.joining(",")), "second");
        byte[] diff = git.diffAsByteArray("HEAD~1", "HEAD", false);
        assertNotNull(diff);
        List<FileChange> changes = git.getChangesInDiff("HEAD~1", "HEAD");
        assertFalse(changes.isEmpty());
    }

    @Test
    public void unknownConfigValueIsEmpty() {
        String value = git.configValue("workflow.sdafddfdsffdsfd");
        assertTrue(StringUtils.isEmpty(value));
    }

    @Test
    public void listConfigValues() {
        Map<String, String> values = git.configValues();
        assertFalse(values.isEmpty());
    }

    @Test
    public void diffWithParentRef() {
        String diff = git.diff("HEAD~1", false);
        assertNotNull(diff);
    }

    @Test
    public void detectModifyChange() {
        IOUtils.write(testFile, "Changes to text");

        List<FileChange> allChanges = git.getAllChanges();
        assertEquals("Expected changes", 1, allChanges.size());

        expectStagedChangesAfterAddAll();
        assertEquals(format(modified.getDescription(), testFile.getName()), allChanges.get(0).toString());
    }

    @Test
    public void detectDeleteChange() {
        testFile.delete();

        List<FileChange> allChanges = git.getAllChanges();
        assertEquals("Expected changes", 1, allChanges.size());

        expectStagedChangesAfterAddAll();

        assertEquals(format(deleted.getDescription(), testFile.getName()), allChanges.get(0).toString());
    }

    @Test
    public void detectRenameChange() {
        File renamedFile = new File(workingDirectory.getAbsolutePath() + File.separator + "testFile1.java");

        testFile.renameTo(renamedFile);

        List<FileChange> stagedChanges = expectStagedChangesAfterAddAll();

        assertEquals(format(renamed.getDescription(), testFile.getName(), renamedFile.getName()), stagedChanges.get(0).toString());
    }

    @Test
    public void revParseValidRef() {
        git.revParse("head");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotRevParseInvalidRef() {
        git.revParse("head1");
    }

    @Test
    public void printLast200Commits() {
        int numberOfCommitsToCheck = Math.min(git.totalCommitCount(), 200);
        CommitConfig configuration = new CommitConfig("http://reviewboard",
                "http://gobuild", "http://jenkins", "Testing Done:", "Bug Number:", "Reviewed by:", "Review URL:", "Merge to:",
                new String[] {"main"}, "approvedBy");

        for (int i = 0; i < numberOfCommitsToCheck; i ++) {
            String commitText = git.commitText(i);
            ReviewRequestDraft draft = new ReviewRequestDraft(commitText, configuration);
            System.out.println((i + 1) + "\n" + draft.toText(configuration));
        }
    }

    private List<FileChange> expectStagedChangesAfterAddAll() {
        assertEquals("Expected no staged changes", 0, git.getStagedChanges().size());

        git.addAllFiles();

        List<FileChange> stagedChanges = git.getStagedChanges();
        assertTrue("Expected staged changes after add all", stagedChanges.size() > 0);
        return stagedChanges;
    }

    private static File createTempDirectory() {
        final File tempFile;

        try {
            tempFile = File.createTempFile("temp", "repo");
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }

        tempFile.delete();
        File tempRepoDir = new File(tempFile.getAbsolutePath() + "dir");
        tempRepoDir.mkdir();
        tempRepoDir.deleteOnExit();
        return (tempRepoDir);
    }

}
