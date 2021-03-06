package com.vmware;

import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.NotFoundException;
import com.vmware.reviewboard.domain.ReviewRequestDraft;

import java.util.List;

/**
 * Superclass for common functionality for rest build services such as Jenkins and Buildweb.
 */
public abstract class AbstractRestBuildService extends AbstractRestService {

    protected AbstractRestBuildService(String baseUrl, String apiPath, ApiAuthentication credentialsType, String username) {
        super(baseUrl, apiPath, credentialsType, username);
    }

    public void checkStatusOfBuilds(ReviewRequestDraft draft) {
        log.info("Checking status of builds matching url {}", baseUrl);
        List<JobBuild> jobsToCheck = draft.jobBuildsMatchingUrl(baseUrl);

        if (jobsToCheck.isEmpty()) {
            log.info("No builds found in testing done text");
            updateAllBuildsResultSuccessValue(draft, true);
            return;
        }

        updateAllBuildsResultSuccessValue(draft, checkIfAllBuildsSucceeded(jobsToCheck));
    }

    private boolean checkIfAllBuildsSucceeded(List<JobBuild> jobsToCheck) {
        boolean isSuccess = true;
        for (JobBuild jobBuild : jobsToCheck) {
            String jobUrl = jobBuild.url;

            if (jobBuild.result == BuildResult.BUILDING) {
                try {
                    jobBuild.result = getResultForBuild(jobUrl);
                    log.info("Build: {} Result: {}", jobUrl, jobBuild.result);
                } catch (NotFoundException nfe) {
                    log.info("Build {} could not be found", jobUrl);
                }
            } else {
                log.info("Build: {} Result: {}", jobUrl, jobBuild.result);
            }
            isSuccess = isSuccess && jobBuild.result == BuildResult.SUCCESS;
        }
        return isSuccess;
    }

    protected abstract BuildResult getResultForBuild(String url);

    protected abstract void updateAllBuildsResultSuccessValue(ReviewRequestDraft draft, boolean result);
}
