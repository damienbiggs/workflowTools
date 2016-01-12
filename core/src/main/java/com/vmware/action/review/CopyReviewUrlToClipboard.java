package com.vmware.action.review;

import com.vmware.action.base.BaseCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.IOUtils;
import com.vmware.utils.UrlUtils;
import com.vmware.utils.exceptions.RuntimeIOException;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Locale;

@ActionDescription("Copies the review board url to the clipboard. Handy for pasting it into a browser.")
public class CopyReviewUrlToClipboard extends BaseCommitWithReviewAction {
    public CopyReviewUrlToClipboard(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() {
        // don't need to load reviewboard for this action
    }


    @Override
    public void process() {
        String reviewUrl = String.format("%sr/%s/", UrlUtils.addTrailingSlash(config.reviewboardUrl), draft.id);

        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        boolean isOsx = osName.contains("mac") || osName.contains("darwin");

        if (isOsx) {
            try {
                copyUsingPbcopyCommand(reviewUrl);
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        } else {
            StringSelection stringSelection = new StringSelection(reviewUrl);
            Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbrd.setContents(stringSelection, null);
        }
        log.info("Copied review url to clipboard");
    }

    private void copyUsingPbcopyCommand(String reviewUrl) throws IOException {
        log.debug("Using pbcopy command to copy review url to clipboard as it doesn't cause terminal in full screen mode to jump back to the desktop view");
        ProcessBuilder builder = new ProcessBuilder("pbcopy").redirectErrorStream(true);
        Process statusProcess = builder.start();
        IOUtils.write(statusProcess.getOutputStream(), reviewUrl);
        String output = IOUtils.read(statusProcess.getInputStream());
        log.debug(output);
    }
}
