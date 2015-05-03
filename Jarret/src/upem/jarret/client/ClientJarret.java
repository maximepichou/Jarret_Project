package upem.jarret.client;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import fr.upem.net.tcp.http.HTTPException;
import fr.upem.net.tcp.http.HTTPHeader;
import fr.upem.net.tcp.http.HTTPReader;

//http://www.journaldev.com/2324/jackson-json-processing-api-in-java-example-tutorial
//http://repo1.maven.org/maven2/com/fasterxml/jackson/
//http://docs.oracle.com/javase/7/docs/api/java/net/URLClassLoader.html#URLClassLoader(java.net.URL[],%20java.lang.ClassLoader)
public class ClientJarret {
	private final String clientId;
	private InetSocketAddress serverAddress;
	private final SocketChannel sc;
	private static final int BUFFER_SIZE = 4096;
	private ByteBuffer buff;
	public static final Charset ASCII_CHARSET = Charset.forName("ASCII");
	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	public ClientJarret(String clientID, String adressServer, int portServer)
			throws IOException {
		this.clientId = clientID;
		sc = SocketChannel.open();
		serverAddress = new InetSocketAddress(adressServer, portServer);
		// connection to server
		sc.connect(serverAddress);
	}

	private static void usage() {
		System.out.println("ClientJarret clientID Adress Port");

	}

	public ByteBuffer getRequestPacket() {
		buff = ByteBuffer.allocate(BUFFER_SIZE);
		// String request2 = "GET / HTTP/1.1\r\n" + "Host: www.w3.org\r\n" +
		// "\r\n";
		String request = "GET Task HTTP/1.1\r\n" + "Host: "
				+ serverAddress.getHostString() + "\r\n" + "\r\n";
		buff.put(ASCII_CHARSET.encode(request));
		return buff;
	}

	public void sendPacket(ByteBuffer buffer) throws IOException {
		buffer.flip();
		sc.write(buffer);
		buffer.clear();
	}

	private ByteBuffer createAnswerPacket(ClientTask cTask)
			throws HTTPException {
		Map<String, String> fields = new HashMap<>();
		String response = "POST Answer HTTP/1.1";
		fields.put("Host", serverAddress.getHostString());
		fields.put("Content-Type", "application/json; charset=utf-8");
		String jsonAnswer = cTask.convertToJsonString(clientId);
		fields.put("Content-Length", String.valueOf(jsonAnswer.length()));
		HTTPHeader postHeader = HTTPHeader.create(response, fields);
		ByteBuffer buff = ByteBuffer.allocate(BUFFER_SIZE);
		buff.put(ASCII_CHARSET.encode(postHeader.toString()));
		buff.put(UTF8_CHARSET.encode(jsonAnswer));
		return buff;

	}

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			usage();
			return;
		}
		String clientID = args[0];
		String adress = args[1];
		int port = Integer.valueOf(args[2]);

		System.out.println("\n\nClient initialization "
				+ "\nConnecting to the server " + adress + " on " + port + ".");
		ClientJarret cJarret = new ClientJarret(clientID, adress, port);

		while (true) {
			System.out.println("Asking for new task to the server.");
			cJarret.sendPacket(cJarret.getRequestPacket());
			HTTPReader reader = new HTTPReader(cJarret.sc, cJarret.buff);
			HTTPHeader header = reader.readHeader();
			int contentLength = header.getContentLength();
			reader.readBytes(contentLength);
			if (!"application/json".equals(header.getContentType())) {
				throw new IllegalArgumentException(
						"This is not JSON Content-Type");
			}
			String content = header.getCharset().decode(cJarret.buff)
					.toString();
			ClientTask cTask = ClientTask.create(content);
			int sleep;
			if ((sleep = cTask.haveToSleep()) != 0) {
				System.out.println("No Task Available");
				System.out.println("Retry in " + cTask.haveToSleep()
						+ " seconds");
				long timeSlept = System.currentTimeMillis();
				while (System.currentTimeMillis() - timeSlept < sleep * 1000) {
					try {
						Thread.sleep(sleep * 1000
								- (System.currentTimeMillis() - timeSlept));
					} catch (InterruptedException e) {
						// Do nothing
					}
				}
				continue;
			}
			System.out.println("New Task available :\nTask " + cTask.getTask()
					+ " of Job " + cTask.getJobId() + "\nWorkerURL : "
					+ cTask.getWorkerURL() + "\nWorkerClassName : "
					+ cTask.getWorkerClassName() + "\nWorkerClassVersion : "
					+ cTask.getWorkerVersion());
			try {
				cTask.doWork();
			} catch (ClassNotFoundException | IllegalAccessException
					| InstantiationException e) {
				System.err.println("Error while loading URL class");
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			System.out.println("Sending Packet Answer");
			ByteBuffer buff = cJarret.createAnswerPacket(cTask);
			cJarret.sendPacket(buff);
			cJarret.buff.clear();
			reader = new HTTPReader(cJarret.sc, cJarret.buff);
			header = reader.readHeader();
			System.out.println("Server returns answer with code : "
					+ header.getCode());
		}

	}

}
