package vn.qahub.api.testrun;

public record TestCaseResponse(
        Long id, String className, String methodName, String displayName, String status,
        Long durationMs, String failureMessage, String stackTrace, String businessFlowTag) {

    public static TestCaseResponse from(TestCase testCase) {
        return new TestCaseResponse(
                testCase.getId(), testCase.getClassName(), testCase.getMethodName(), testCase.getDisplayName(),
                testCase.getStatus().name(), testCase.getDurationMs(), testCase.getFailureMessage(),
                testCase.getStackTrace(), testCase.getBusinessFlowTag());
    }
}
