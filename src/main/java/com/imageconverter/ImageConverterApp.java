package com.imageconverter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main JavaFX application for Image to WebP Converter.
 * Provides a dark-themed GUI for batch converting images to WebP format.
 */
public class ImageConverterApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(ImageConverterApp.class);
    
    // UI Components
    private TextField directoryField;
    private TextField sizeField;
    private ListView<String> fileListView;
    private Button chooseButton;
    private Button convertButton;
    private ProgressBar progressBar;
    private Label statusLabel;
    
    // Data
    private File selectedDirectory;
    private List<File> imageFiles;
    private DiskSpaceValidator validator;

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting Image Converter Application");
        
        validator = new DiskSpaceValidator();
        imageFiles = new ArrayList<>();
        
        primaryStage.setTitle("Image to WebP Converter");
        
        // Create main layout
        VBox root = createMainLayout();
        root.getStyleClass().add("main-container");
        
        // Create scene with dark theme
        Scene scene = new Scene(root, 700, 550);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(500);
        
        primaryStage.show();
        
        logger.info("Application window displayed");
    }

    /**
     * Create the main layout with all UI components.
     */
    private VBox createMainLayout() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        
        // Title
        Label titleLabel = new Label("Image to WebP Converter");
        titleLabel.getStyleClass().add("title-label");
        
        // Directory selection section
        VBox directorySection = createDirectorySection();
        
        // Resize options section
        VBox resizeSection = createResizeSection();
        
        // File list section
        VBox fileListSection = createFileListSection();
        VBox.setVgrow(fileListSection, Priority.ALWAYS);
        
        // Progress section
        VBox progressSection = createProgressSection();
        
        // Convert button
        convertButton = new Button("Convert to WebP");
        convertButton.setMaxWidth(Double.MAX_VALUE);
        convertButton.setDisable(true);
        convertButton.setOnAction(e -> startConversion());
        
        layout.getChildren().addAll(
            titleLabel,
            new Separator(),
            directorySection,
            resizeSection,
            fileListSection,
            progressSection,
            convertButton
        );
        
        return layout;
    }

    /**
     * Create directory selection section.
     */
    private VBox createDirectorySection() {
        VBox section = new VBox(8);
        
        Label label = new Label("Select Directory:");
        
        HBox selectionBox = new HBox(10);
        selectionBox.setAlignment(Pos.CENTER_LEFT);
        
        directoryField = new TextField();
        directoryField.setPromptText("No directory selected");
        directoryField.setEditable(false);
        HBox.setHgrow(directoryField, Priority.ALWAYS);
        
        chooseButton = new Button("Choose Directory");
        chooseButton.getStyleClass().add("secondary-button");
        chooseButton.setOnAction(e -> chooseDirectory());
        
        selectionBox.getChildren().addAll(directoryField, chooseButton);
        section.getChildren().addAll(label, selectionBox);
        
        return section;
    }

    /**
     * Create resize options section.
     */
    private VBox createResizeSection() {
        VBox section = new VBox(8);
        
        Label label = new Label("Resize Options:");
        
        HBox resizeBox = new HBox(10);
        resizeBox.setAlignment(Pos.CENTER_LEFT);
        
        Label sizeLabel = new Label("Short edge size (pixels):");
        
        sizeField = new TextField();
        sizeField.setPromptText("Leave empty for original size");
        sizeField.setPrefWidth(150);
        
        Label infoLabel = new Label("(longer edge scales automatically)");
        infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
        
        resizeBox.getChildren().addAll(sizeLabel, sizeField, infoLabel);
        section.getChildren().addAll(label, resizeBox);
        
        return section;
    }

    /**
     * Create file list section.
     */
    private VBox createFileListSection() {
        VBox section = new VBox(8);
        
        Label label = new Label("Found Images:");
        
        fileListView = new ListView<>();
        fileListView.setPlaceholder(new Label("No images found. Select a directory."));
        VBox.setVgrow(fileListView, Priority.ALWAYS);
        
        section.getChildren().addAll(label, fileListView);
        
        return section;
    }

    /**
     * Create progress section.
     */
    private VBox createProgressSection() {
        VBox section = new VBox(8);
        
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #888888;");
        
        section.getChildren().addAll(progressBar, statusLabel);
        
        return section;
    }

    /**
     * Handle directory selection.
     */
    private void chooseDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Image Directory");
        
        if (selectedDirectory != null && selectedDirectory.exists()) {
            chooser.setInitialDirectory(selectedDirectory);
        }
        
        File directory = chooser.showDialog(chooseButton.getScene().getWindow());
        
        if (directory != null) {
            selectedDirectory = directory;
            directoryField.setText(directory.getAbsolutePath());
            loadImageFiles();
            logger.info("Directory selected: {}", directory.getAbsolutePath());
        }
    }

    /**
     * Load image files from the selected directory.
     */
    private void loadImageFiles() {
        imageFiles.clear();
        fileListView.getItems().clear();
        
        if (selectedDirectory == null || !selectedDirectory.exists()) {
            return;
        }
        
        File[] files = selectedDirectory.listFiles();
        if (files == null) {
            return;
        }
        
        imageFiles = Arrays.stream(files)
            .filter(File::isFile)
            .filter(ImageConverter::isSupportedFormat)
            .collect(Collectors.toList());
        
        List<String> fileNames = imageFiles.stream()
            .map(File::getName)
            .collect(Collectors.toList());
        
        fileListView.getItems().addAll(fileNames);
        
        // Enable convert button if files found
        convertButton.setDisable(imageFiles.isEmpty());
        
        statusLabel.setText(String.format("Found %d image(s)", imageFiles.size()));
        logger.info("Loaded {} image files", imageFiles.size());
    }

    /**
     * Start the conversion process.
     */
    private void startConversion() {
        // Parse resize dimension
        int shortEdgeSize = 0;
        String sizeText = sizeField.getText().trim();
        if (!sizeText.isEmpty()) {
            try {
                shortEdgeSize = Integer.parseInt(sizeText);
                if (shortEdgeSize <= 0) {
                    showError("Invalid Size", "Please enter a positive number for the image size.");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Invalid Size", "Please enter a valid number for the image size.");
                return;
            }
        }
        
        // Validate disk space
        DiskSpaceValidator.ValidationResult validation = 
            validator.validateDiskSpace(imageFiles, selectedDirectory);
        
        if (!validation.isValid()) {
            showError("Insufficient Disk Space", validation.getMessage());
            logger.warn("Conversion aborted due to insufficient disk space");
            return;
        }
        
        // Disable UI during conversion
        setUIEnabled(false);
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        
        // Create and start conversion task
        ConversionTask task = new ConversionTask(imageFiles, shortEdgeSize);
        
        // Bind progress
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());
        
        // Handle completion
        task.setOnSucceeded(event -> {
            ConversionResult result = task.getValue();
            logger.info("Conversion completed: {} success, {} failed", 
                       result.getSuccessCount(), result.getFailCount());
            
            // Show summary dialog
            Platform.runLater(() -> {
                ErrorSummaryDialog dialog = new ErrorSummaryDialog(result);
                dialog.showAndWait();
                
                // Re-enable UI
                setUIEnabled(true);
                progressBar.setVisible(false);
                statusLabel.setText("Conversion complete");
                
                // Refresh file list
                loadImageFiles();
            });
        });
        
        task.setOnFailed(event -> {
            logger.error("Conversion task failed", task.getException());
            Platform.runLater(() -> {
                showError("Conversion Failed", 
                         "An error occurred during conversion: " + task.getException().getMessage());
                setUIEnabled(true);
                progressBar.setVisible(false);
                statusLabel.setText("Conversion failed");
            });
        });
        
        // Run task in background thread
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        
        logger.info("Conversion task started for {} files", imageFiles.size());
    }

    /**
     * Enable or disable UI controls.
     */
    private void setUIEnabled(boolean enabled) {
        chooseButton.setDisable(!enabled);
        convertButton.setDisable(!enabled);
        sizeField.setDisable(!enabled);
    }

    /**
     * Show error dialog.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        logger.info("Application starting...");
        launch(args);
    }

    @Override
    public void stop() {
        logger.info("Application closing");
    }
}
