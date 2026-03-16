package org.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.UnaryOperator;

public class GUI extends Application {
    public TextArea textAreaInputFile = new TextArea();
    public TextArea textAreaOutputFile = new TextArea();
    public TextArea textAreaKey = new TextArea();
    public TextField textFieldInitialState = new TextField("11111111111111111111111111111111111"); // 35 бит

    public Button btnOpenFile = new Button("Загрузить файл");
    public Button btnEncrypt = new Button("Зашифровать/Расшифровать");
    public Button btnSaveFile = new Button("Сохранить результат");

    private final VBox root = new VBox();

    private byte[] inputFile = null;
    private byte[] processedData = null;

    @Override
    public void start(Stage stage) {

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("[01]{0,35}")) {
                return change;
            }
            return null;
        };
        textFieldInitialState.setTextFormatter(new TextFormatter<>(filter));

        root.setPadding(new Insets(15));
        root.setSpacing(10);

        initTextAreas(stage);
        initButtons(stage);

        root.getChildren().addAll(
                new Label("Генератор ключевой последовательности на основе линейного сдвигового регистра с обратной связью LFSR (размерность регистра 35)."),
                new Label("Многочлен: x^35 + x^2 + 1"),
                btnOpenFile,
                new Label("Начальное состояние регистра (35 битов 0/1):"),
                textFieldInitialState,
                new Separator(),

                new Label("Входной файл (бинарный вид):"),
                textAreaInputFile,
                new Separator(),

                btnEncrypt,
                new Separator(),

                new Label("Биты ключа"),
                textAreaKey,
                new Label("Результат (бинарный вид):"),
                textAreaOutputFile,
                btnSaveFile
        );

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Потоковое шифрование (LFSR-35)");
        stage.setScene(scene);
        stage.show();
    }

    private void initButtons(Stage stage) {
        btnOpenFile.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();

            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Все файлы", "*.*"),
                    new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt"),
                    new FileChooser.ExtensionFilter("Бинарные файлы", "*.bin")
            );

            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                try {
                    inputFile = Files.readAllBytes(selectedFile.toPath());

                    StringBuilder binaryContent = new StringBuilder();
                    for (int i = 0; i < Math.min(inputFile.length, 500); i++) {
                        String binaryString = String.format("%8s", Integer.toBinaryString(inputFile[i] & 0xFF)).replace(' ', '0');
                        binaryContent.append(binaryString).append(" ");

                        if ((i + 1) % 4 == 0) {
                            binaryContent.append("\n");
                        }
                    }

                    if (inputFile.length > 500) {
                        binaryContent.append("\n... и ещё ").append(inputFile.length - 500).append(" байт");
                    }

                    textAreaInputFile.setText(binaryContent.toString());

                    processedData = null;
                    textAreaOutputFile.clear();
                    textAreaKey.clear();

                } catch (IOException ex) {
                    showError("Ошибка при чтении файла: " + ex.getMessage());
                }
            }
        });

        btnEncrypt.setOnAction(e -> {
            processCryptography();
        });

        btnSaveFile.setOnAction(e -> {
            if (processedData == null) {
                showError("Нет данных для сохранения! Сначала выполните шифрование/расшифрование.");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                try {
                    Files.write(file.toPath(), processedData);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText(null);
                    alert.setContentText("Файл успешно сохранён!");
                    alert.showAndWait();

                } catch (IOException ex) {
                    showError("Ошибка записи: " + ex.getMessage());
                }
            }
        });
    }

    private void initTextAreas(Stage stage) {
        textAreaOutputFile.setPromptText("Здесь появится двоичное представление обработанного файла...");
        textAreaOutputFile.setWrapText(true);
        textAreaOutputFile.setEditable(false);

        textAreaInputFile.setPromptText("Здесь появится двоичное представление открытого файла...");
        textAreaInputFile.setWrapText(true);
        textAreaInputFile.setEditable(false);

        textAreaKey.setPromptText("Здесь появится двоичное представление ключа...");
        textAreaKey.setWrapText(true);
        textAreaKey.setEditable(false);
    }

    private void processCryptography() {
        String stateStr = textFieldInitialState.getText();

        if (stateStr.length() != 35) {
            showError("Начальное состояние должно содержать ровно 35 битов (0 или 1).");
            return;
        }

        if (inputFile == null) {
            showError("Файл не загружен. Пожалуйста, выберите файл для обработки.");
            return;
        }

        try {
            long initialState = Long.parseUnsignedLong(stateStr, 2);

            Cryptography.Result cryptoResult = Cryptography.encryptWithKey(inputFile, initialState);
            processedData = cryptoResult.encryptedData();

            StringBuilder keySb = new StringBuilder();
            for (int i = 0; i < Math.min(cryptoResult.keyStream().length, 500); i++) {
                String binaryByte = String.format("%8s", Integer.toBinaryString(cryptoResult.keyStream()[i] & 0xFF))
                        .replace(' ', '0');
                keySb.append(binaryByte).append(" ");
                if ((i + 1) % 4 == 0) {
                    keySb.append("\n");
                }
            }

            if (cryptoResult.keyStream().length > 500) {
                keySb.append("\n... и ещё ").append(cryptoResult.keyStream().length - 500).append(" байт");
            }
            textAreaKey.setText(keySb.toString());

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(processedData.length, 500); i++) {
                String binaryByte = String.format("%8s", Integer.toBinaryString(processedData[i] & 0xFF))
                        .replace(' ', '0');
                sb.append(binaryByte).append(" ");

                if ((i + 1) % 4 == 0) {
                    sb.append("\n");
                }
            }

            if (processedData.length > 500) {
                sb.append("\n... и ещё ").append(processedData.length - 500).append(" байт");
            }
            textAreaOutputFile.setText(sb.toString());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText("Обработка выполнена успешно!\n" +
                    "Размер обработанных данных: " + processedData.length + " байт");
            alert.showAndWait();

        } catch (NumberFormatException e) {
            showError("Ошибка: в поле начального состояния должны быть только 0 и 1.");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}