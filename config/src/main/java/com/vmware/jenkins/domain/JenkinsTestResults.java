package com.vmware.jenkins.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.vmware.jenkins.domain.TestResult.TestStatus.FAIL;
import static com.vmware.jenkins.domain.TestResult.TestStatus.FAILED;
import static com.vmware.jenkins.domain.TestResult.TestStatus.FIXED;
import static com.vmware.jenkins.domain.TestResult.TestStatus.PASS;
import static com.vmware.jenkins.domain.TestResult.TestStatus.PASSED;
import static com.vmware.jenkins.domain.TestResult.TestStatus.REGRESSION;
import static com.vmware.jenkins.domain.TestResult.TestStatus.SKIP;
import static java.util.Arrays.stream;

public class JenkinsTestResults extends TestResults {
    public static final String JUNIT_ROOT = "junit/(root)";
    @SerializedName("package")
    public Package[] packages;

    public Suite[] suites;

    public void setBuild(JobBuild build) {
        this.build = build;
    }

    public void addTestResult(TestResult testResult) {
        testResults().add(testResult);
    }

    @Override
    public List<TestResult> testResults() {
        if (loadedTestResults != null) {
            return loadedTestResults;
        }

        loadedTestResults = new ArrayList<>();
        if (packages != null) {
            for (Package pkg : packages) {
                for (Class clazz : pkg.classs) {
                    Map<String, String[]> usedUrls = new HashMap<>();
                    Set<String> usedSkipExceptions = new HashSet<>();
                    for (TestResult testResult : clazz.testResults) {
                        testResult.packagePath = pkg.name;
                        testResult.buildNumber = build.buildNumber;
                        testResult.commitId = build.commitId;
                        testResult.setUrlForTestMethod(build.getTestReportsUIUrl(), usedUrls);
                        addExceptionForSkippedMethodIfNeeded(usedUrls, usedSkipExceptions, testResult, clazz.testResults);
                        loadedTestResults.add(testResult);
                    }
                }
            }
        }
        if (suites != null) {
            for (Suite suite : suites) {
                Map<String, String[]> usedUrls = new HashMap<>();
                Set<String> usedSkipExceptions = new HashSet<>();
                for (TestResult testResult : suite.testResults) {
                    testResult.packagePath = JUNIT_ROOT;
                    testResult.buildNumber = build.buildNumber;
                    testResult.commitId = build.commitId;
                    testResult.setUrlForTestMethod(UrlUtils.addRelativePaths(build.url, "testReport"), usedUrls);
                    if (testResult.skipped) {
                        testResult.status = SKIP;
                    }
                    if (testResult.status == REGRESSION || testResult.status == FAILED) {
                        testResult.status = FAIL;
                    }
                    if (testResult.status == PASSED || testResult.status == FIXED) {
                        testResult.status = PASS;
                    }
                    if (StringUtils.isNotBlank(testResult.errorStackTrace)) {
                        testResult.exception = testResult.errorStackTrace;
                    }
                    addExceptionForSkippedMethodIfNeeded(usedUrls, usedSkipExceptions, testResult, suite.testResults);
                    loadedTestResults.add(testResult);
                }
            }

        }

        return loadedTestResults;
    }

    private void addExceptionForSkippedMethodIfNeeded(Map<String, String[]> usedUrls, Set<String> usedSkipExceptions, TestResult testResult, TestResult[] testResults) {
        if (testResult.status == SKIP && StringUtils.isNotBlank(testResult.exception) && !usedSkipExceptions.contains(testResult.exception)) {
            testResult.similarSkips = Math.toIntExact(
                    stream(testResults).filter(method -> method.status == SKIP
                            && StringUtils.equals(method.exception, testResult.exception)).count() - 1);
            usedSkipExceptions.add(testResult.exception);
        }
        usedUrls.put(testResult.url, testResult.parameters);
    }

    public static class Package {
        public String name;
        public Class[] classs;
        public int fail;
        public int skip;
        public int totalCount;
        public double duration;
    }

    public static class Class {
        public String name;
        public int fail;
        public int skip;
        @SerializedName("test-method")
        public TestResult[] testResults;
        public int totalCount;
        public double duration;
    }

    public static class Suite {
        public String name;
        @SerializedName("cases")
        public TestResult[] testResults;
        public int failCount;
        public int skipCount;
        public int passCount;
        public double duration;
    }

}
