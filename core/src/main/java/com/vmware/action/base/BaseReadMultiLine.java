package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.util.ReflectionUtils;
import com.vmware.util.input.InputUtils;


public abstract class BaseReadMultiLine extends BaseCommitReadAction {

    private final boolean append;

    private final String[] historyValues;

    public BaseReadMultiLine(WorkflowConfig config, String propertyName, boolean append, String... historyValues) {
        super(config, propertyName);
        this.append = append;
        this.historyValues = historyValues;
    }

    @Override
    public void process() {
        String propertyValue = (String) ReflectionUtils.getValue(property, draft);
        if (!propertyValue.isEmpty()) {
            log.info("Existing value for section {}{}{}", property.getName(), System.lineSeparator(), propertyValue);
        }
        String titleToDisplay = propertyValue.isEmpty() || !append ? title : "Additional " + title;
        if (append) {
            propertyValue += System.lineSeparator() + InputUtils.readData(titleToDisplay, false, commitConfig.maxDescriptionLength);
        } else {
            propertyValue = InputUtils.readData(titleToDisplay, false, commitConfig.maxDescriptionLength, historyValues);
        }
        ReflectionUtils.setValue(property, draft, propertyValue);
    }
}
