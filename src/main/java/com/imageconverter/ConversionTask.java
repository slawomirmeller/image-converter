package com.imageconverter;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Background task for batch image conversion.
 * Extends JavaFX Task to provide progress updates and run on a background thread.
 */
public class ConversionTask extends Task<ConversionResult> {
    private static final Logger logger = LoggerFactory.getLogger(ConversionTask.class);
    
    private final List<File> files;
    private final int shortEdgeSize;
    private final ImageConverter converter;

    /**
     * Create a new conversion task.
     *
     * @param files list of files to convert
     * @param shortEdgeSize desired size of the shorter edge (0 for no resize)
     */
    public ConversionTask(List<File> files, int shortEdgeSize) {
        this.files = files;
        this.shortEdgeSize = shortEdgeSize;
        this.converter = new ImageConverter();
    }

    @Override
    protected ConversionResult call() throws Exception {
        logger.info("Starting batch conversion of {} files", files.size());
        
        int totalFiles = files.size();
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        long totalSaved = 0;

        updateMessage("Starting conversion...");
        updateProgress(0, totalFiles);

        for (int i = 0; i < totalFiles; i++) {
            // Check if task was cancelled
            if (isCancelled()) {
                logger.info("Conversion task was cancelled");
                updateMessage("Conversion cancelled");
                break;
            }

            File file = files.get(i);
            updateMessage("Converting: " + file.getName());
            
            try {
                long originalSize = file.length();
                boolean success = converter.convertImage(file, shortEdgeSize);
                
                if (success) {
                    successCount++;
                    
                    // Calculate space saved
                    String webpPath = file.getAbsolutePath();
                    webpPath = webpPath.substring(0, webpPath.lastIndexOf('.')) + ".webp";
                    File webpFile = new File(webpPath);
                    if (webpFile.exists()) {
                        long webpSize = webpFile.length();
                        totalSaved += (originalSize - webpSize);
                    }
                    
                    logger.debug("Successfully converted: {}", file.getName());
                } else {
                    failCount++;
                    String errorMsg = "Failed to convert: " + file.getName();
                    errors.add(errorMsg);
                    logger.error(errorMsg);
                }
            } catch (Exception e) {
                failCount++;
                String errorMsg = file.getName() + ": " + e.getMessage();
                errors.add(errorMsg);
                logger.error("Exception during conversion of {}", file.getName(), e);
            }

            // Update progress
            updateProgress(i + 1, totalFiles);
        }

        updateMessage("Conversion complete");
        logger.info("Batch conversion completed: {} successful, {} failed", successCount, failCount);

        return new ConversionResult(successCount, failCount, errors, totalSaved);
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        logger.info("ConversionTask succeeded");
    }

    @Override
    protected void failed() {
        super.failed();
        logger.error("ConversionTask failed", getException());
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        logger.warn("ConversionTask was cancelled");
    }
}
