import soap.ChatServer;
import soap.FileInfo;
import soap.IChatServer;
import soap.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    private static IChatServer proxy;
    private static String sessionId = null;
    private static Timer timer = null;

    public static void main(String[] args) {
        try {
            ChatServer service = new ChatServer();
            proxy = service.getChatServerProxy();

            System.out.println("Connected to SOAP server");
        } catch (Exception e) {
            System.out.println("Connection error: " + e.getMessage());
            return;
        }

        Scanner scanner = new Scanner(System.in);

        System.out.println("Available commands:");
        System.out.println("ping");
        System.out.println("echo <text>");
        System.out.println("login <username> <password>");
        System.out.println("list");
        System.out.println("msg <user> <text>");
        System.out.println("file <user> <path>");
        System.out.println("exit");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            try {
                if (input.equals("ping")) {
                    proxy.ping();
                    System.out.println("Ping OK");
                } else if (input.startsWith("echo ")) {
                    String text = input.substring(5);
                    System.out.println(proxy.echo(text));
                } else if (input.startsWith("login ")) {
                    handleLogin(input);
                } else if (input.equals("list")) {
                    handleList();
                } else if (input.startsWith("msg ")) {
                    handleMessage(input);
                } else if (input.startsWith("file ")) {
                    handleFile(input);
                } else if (input.equals("exit")) {
                    handleExit();
                    break;
                } else {
                    System.out.println("Unknown command");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        scanner.close();
    }

    private static void handleLogin(String input) throws Exception {
        String[] parts = input.split("\\s+", 3);

        if (parts.length < 3) {
            System.out.println("Usage: login <username> <password>");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        sessionId = proxy.login(username, password);
        System.out.println("Logged in successfully");

        startPolling();
    }

    private static void handleList() throws Exception {
        if (!isLoggedIn()) {
            System.out.println("You must login first");
            return;
        }

        List<String> users = proxy.listUsers(sessionId);

        if (users == null || users.isEmpty()) {
            System.out.println("No active users");
            return;
        }

        System.out.println("Active users:");
        for (String user : users) {
            System.out.println("- " + user);
        }
    }

    private static void handleMessage(String input) throws Exception {
        if (!isLoggedIn()) {
            System.out.println("You must login first");
            return;
        }

        String[] parts = input.split("\\s+", 3);

        if (parts.length < 3) {
            System.out.println("Usage: msg <user> <text>");
            return;
        }

        String receiver = parts[1];
        String text = parts[2];

        Message msg = new Message();
        msg.setReceiver(receiver);
        msg.setMessage(text);

        proxy.sendMessage(sessionId, msg);
        System.out.println("Message sent");
    }

    private static void handleFile(String input) throws Exception {
        if (!isLoggedIn()) {
            System.out.println("You must login first");
            return;
        }

        String[] parts = input.split("\\s+", 3);

        if (parts.length < 3) {
            System.out.println("Usage: file <user> <path>");
            return;
        }

        String receiver = parts[1];
        String path = parts[2];

        File localFile = new File(path);
        if (!localFile.exists() || !localFile.isFile()) {
            System.out.println("File not found: " + path);
            return;
        }

        byte[] content = Files.readAllBytes(localFile.toPath());

        FileInfo fileInfo = new FileInfo();
        fileInfo.setReceiver(receiver);
        fileInfo.setFilename(localFile.getName());
        fileInfo.setFileContent(content);

        proxy.sendFile(sessionId, fileInfo);
        System.out.println("File sent");
    }

    private static void handleExit() {
        stopPolling();

        if (sessionId != null) {
            try {
                proxy.exit(sessionId);
                System.out.println("Logged out");
            } catch (Exception e) {
                System.out.println("Exit warning: " + e.getMessage());
            }
        }

        System.out.println("Bye!");
    }

    private static boolean isLoggedIn() {
        return sessionId != null && !sessionId.trim().isEmpty();
    }

    private static void startPolling() {
        stopPolling();

        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isLoggedIn()) {
                    return;
                }

                try {
                    Message msg = proxy.receiveMessage(sessionId);
                    if (msg != null) {
                        System.out.println();
                        System.out.println("[NEW MESSAGE] from " + msg.getSender() + ": " + msg.getMessage());
                        System.out.print("> ");
                    }
                } catch (Exception e) {
                    System.out.println();
                    System.out.println("[Message receive error] " + e.getMessage());
                    System.out.print("> ");
                }

                try {
                    FileInfo file = proxy.receiveFile(sessionId);
                    if (file != null) {
                        File dir = new File("received");
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }

                        String fileName = file.getFilename();
                        if (fileName == null || fileName.trim().isEmpty()) {
                            fileName = "received_file";
                        }

                        File outFile = new File(dir, fileName);
                        int index = 1;
                        while (outFile.exists()) {
                            outFile = new File(dir, index + "_" + fileName);
                            index++;
                        }

                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            fos.write(file.getFileContent());
                        }

                        System.out.println();
                        System.out.println("[NEW FILE] from " + file.getSender() + ": " + outFile.getPath());
                        System.out.print("> ");
                    }
                } catch (Exception e) {
                    System.out.println();
                    System.out.println("[File receive error] " + e.getMessage());
                    System.out.print("> ");
                }
            }
        }, 0, 3000);
    }

    private static void stopPolling() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}