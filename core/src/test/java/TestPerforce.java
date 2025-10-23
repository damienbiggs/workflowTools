import com.vmware.http.HttpConnection;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.util.commandline.scm.FileChange;
import com.vmware.util.commandline.Perforce;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestPerforce {

    private Perforce perforce = new Perforce("dbiggs-vcloud-sp-main", null);

    @Test
    public void bustLoadBalancer() {
        IntStream.range(0, 10000).parallel().forEach(i -> {
            HttpConnection connection = new HttpConnection(RequestBodyHandling.AsMultiPartFormEntity);
            String pageText = connection.get("http://10.150.174.208/index.html", String.class);
            System.out.println(i + " " + pageText);
        });

    }

    @Test
    public void canDetermineRootDirectory() {
        File clientDirectory = perforce.getWorkingDirectory();
        assertNotNull(clientDirectory);
        assertEquals("/Users/dbiggs/p4-sp-main", clientDirectory.getPath());
    }

    @Test
    public void canDetermineClientName() {
        perforce = new Perforce("/Users/dbiggs/p4-sp-main/");
        assertTrue(perforce.isLoggedIn());
        assertEquals("dbiggs", perforce.getUsername());
        assertEquals("dbiggs-vcloud-sp-main", perforce.getClientName());
    }

    @Test
    public void changelistIsSubmitted() {
        assertEquals("submitted", perforce.getChangelistStatus("448453"));
    }

    @Test
    public void canGetFileChangesInChangelist() {
        List<String> openChangelists = perforce.getPendingChangelists();
        assertFalse("Need a pending changelist to run this test", openChangelists.isEmpty());
        List<FileChange> changes = perforce.getFileChangesForPendingChangelist(openChangelists.get(0));
        assertFalse("Should not be empty", changes.isEmpty());
    }
}
