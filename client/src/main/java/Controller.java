import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Controller implements Initializable {

    @FXML
    ListView<String> listView, listView2;

    @FXML
    TextField input;

    private static final byte[] buffer = new byte[1024];
    private static final String APP_NAME = "client/Files/";
    private static final String ROOT_DIR = "server/root/";

    private DataInputStream is;
    private DataOutputStream os;

    public static List<String> list = new ArrayList<>();

    public void copy(ActionEvent actionEvent) throws Exception {
        String msg = input.getText();
        input.clear();
        getSource(APP_NAME);
        if (msg != null && msg.trim().length() != 0 /*&& !checkFilename(msg)*/) {
            transfer(new File(APP_NAME + msg),
                    new File(ROOT_DIR + "copy_" + msg));
        } else {
            log.error("Error filename!!!");
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error filename!!!", ButtonType.OK);
            alert.showAndWait();
        }
        os.writeUTF(msg);
        os.flush();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Socket socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());

            listView.getItems().addAll(getSource(APP_NAME));

            listView2.getItems().addAll(getSource(ROOT_DIR));

            Thread daemon = new Thread(() -> {
                try {
                    while (true) {
                        String msg = is.readUTF();
                        log.debug("received: {}", msg);
                        Platform.runLater(() -> listView.getItems().clear());
                        Platform.runLater(() -> listView.getItems().addAll(getSource(APP_NAME)));
                        Platform.runLater(() -> listView2.getItems().clear());
                        Platform.runLater(() -> listView2.getItems().addAll(getSource(ROOT_DIR)));
                    }
                } catch (Exception e) {
                    log.error("exception while read from input stream");
                }
            });
            daemon.setDaemon(true);
            daemon.start();
        } catch (IOException ioException) {
            log.error("e=", ioException);
        }
    }

    private void transfer(File src, File dst) {
        try (FileInputStream is = new FileInputStream(src);
             FileOutputStream os = new FileOutputStream(dst)
        ) {
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                os.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getSource(String path) {
        list.clear();
        try {
            File folder = new File(path);
            String[] files = folder.list();
            for (String fileName : files) {
                list.add(fileName);
            }
        } catch (NullPointerException e) {
            System.out.println("Ошибка" + e.getMessage());
        }
        return list;
    }


    //Проверка на существование файла
    public static boolean checkFilename(String msg) {
        File folder = new File(APP_NAME);
        String[] files = folder.list();
        for (String fileName : files) {
            if (msg == fileName) {
                return true;
            }
        }
        return false;
    }
}