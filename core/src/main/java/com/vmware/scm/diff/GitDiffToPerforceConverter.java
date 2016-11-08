import com.vmware.scm.FileChangeType;
import com.vmware.scm.ScmType;
import com.vmware.util.IOUtils;
import java.io.File;
import static com.vmware.scm.ScmType.git;
        FileChange fileChange = null;
            fileChange = new FileChange(git, FileChangeType.added, addFile);
            fileChange = new FileChange(git, FileChangeType.deleted, lastDiffFile);
            fileChange = new FileChange(git, FileChangeType.modified, addDiffFile);
        if (fileChange != null) {
            fileChanges.add(fileChange);
        }
            fileChanges.add(new FileChange(git, FileChangeType.renamed, renameFromFile, renameToFile));
            fileChanges.add(new FileChange(git, FileChangeType.renamedAndModified, renameFromFile, renameToFile));
                String fileVersion = StringUtils.isNotBlank(lastSubmittedChangelist) ? "@" + lastSubmittedChangelist : "";
                filesListToCheck += format("%s/%s%s", perforce.getWorkingDirectory(), depotFileToCheck, fileVersion);

    public static void main(String[] args) {
        String diff = IOUtils.read(new File("/Users/dbiggs/Downloads/rb1030085.patch"));
        PerforceDiffToGitConverter converter = new PerforceDiffToGitConverter();
        String diffText = converter.convert(diff);
        System.out.println(diffText);

    }