import com.vmware.scm.FileChange;
    private List<FileChange> fileChanges;
    private String output;
        output = "";
        fileChanges = new ArrayList<>();
        output = "";
            appendLineToOutput(lineToAdd);
        addDepotInfoToOutput();
    public List<FileChange> getFileChanges() {
        return fileChanges;
    }

    private void addDepotInfoToOutput() {
    private void appendLineToOutput(String lineToAdd) {
            return;