import java.io.*;
import java.net.*;
import java.util.zip.CRC32;
import java.nio.file.Files;

public class Server {
    private static final int SERVER_PORT = 1234;
    private static final String SERVER_DIRECTORY = "server_files/";

    public static void main(String[] args) throws IOException {
        System.out.println("Server started...");

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            while (true) {
                try (Socket socket = serverSocket.accept();
                     DataInputStream in = new DataInputStream(socket.getInputStream());
                     DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                    System.out.println("Client connected");

                    // Authenticate client
                    String password = in.readUTF();
                    if (password.equals("1234")) {
                        out.writeUTF("Authentication successful.");
                    } else {
                        out.writeUTF("Authentication failed.");
                        continue;
                    }

                    // Handle file operations
                    String command;
                    while (true) {
                        command = in.readUTF();
                        if (command.equals("exit")) {
                            break;
                        }

                        String[] parts = command.split(" ");
                        if (parts[0].equals("put")) {
                            uploadFile(parts[1], in, out);
                        } else if (parts[0].equals("get")) {
                            downloadFile(parts[1], out, in);
                        }
                    }
                }
            }
        }
    }

    private static void uploadFile(String filename, DataInputStream in, DataOutputStream out) throws IOException {
        // Receive checksum and file length
        String checksum = in.readUTF();
        int fileLength = in.readInt();

        byte[] fileData = new byte[fileLength];
        in.readFully(fileData);  // Read the file data

        // Write the file to the server directory
        try (FileOutputStream fos = new FileOutputStream(SERVER_DIRECTORY + filename)) {
            fos.write(fileData);
        }

        out.writeUTF("File uploaded successfully");

        // Validate checksum
        String calculatedChecksum = calculateCRC(fileData);
        if (!checksum.equals(calculatedChecksum)) {
            System.out.println("Checksum mismatch, file corrupted.");
        } else {
            System.out.println("File uploaded successfully with correct checksum.");
        }
    }

    private static void downloadFile(String filename, DataOutputStream out, DataInputStream in) throws IOException {
        File file = new File(SERVER_DIRECTORY + filename);

        if (!file.exists()) {
            out.writeUTF("File not found");
            return;
        }

        byte[] fileData = Files.readAllBytes(file.toPath());
        out.writeInt(fileData.length);  // Send file length
        out.write(fileData);  // Send the file data

        String checksum = calculateCRC(fileData);
        out.writeUTF(checksum);  // Send the checksum to the client
    }

    private static String calculateCRC(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return Long.toHexString(crc.getValue()); // Return checksum as hex
    }
}
