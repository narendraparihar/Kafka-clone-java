import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible
    // when running tests.
    System.err.println("Logs from your program will appear here!");

    // Uncomment this block to pass the first stage

    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    int port = 9092;
    try {
      serverSocket = new ServerSocket(port);
      serverSocket.setReuseAddress(true);

      clientSocket = serverSocket.accept();
      InputStream in = clientSocket.getInputStream();

      // Read at least 12 bytes: message_size(4) + api_key(2) + api_version(2) +
      // correlation_id(4)
      byte[] buffer = new byte[12];
      int bytesRead = in.read(buffer);
      if (bytesRead < 12) {
        System.out.println("Incomplete header");
        return;
      }

      ByteBuffer bb = ByteBuffer.wrap(buffer);
      bb.getInt(); // message_size
      short apiKey = bb.getShort(); // 18 for ApiVersions
      short apiVersion = bb.getShort(); // should be 4
      int correlationId = bb.getInt();
      OutputStream out = clientSocket.getOutputStream();
      if (apiKey == 18 && apiVersion == 3 || apiVersion == 4) {
        short errorCode = 0;
        int apiCount = 3;

        short[][] supportedApis = {
            { 0, 0, 2 },
            { 1, 0, 1 },
            { 18, 0, 4 }
        };

        int bodySize = 2 + 4 + (apiCount * 6) + 4;

        // Total message size is correlationId (4) + bodySize
        int messageSize = 4 + bodySize;

        // Allocate buffer for messageSize (4) + messageSize bytes
        ByteBuffer response = ByteBuffer.allocate(4 + messageSize);

        // Put message size (doesn't include these 4 bytes)
        response.putInt(messageSize);
        // Put correlation_id
        response.putInt(correlationId);
        // Put error_code
        response.putShort(errorCode);
        // Put number of APIs
        response.putInt(apiCount);
        for (short[] api : supportedApis) {
          response.putShort(api[0]); // api_key
          response.putShort(api[1]); // min_version
          response.putShort(api[2]); // max_version
        }

        response.putInt(0); // throttle_time_ms

        out.write(response.array());
        System.out.println("Sent ApiVersions v3 response");
      }

    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      try {
        if (clientSocket != null) {
          clientSocket.close();
        }
      } catch (IOException e) {
        System.out.println("IOException: " + e.getMessage());
      }
    }
  }
}
