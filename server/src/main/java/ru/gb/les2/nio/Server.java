package ru.gb.les2.nio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Server {

    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer buffer;
    private static final String ls = "ls";
    private static final String cat = "cat";

    public Server() throws IOException {

        buffer = ByteBuffer.allocate(256);
        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();
        serverChannel.bind(new InetSocketAddress(8189));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (serverChannel.isOpen()) {

            selector.select();

            Set<SelectionKey> keys = selector.selectedKeys();

            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key);
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                iterator.remove();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Server();
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        buffer.clear();
        int read = 0;
        StringBuilder msg = new StringBuilder();
        while (true) {
            if (read == -1) {
                channel.close();
                return;
            }
            read = channel.read(buffer);
            if (read == 0) {
                break;
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                msg.append((char) buffer.get());
            }
            buffer.clear();
        }
        String message = msg.toString();
        if (message.startsWith(ls)) {/*message.equals(ls)) { Почему то не работает сравнение строк?????*/
            DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(""));
            for (Path file : stream) {
                String a = (file.getFileName() + "    ");
                channel.write(ByteBuffer.wrap((a).intern().getBytes(StandardCharsets.UTF_8)));/*?????*/
            }
        } else if (message.startsWith(cat)) {
            String[] tokens = message.split("\\s+");
            if (tokens.length > 2) {
                channel.write(ByteBuffer.wrap(("Entering Command incorrectly!!!\n").getBytes(StandardCharsets.UTF_8)));/*?????*/
                return;
            }
            String filename = tokens[1];
            channel.write(ByteBuffer.wrap((cmdCat(filename)).intern().getBytes(StandardCharsets.UTF_8)));/*?????*/
        } else {
            System.out.println(message);
            channel.write(ByteBuffer.wrap(("[" + LocalDateTime.now() + "] " + message).getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
    }

    private String cmdCat(String filename) {
        Path filePath = Paths.get(filename);
        if (Files.exists(filePath)) {
            try {
                byte[] bytes = Files.readAllBytes(filePath);
                String text = new String(bytes, StandardCharsets.UTF_8);
                return text + "\n";
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return "File does not exist\n";
    }
}