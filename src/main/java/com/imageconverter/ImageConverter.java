package com.imageconverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Core class for image conversion operations.
 * Handles loading, resizing, and saving images in WebP format.
 */
public class ImageConverter {
    private static final Logger logger = LoggerFactory.getLogger(ImageConverter.class);
    private static final float WEBP_QUALITY = 0.90f;
    private static final String[] SUPPORTED_FORMATS = {"jpg", "jpeg", "png", "bmp"};

    /**
     * Load an image from file.
     *
     * @param file the image file to load
     * @return BufferedImage or null if loading fails
     */
    public BufferedImage loadImage(File file) {
        try {
            logger.info("Loading image: {}", file.getAbsolutePath());
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                logger.error("Failed to load image (unsupported format): {}", file.getName());
                return null;
            }
            logger.debug("Image loaded successfully: {} ({}x{})", file.getName(), 
                        image.getWidth(), image.getHeight());
            return image;
        } catch (IOException e) {
            logger.error("Error loading image: {}", file.getName(), e);
            return null;
        }
    }

    /**
     * Resize image by specifying the shorter edge dimension.
     * The longer edge is calculated automatically to maintain aspect ratio.
     *
     * @param original the original image
     * @param shortEdgeSize the desired size of the shorter edge in pixels
     * @return resized BufferedImage
     */
    public BufferedImage resizeByShortEdge(BufferedImage original, int shortEdgeSize) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // If no resizing needed (shortEdgeSize = 0 or negative)
        if (shortEdgeSize <= 0) {
            logger.debug("No resizing needed, returning original image");
            return original;
        }

        int newWidth, newHeight;
        
        // Determine which edge is shorter and calculate new dimensions
        if (originalWidth < originalHeight) {
            // Width is shorter
            newWidth = shortEdgeSize;
            newHeight = (int) Math.round((double) originalHeight * shortEdgeSize / originalWidth);
        } else {
            // Height is shorter (or equal)
            newHeight = shortEdgeSize;
            newWidth = (int) Math.round((double) originalWidth * shortEdgeSize / originalHeight);
        }

        logger.info("Resizing image from {}x{} to {}x{}", 
                   originalWidth, originalHeight, newWidth, newHeight);

        // Create resized image with high quality
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        
        // High quality rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return resized;
    }

    /**
     * Save image as WebP format with specified quality.
     *
     * @param image the image to save
     * @param outputFile the output file
     * @return true if successful, false otherwise
     */
    public boolean saveAsWebP(BufferedImage image, File outputFile) {
        try {
            logger.info("Saving image as WebP: {}", outputFile.getAbsolutePath());

            // Get WebP writer
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
            if (!writers.hasNext()) {
                logger.error("No WebP writer found. Make sure webp-imageio is in the classpath.");
                return false;
            }

            ImageWriter writer = writers.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            
            // Set compression mode and type for WebP
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            String[] compressionTypes = writeParam.getCompressionTypes();
            if (compressionTypes != null && compressionTypes.length > 0) {
                writeParam.setCompressionType(compressionTypes[0]);
            }
            writeParam.setCompressionQuality(WEBP_QUALITY);

            // Ensure parent directory exists
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Write the image
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), writeParam);
            } finally {
                writer.dispose();
            }

            logger.info("Successfully saved WebP image: {} (size: {} bytes)", 
                       outputFile.getName(), outputFile.length());
            return true;

        } catch (IOException e) {
            logger.error("Error saving WebP image: {}", outputFile.getName(), e);
            return false;
        }
    }

    /**
     * Convert a single image file to WebP format with optional resizing.
     *
     * @param inputFile the input image file
     * @param shortEdgeSize the desired size of the shorter edge (0 or negative for no resize)
     * @return true if conversion was successful
     */
    public boolean convertImage(File inputFile, int shortEdgeSize) {
        logger.info("Starting conversion: {}", inputFile.getName());

        // Load image
        BufferedImage image = loadImage(inputFile);
        if (image == null) {
            return false;
        }

        // Resize if needed
        if (shortEdgeSize > 0) {
            image = resizeByShortEdge(image, shortEdgeSize);
        }

        // Create output file path (same directory, same name, .webp extension)
        String inputPath = inputFile.getAbsolutePath();
        String outputPath = inputPath.substring(0, inputPath.lastIndexOf('.')) + ".webp";
        File outputFile = new File(outputPath);

        // Save as WebP
        boolean success = saveAsWebP(image, outputFile);
        
        if (success) {
            logger.info("Conversion completed successfully: {} -> {}", 
                       inputFile.getName(), outputFile.getName());
        } else {
            logger.error("Conversion failed: {}", inputFile.getName());
        }

        return success;
    }

    /**
     * Check if a file is a supported image format.
     *
     * @param file the file to check
     * @return true if file has a supported extension
     */
    public static boolean isSupportedFormat(File file) {
        String name = file.getName().toLowerCase();
        for (String format : SUPPORTED_FORMATS) {
            if (name.endsWith("." + format)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Estimate the output file size (rough approximation).
     * WebP typically achieves 70-80% compression compared to original.
     *
     * @param inputFile the input file
     * @return estimated output size in bytes
     */
    public static long estimateOutputSize(File inputFile) {
        // WebP is typically 70% of original size (conservative estimate)
        return (long) (inputFile.length() * 0.7);
    }
}
