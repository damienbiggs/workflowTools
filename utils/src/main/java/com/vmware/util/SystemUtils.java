package com.vmware.util;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;

import com.vmware.util.commandline.CommandLineUtils;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.logging.LogLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemUtils {

    private static final Logger log = LoggerFactory.getLogger(SystemUtils.class);

    public static void openUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            log.error("Not opening url as it is blank");
            return;
        }

        log.info("Opening url {}", url);
        if (CommandLineUtils.isOsxCommandAvailable("open")) {
            log.debug("Opening url using osx open command");
            CommandLineUtils.executeCommand(null, "open " + url, null, true, LogLevel.DEBUG);
        } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            log.debug("Opening url using java desktop support");
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        } else {
            log.error("Cannot open url {} as BROWSER action for Java desktop is not supported", url);
        }
    }

    public static void copyTextToClipboard(String text) {
        if (CommandLineUtils.isOsxCommandAvailable("pbcopy")) {
            log.trace("Using osx pbcopy command to copy text to clipboard as it doesn't cause terminal in full screen mode to jump back to the desktop view");
            CommandLineUtils.executeCommand(null, "pbcopy", text, true, LogLevel.DEBUG);
        } else {
            log.debug("Using Java clipboard support to copy text");
            StringSelection stringSelection = new StringSelection(text);
            Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbrd.setContents(stringSelection, null);
        }
    }

    public static boolean postgresSchemaExists(String schemaName) {
        if (!CommandLineUtils.isCommandAvailable("psql") || StringUtils.isEmpty(schemaName)) {
            return false;
        }
        String sqlCommand = "select exists(SELECT datname FROM pg_catalog.pg_database WHERE datname = '" + schemaName + "')";
        String command = String.format("psql -t -c \"%s\"", sqlCommand);
        String output = CommandLineUtils.executeCommand(command, LogLevel.DEBUG);
        return "t".equals(StringUtils.trim(output));
    }
}
