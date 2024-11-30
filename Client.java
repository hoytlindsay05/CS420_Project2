import java.io.*;
import java.net.*;
import java.util.zip.CRC32;
import java.nio.file.Files;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1234;
    private static final String CLIENT_DIRECTORY = "client_files/";

    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            System.out.println("Connected to the server");

            // Authenticate
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter password: ");
            String password = reader.readLine();
            out.writeUTF(password);
            System.out.println(in.readUTF());

            // Command loop
            String command;
            while (true) {
                System.out.print("Enter command (get <filename>, put <filename>, exit): ");
                command = reader.readLine();
                out.writeUTF(command);

                String[] parts = command.split(" ");
                if (parts[0].equals("exit")) {
                    break;
                } else if (parts[0].equals("put")) {
                    uploadFile(parts[1], in, out);
                } else if (parts[0].equals("get")) {
                    downloadFile(parts[1], in, out);
                }
            }
        }
    }

    private static void uploadFile(String filename, DataInputStream in, DataOutputStream out) throws IOException {
        File file = new File(CLIENT_DIRECTORY + filename);
        if (!file.exists()) {
            System.out.println("File not found.");
            return;
        }

        byte[] fileData = Files.readAllBytes(file.toPath());
        String checksum = calculateCRC(fileData);
        out.writeUTF(checksum); // Send checksum to server
        out.writeInt(fileData.length); // Send file length
        out.write(fileData); // Send file data

        System.out.println("File uploaded successfully.");
    }

    private static void downloadFile(String filename, DataInputStream in, DataOutputStream out) throws IOException {
        out.writeUTF("get " + filename); // Request the file from the server

        String response = in.readUTF();
        if (response.equals("File not found")) {
            System.out.println("File not found on server.");
            return;
        }

        int fileLength = in.readInt();
        byte[] fileData = new byte[fileLength];
        in.readFully(fileData); // Read file data

        String checksum = in.readUTF();
        String calculatedChecksum = calculateCRC(fileData);
        if (!checksum.equals(calculatedChecksum)) {
            System.out.println("Checksum mismatch, file corrupted.");
        } else {
            try (FileOutputStream fos = new FileOutputStream(CLIENT_DIRECTORY + filename)) {
                fos.write(fileData);
            }
            System.out.println("File downloaded and saved.");
        }
    }

    private static String calculateCRC(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return Long.toHexString(crc.getValue()); // Return checksum as hex
    }
}
