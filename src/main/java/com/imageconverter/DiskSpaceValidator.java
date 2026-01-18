package com.imageconverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Validates available disk space before performing batch conversions.
 */
public class DiskSpaceValidator {
    private static final Logger logger = LoggerFactory.getLogger(DiskSpaceValidator.class);
    private static final double SAFETY_MARGIN = 1.2; // 20% safety margin

    /**
     * Validate that there is sufficient disk space for the conversion.
     *
     * @param files list of files to be converted
     * @param targetDirectory the directory where WebP files will be saved
     * @return ValidationResult with status and message
     */
    public ValidationResult validateDiskSpace(List<File> files, File targetDirectory) {
        logger.info("Validating disk space for {} files", files.size());

        if (files.isEmpty()) {
            return new ValidationResult(true, "No files to convert");
        }

        // Calculate estimated space needed
        long estimatedSpaceNeeded = 0;
        for (File file : files) {
            estimatedSpaceNeeded += ImageConverter.estimateOutputSize(file);
        }

        // Apply safety margin
        long requiredSpace = (long) (estimatedSpaceNeeded * SAFETY_MARGIN);

        // Get available disk space
        long availableSpace = targetDirectory.getUsableSpace();

        logger.info("Estimated space needed: {} bytes ({})", 
                   requiredSpace, formatBytes(requiredSpace));
        logger.info("Available disk space: {} bytes ({})", 
                   availableSpace, formatBytes(availableSpace));

        if (availableSpace < requiredSpace) {
            String message = String.format(
                "Insufficient disk space!\n\n" +
                "Required: %s\n" +
                "Available: %s\n\n" +
                "Please free up disk space before continuing.",
                formatBytes(requiredSpace),
                formatBytes(availableSpace)
            );
            logger.warn("Insufficient disk space for conversion");
            return new ValidationResult(false, message);
        }

        logger.info("Disk space validation passed");
        return new ValidationResult(true, "Sufficient disk space available");
    }

    /**
     * Format bytes into human-readable string.
     *
     * @param bytes number of bytes
     * @return formatted string (e.g., "1.5 MB")
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Result of disk space validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
