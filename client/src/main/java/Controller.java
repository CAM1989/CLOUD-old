import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Controller implements Initializable {

    private static String APP_DIR = "client/Files";
    private static String ROOT_DIR = "server/root";
    private static byte[] buffer = new byte[1024];

    @FXML
    ListView<String> listView, listView2;

    @FXML
    TextField input;

    private DataInputStream is;
    private DataOutputStream os;

    public void copy(ActionEvent actionEvent) throws Exception {
        String fileName = input.getText();
        input.clear();
        sendFile(fileName);
    }

    private void sendFile(String fileName) throws IOException {
        Path file = Paths.get(APP_DIR, fileName);
        if (Files.exists(file)) {
            long size = Files.size(file);

            os.writeUTF(fileName);
            os.writeLong(size);

            InputStream fileStream = Files.newInputStream(file);
            int read;
            while ((read = fileStream.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
        } else {
            os.writeUTF(fileName);
            os.flush();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            fillFilesInCurrentDir(listView,APP_DIR);
            fillFilesInCurrentDir(listView2,ROOT_DIR);
            Socket socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            Thread daemon = new Thread(() -> {
                try {
                    while (true) {
                        String msg = is.readUTF();
                        log.debug("received: {}", msg);
                        Platform.runLater(() -> input.setText(msg));
                        Platform.runLater(() -> {
                            try {
                                fillFilesInCurrentDir(listView2,ROOT_DIR);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
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

    private void fillFilesInCurrentDir(ListView<String> list, String dir) throws IOException {
        list.getItems().clear();
        list.getItems().addAll(
                Files.list(Paths.get(dir))
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toList())
        );
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = list.getSelectionModel().getSelectedItem();
                input.setText(item);
            }
        });
    }
}
