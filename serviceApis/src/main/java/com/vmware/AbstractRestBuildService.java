package com.vmware;

import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.NotFoundException;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;
import com.vmware.util.collection.CircularFifoQueue;
import com.vmware.util.logging.LogLevel;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import static com.vmware.util.IOUtils.addLines;

/**
 * Superclass for common functionality for rest build services such as Jenkins and Buildweb.
 */
public abstract class AbstractRestBuildService extends AbstractRestService {

    protected AbstractRestBuildService(String baseUrl, String apiPath, ApiAuthentication credentialsType, String username) {
        super(baseUrl, apiPath, credentialsType, username);
    }

    public void checkStatusOfBuilds(ReviewRequestDraft draft) {
        String urlToCheckFor = urlUsedInBuilds();
        log.info("Checking status of builds matching url {}", urlToCheckFor);
        List<JobBuild> jobsToCheck = draft.jobBuildsMatchingUrl(urlToCheckFor);

        if (jobsToCheck.isEmpty()) {
            log.info("No builds found in testing done text");
            updateAllBuildsResultSuccessValue(draft, true);
            return;
        }

        updateAllBuildsResultSuccessValue(draft, checkIfAllBuildsSucceeded(jobsToCheck));
    }

    protected String urlUsedInBuilds() {
        return baseUrl;
    }

    private boolean checkIfAllBuildsSucceeded(List<JobBuild> buildsToCheck) {
        boolean isSuccess = true;
        for (JobBuild jobBuild : buildsToCheck) {
            String jobUrl = jobBuild.url;

            if (jobBuild.status == null
                    || jobBuild.status == BuildStatus.STARTING || jobBuild.status == BuildStatus.BUILDING) {
                try {
                    jobBuild.status = getResultForBuild(jobUrl);
                    log.info("{} {} Result: {}", jobBuild.name, jobUrl, jobBuild.status);
                } catch (NotFoundException nfe) {
                    log.info("{} {} could not be found", jobBuild.name, jobUrl);
                }
            } else {
                log.info("{} {} Result: {}", jobBuild.name, jobUrl, jobBuild.status);
            }
            isSuccess = isSuccess && jobBuild.status == BuildStatus.SUCCESS;
        }
        return isSuccess;
    }

    protected String tail(String url, int numberOfLinesToTail) {
        log.debug("Tailing {} lines using url {}", numberOfLinesToTail, url);
        Queue<String> lines = new CircularFifoQueue<>(numberOfLinesToTail);
        String text = get(url, String.class);
        lines.addAll(Arrays.asList(text.split("\n")));
        return StringUtils.join(lines, "\n");
    }

    protected abstract BuildStatus getResultForBuild(String url);

    protected abstract void updateAllBuildsResultSuccessValue(ReviewRequestDraft draft, boolean result);
}
