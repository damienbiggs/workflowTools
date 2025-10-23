package com.vmware.util.commandline.scm;

import java.util.List;

public interface DiffConverter {

    String convert(String diffData);

    byte[] convertAsBytes(String diffData);

    List<FileChange> getFileChanges();
}
