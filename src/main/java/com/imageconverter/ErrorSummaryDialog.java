package com.imageconverter;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;

/**
 * Dialog to display conversion results and error summary.
 */
public class ErrorSummaryDialog extends Dialog<Void> {
    
    /**
     * Create and show an error summary dialog.
     *
     * @param result the conversion result
     */
    public ErrorSummaryDialog(ConversionResult result) {
        setTitle("Conversion Summary");
        setHeaderText("Batch Conversion Complete");
        
        // Create the content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Statistics
        GridPane stats = new GridPane();
        stats.setHgap(15);
        stats.setVgap(10);
        
        Label totalLabel = new Label("Total files:");
        totalLabel.setStyle("-fx-font-weight: bold;");
        Label totalValue = new Label(String.valueOf(result.getTotalCount()));
        
        Label successLabel = new Label("Successfully converted:");
        successLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        Label successValue = new Label(String.valueOf(result.getSuccessCount()));
        successValue.setStyle("-fx-text-fill: #4CAF50;");
        
        Label failLabel = new Label("Failed:");
        failLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #F44336;");
        Label failValue = new Label(String.valueOf(result.getFailCount()));
        failValue.setStyle("-fx-text-fill: #F44336;");
        
        stats.add(totalLabel, 0, 0);
        stats.add(totalValue, 1, 0);
        stats.add(successLabel, 0, 1);
        stats.add(successValue, 1, 1);
        stats.add(failLabel, 0, 2);
        stats.add(failValue, 1, 2);
        
        // Space saved info
        if (result.getSpaceSaved() > 0) {
            Label savedLabel = new Label("Space saved:");
            savedLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
            Label savedValue = new Label(formatBytes(result.getSpaceSaved()));
            savedValue.setStyle("-fx-text-fill: #2196F3;");
            stats.add(savedLabel, 0, 3);
            stats.add(savedValue, 1, 3);
        }
        
        content.getChildren().add(stats);
        
        // Error details (if any)
        if (!result.getErrors().isEmpty()) {
            Label errorHeader = new Label("Errors:");
            errorHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            content.getChildren().add(errorHeader);
            
            // Error list in a scrollable text area
            TextArea errorArea = new TextArea();
            errorArea.setEditable(false);
            errorArea.setPrefRowCount(10);
            errorArea.setWrapText(true);
            
            StringBuilder errorText = new StringBuilder();
            for (String error : result.getErrors()) {
                errorText.append(error).append("\n");
            }
            errorArea.setText(errorText.toString());
            
            VBox.setVgrow(errorArea, Priority.ALWAYS);
            content.getChildren().add(errorArea);
        }
        
        // Log file location
        File logDir = new File("logs");
        if (logDir.exists()) {
            Label logLabel = new Label("Detailed logs available at: " + logDir.getAbsolutePath());
            logLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
            content.getChildren().add(logLabel);
        }
        
        getDialogPane().setContent(content);
        getDialogPane().setPrefSize(500, 400);
        
        // Add OK button
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().add(okButton);
    }
    
    /**
     * Format bytes into human-readable string.
     */
    private String formatBytes(long bytes) {
        if (bytes < 0) {
            return "-" + formatBytes(-bytes);
        }
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
}
