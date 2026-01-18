package com.imageconverter;

import java.util.List;

/**
 * Result of a batch conversion operation.
 */
public class ConversionResult {
    private final int successCount;
    private final int failCount;
    private final List<String> errors;
    private final long spaceSaved;

    public ConversionResult(int successCount, int failCount, List<String> errors, long spaceSaved) {
        this.successCount = successCount;
        this.failCount = failCount;
        this.errors = errors;
        this.spaceSaved = spaceSaved;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public long getSpaceSaved() {
        return spaceSaved;
    }

    public int getTotalCount() {
        return successCount + failCount;
    }
}
