package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;
import com.vmware.util.ArrayUtils;
import com.vmware.util.CollectionUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.db.AfterDbLoad;
import com.vmware.util.db.BaseDbClass;
import com.vmware.util.db.DbSaveIgnore;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.vmware.jenkins.domain.TestResult.TestStatusOnBuildRemoval.DELETEABLE;
import static com.vmware.jenkins.domain.TestResult.TestStatusOnBuildRemoval.UPDATEABLE;
import static com.vmware.jenkins.domain.TestResult.TestStatusOnBuildRemoval.CONTAINS_BUILD;
import static com.vmware.jenkins.domain.TestResult.TestStatusOnBuildRemoval.NO_UPDATE_NEEDED;
import static com.vmware.jenkins.domain.TestResults.JUNIT_ROOT;

public class TestResult extends BaseDbClass {

    private static final String SUSPECTS_TITLE = "Commits between last pass and first failure, guilty until proven innocent";
    private static final String LINK_IN_NEW_TAB = "target=\"_blank\" rel=\"noopener noreferrer\"";
    private static final SimpleDateFormat START_DAY_FORMATTER = new SimpleDateFormat("MMM dd");
    public String name;
    @Expose(serialize = false, deserialize = false)
    public Long jobBuildId;

    public TestStatus status;

    @DbSaveIgnore
    public boolean skipped;

    public String packagePath;
    public String className;
    public String exception;

    @DbSaveIgnore
    public String errorStackTrace;

    public double duration;
    public long startedAt;
    public String[] parameters;
    @Expose(serialize = false, deserialize = false)
    public Integer dataProviderIndex;

    public Integer similarSkips;

    @Expose(serialize = false, deserialize = false)
    public Integer[] failedBuilds;

    @Expose(serialize = false, deserialize = false)
    public Integer[] skippedBuilds;

    @Expose(serialize = false, deserialize = false)
    public Integer[] passedBuilds;

    @Expose(serialize = false, deserialize = false)
    public Integer[] presumedPassedBuilds;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    public String url;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    public int buildNumber;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    public String commitId;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    public List<TestResult> testRuns;

    public TestResult() {
    }

    public TestResult(TestResult methodToClone, JobBuild build, TestStatus status) {
        this.name = methodToClone.name;
        this.buildNumber = build.buildNumber;
        this.url = methodToClone.url.replace(String.valueOf(methodToClone.buildNumber), String.valueOf(build.buildNumber));
        this.packagePath = methodToClone.packagePath;
        this.className = methodToClone.className;
        this.parameters = methodToClone.parameters;
        this.jobBuildId = methodToClone.jobBuildId;
        this.status = status;
        this.commitId = build.commitId;
        this.startedAt = build.buildTimestamp;
    }

    @AfterDbLoad
    public void sortArrays() {
        Stream.of(passedBuilds, presumedPassedBuilds, skippedBuilds, failedBuilds).filter(Objects::nonNull).forEach(Arrays::sort);
    }

    public String fullTestNameWithoutParameters() {
        return packagePath + "." + className + "." + name;
    }

    public String fullTestNameWithPackage() {
        return packagePath + "." + classAndTestName();
    }

    public long getStartedAt() {
        return startedAt;
    }

    public String fullTestNameWithExceptionInfo() {
        if (StringUtils.isEmpty(exception)) {
            return fullTestNameWithPackage() + " " + status;
        } else {
            return fullTestNameWithPackage() + " " + status + System.lineSeparator() + exception + System.lineSeparator();
        }
    }

    public String fullTestNameForDisplay() {
        String fullTestName = JUNIT_ROOT.equals(packagePath) ? name : classAndTestName();
        if (status == TestStatus.SKIP && similarSkips != null && similarSkips > 0) {
            fullTestName = fullTestName + " (" + StringUtils.pluralize(similarSkips, "similar skip") + ")";
        }
        return fullTestName;
    }

    public String classAndTestName() {
        String testName = className + "." + name;
        if (parameters != null && parameters.length > 0) {
            String testParams = String.join(",", parameters);
            testName += " (" + testParams + ")";
        }
        return testName;
    }

    public String fullPackageAndTestName() {
        if (JUNIT_ROOT.equals(packagePath)) {
            return className + "." + name;
        }
        return packagePath + "." + className + "." + name;
    }

    public void setUrlForTestMethod(String uiUrl, Set<String> usedUrls) {
        String nameToUseForUrl = name;
        String classNameToUseForUrl = className;
        if (packagePath.equals(JUNIT_ROOT)) {
            classNameToUseForUrl = className.replace(" ", "%20").replace("\"", "%22");
            nameToUseForUrl = name.replace(" ", "_").replace("\"", "_");
        }
        if (dataProviderIndex != null) {
            this.url = UrlUtils.addRelativePaths(uiUrl, packagePath, classNameToUseForUrl, nameToUseForUrl + "_" + dataProviderIndex);
            return;
        }

        String urlToUse = UrlUtils.addRelativePaths(uiUrl, packagePath, classNameToUseForUrl, nameToUseForUrl);
        if (!usedUrls.contains(urlToUse)) {
            this.url = urlToUse;
            return;
        }
        int counter = 0;
        while (usedUrls.contains(urlToUse)) {
            urlToUse = UrlUtils.addRelativePaths(uiUrl, packagePath, classNameToUseForUrl, nameToUseForUrl + "_" + ++counter);
        }
        this.dataProviderIndex = counter;
        this.url = urlToUse;
    }

    public String testLinks(String commitComparisonUrl) {
        List<TestResult> sortedTestRuns = testRuns.stream().sorted(Comparator.comparing(TestResult::buildNumber)).collect(Collectors.toList());
        String resultLinks = sortedTestRuns.stream().map(this::testResultLink).collect(Collectors.joining(System.lineSeparator()));

        sortedTestRuns.sort(Comparator.comparing(TestResult::buildNumber).reversed());
        Optional<TestResult> lastPassingResult =
                IntStream.range(1, sortedTestRuns.size()).mapToObj(sortedTestRuns::get).filter(method -> TestStatus.isPass(method.status)).findFirst();

        if (lastPassingResult.isPresent() && StringUtils.isNotBlank(commitComparisonUrl) && StringUtils.isNotBlank(lastPassingResult.get().commitId)) {
            TestResult firstFailure = sortedTestRuns.get(sortedTestRuns.indexOf(lastPassingResult.get()) - 1);
            String suspectsUrl = commitComparisonUrl.replace("(first)", lastPassingResult.get().commitId)
                    .replace("(second)", firstFailure.commitId);

            String suspectsLink = String.format("<a class =\"suspects\" href=\"%s\" title=\"%s\" %s>SUSPECTS</a>",
                    suspectsUrl, SUSPECTS_TITLE, LINK_IN_NEW_TAB);
            return suspectsLink + System.lineSeparator() + resultLinks;
        } else {
            return resultLinks;
        }
    }

    public boolean containsBuildNumbers(int... buildNumber) {
        return Stream.of(passedBuilds, presumedPassedBuilds, skippedBuilds, failedBuilds).anyMatch(values -> containsBuildNumbers(values, buildNumber));
    }

    public List<Map.Entry<Integer, TestStatus>> buildsToUse(int limit) {
        Map<Integer, TestStatus> testStatusMap = new HashMap<>();
        Optional.ofNullable(passedBuilds).ifPresent(values -> Arrays.stream(values).forEach(value -> testStatusMap.put(value, TestStatus.PASS)));
        Optional.ofNullable(presumedPassedBuilds).ifPresent(values -> Arrays.stream(values).forEach(value -> testStatusMap.put(value, TestStatus.PRESUMED_PASS)));
        Optional.ofNullable(failedBuilds).ifPresent(values -> Arrays.stream(values).forEach(value -> testStatusMap.put(value, TestStatus.FAIL)));
        Optional.ofNullable(skippedBuilds).ifPresent(values -> Arrays.stream(values).forEach(value -> testStatusMap.put(value, TestStatus.SKIP)));

        Comparator<Map.Entry<Integer, TestStatus>> keyComparator = Map.Entry.<Integer, TestStatus>comparingByKey().reversed();

        List<Map.Entry<Integer, TestStatus>> buildsToUse = testStatusMap.entrySet().stream().sorted(keyComparator).limit(limit).collect(Collectors.toList());

        List<Map.Entry<Integer, TestStatus>> passingBuilds = testStatusMap.entrySet().stream().filter(entry -> TestStatus.isPass(entry.getValue()))
                .sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());

        if (buildsToUse.stream().noneMatch(entry -> TestStatus.isPass(entry.getValue())) && CollectionUtils.isNotEmpty(passingBuilds)) {
            Map.Entry<Integer, TestStatus> lastPassingBuild = passingBuilds.get(passingBuilds.size() - 1);
            buildsToUse.remove(buildsToUse.size() - 1);
            buildsToUse.add(lastPassingBuild);
            // get next failing / skipped test
            for (int i = lastPassingBuild.getKey() + 1; i < buildNumber; i ++) {
                if (testStatusMap.containsKey(i) && testStatusMap.get(i) == TestStatus.FAIL) {
                    buildsToUse.remove(buildsToUse.size() - 2); // current build after last passing build
                    buildsToUse.add(new AbstractMap.SimpleEntry<>(i, testStatusMap.get(i)));
                    return buildsToUse;
                }
            }

            for (int i = lastPassingBuild.getKey() + 1; i < buildNumber; i ++) {
                if (testStatusMap.containsKey(i) && testStatusMap.get(i) == TestStatus.SKIP) {
                    buildsToUse.remove(buildsToUse.size() - 2); // current build after last passing build
                    buildsToUse.add(new AbstractMap.SimpleEntry<>(i, testStatusMap.get(i)));
                    return buildsToUse;
                }
            }
        }
        return buildsToUse;
    }

    public void addTestResult(TestResult testResult) {
        if (testResult.buildNumber == 0) {
            throw new RuntimeException("Bad build number for test " + testResult.name);
        }
        switch (testResult.status) {
        case PRESUMED_PASS:
            presumedPassedBuilds = ArrayUtils.add(presumedPassedBuilds, testResult.buildNumber);
            break;
        case PASS:
            passedBuilds = ArrayUtils.add(passedBuilds, testResult.buildNumber);
            break;
        case FAIL:
            failedBuilds = ArrayUtils.add(failedBuilds, testResult.buildNumber);
            break;
        case SKIP:
            skippedBuilds = ArrayUtils.add(skippedBuilds, testResult.buildNumber);
            break;
        }
        if (testResult.status != TestStatus.PRESUMED_PASS) {
            presumedPassedBuilds = ArrayUtils.remove(presumedPassedBuilds, testResult.buildNumber);
        }
        if (testResult.buildNumber <= buildNumber) {
            return;
        }

        // update to latest build result
        this.jobBuildId = testResult.jobBuildId;
        this.url = testResult.url;
        this.dataProviderIndex = testResult.dataProviderIndex;
        this.buildNumber = testResult.buildNumber;
        this.status = testResult.status;
        this.similarSkips = testResult.similarSkips;
        this.exception = testResult.exception;
        this.startedAt = testResult.startedAt;
        this.duration = testResult.duration;
    }

    public TestStatusOnBuildRemoval removeUnimportantTestResultsForBuild(JobBuild build, int lastBuildToKeepNumber) {
        int newestRun = Stream.of(passedBuilds, presumedPassedBuilds, failedBuilds, skippedBuilds)
                .filter(Objects::nonNull).flatMap(Arrays::stream).mapToInt(Integer::intValue).max().orElse(-1);

        if (newestRun < lastBuildToKeepNumber) {
            return DELETEABLE;
        }

        if (Stream.of(passedBuilds, presumedPassedBuilds, failedBuilds, skippedBuilds)
                .filter(Objects::nonNull).flatMap(Arrays::stream).noneMatch(id -> id.intValue() == build.buildNumber)) {
            return NO_UPDATE_NEEDED;
        }

        int newestPass = Stream.of(passedBuilds, presumedPassedBuilds).filter(Objects::nonNull).flatMap(Arrays::stream).mapToInt(Integer::intValue).max().orElse(-1);
        int firstFailureAfterPass = failedBuilds != null ? Arrays.stream(failedBuilds).filter(failure -> failure > newestPass).findFirst().orElse(-1) : -1;
        int firstSkipAfterPass = skippedBuilds != null ? Arrays.stream(skippedBuilds).filter(skip -> skip > newestPass).findFirst().orElse(-1) : -1;
        int firstFailureOrSkipAfterPass = Stream.of(firstFailureAfterPass, firstSkipAfterPass).filter(val -> val != -1).mapToInt(val -> val).min().orElse(-1);
        if (newestPass == build.buildNumber || firstFailureOrSkipAfterPass == build.buildNumber) {
            TestStatus statusForBuild = newestPass == build.buildNumber ? TestStatus.PASS :
                    firstFailureAfterPass == build.buildNumber ? TestStatus.FAIL : TestStatus.SKIP;
            LoggerFactory.getLogger(this.getClass()).info("Test {} {} still contains build {}", classAndTestName(), statusForBuild, build.name);
            return CONTAINS_BUILD;
        }

        passedBuilds = ArrayUtils.remove(passedBuilds, build.buildNumber);
        presumedPassedBuilds = ArrayUtils.remove(presumedPassedBuilds, build.buildNumber);
        failedBuilds = ArrayUtils.remove(failedBuilds, build.buildNumber);
        skippedBuilds = ArrayUtils.remove(skippedBuilds, build.buildNumber);
        return UPDATEABLE;
    }

    public boolean matchesByDataProviderIndex(TestResult testResult) {
        if (!this.fullPackageAndTestName().equals(testResult.fullPackageAndTestName())) {
            return false;
        }

        return Objects.equals(dataProviderIndex, testResult.dataProviderIndex);
    }

    @Override
    public String toString() {
        String text = classAndTestName() + " " + status;
        if (exception != null) {
            text += " " + exception;
        }
        return text;
    }

    private String testResultLink(TestResult testResult) {
        String commitIdSuffix = StringUtils.isNotBlank(testResult.commitId) ? " with commit " + testResult.commitId : "";
        String title = String.format("%s in build %s on %s%s", testResult.status.getDescription(), testResult.buildNumber,
                START_DAY_FORMATTER.format(testResult.getStartedAt()), commitIdSuffix);
        return String.format("<a class =\"%s\" href = \"%s\" title=\"%s\" %s>%s</a>", testResult.status.cssClass, testResult.url, title,
                LINK_IN_NEW_TAB, testResult.buildNumber);
    }

    private boolean containsBuildNumbers(Integer[] values, int... buildNumbers) {
        if (values == null) {
            return false;
        }
        return Arrays.stream(buildNumbers).allMatch(buildNumber -> Arrays.stream(values).anyMatch(value -> value == buildNumber));
    }

    private int buildNumber() {
        return buildNumber;
    }

    public enum TestStatus {
        PRESUMED_PASS("Presumed passed", "presumedTestPass"),
        PASS("Passed", "testPass"),
        SKIP("Skipped", "testSkip"),
        ABORTED("Aborted", "testFail"),
        FAIL("Failed", "testFail"),
        // Test Statuses for Cypress Junit tests, they are only used for deserialization
        PASSED, FIXED, REGRESSION, FAILED;
        private final String description;
        private final String cssClass;

        TestStatus() {
            this.description = null;
            this.cssClass = null;
        }
        TestStatus(String description, String cssClass) {
            this.description = description;
            this.cssClass = cssClass;
        }

        public String getDescription() {
            return description;
        }

        public static boolean isPass(TestStatus status) {
            return status == PASS || status == PRESUMED_PASS;
        }
    }

    public enum TestStatusOnBuildRemoval {
        UPDATEABLE,

        DELETEABLE,
        CONTAINS_BUILD,
        NO_UPDATE_NEEDED
    }
}

