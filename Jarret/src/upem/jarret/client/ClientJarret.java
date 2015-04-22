package upem.jarret.client;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

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
		String request2 = "GET / HTTP/1.1\r\n" + "Host: www.w3.org\r\n" + "\r\n";
		String request = "GET Task HTTP/1.1\r\n" + "Host: "
				+ serverAddress.getHostString() + "\r\n" + "\r\n";
		buff.put(ASCII_CHARSET.encode(request));
		return buff;
	}

	public void sendPacket(ByteBuffer buff) throws IOException {
		buff.flip();
		sc.write(buff);

		buff.clear();
	}
	
	public void writeAnswerHTTP(String response){
		
		
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			usage();
			return;
		}
		String clientID = args[0];
		String adress = args[1];
		int port = Integer.valueOf(args[2]);

		while (true) {
			System.out.println("Initialisation du client " + clientID+ "\nConnexion au server " + adress+" sur le port "+port);
			ClientJarret cJarret = new ClientJarret(clientID, adress, port);
			System.out.println("Envoie du paquet de demande de tâche");
			cJarret.sendPacket(cJarret.getRequestPacket());
			System.out.println("Lecture de la réponse du serveur");
			HTTPReader reader = new HTTPReader(cJarret.sc, cJarret.buff);
			System.out.println("Lecture de l'entete HTTP");
			HTTPHeader header = reader.readHeader();
			System.out.println(header.getFields());
			ByteBuffer content = reader.readBytes(header.getContentLength());
			//System.out.println(header.getCharset().decode(cJarret.buff));
			ClientTask cTask = ClientTask.create(cJarret.buff, header);
			int sleep;
			if ((sleep = cTask.haveToSleep()) != 0) {
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

			try {
				System.out.println("Doing Work");
				cTask.doWork();
			} catch (ClassNotFoundException | IllegalAccessException
					| InstantiationException e) {
				System.err.println("Error while loading URL class");
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			System.out.println("Creating AnswerPacket");
			ByteBuffer buff = cJarret.createAnswerPacket(cTask);
			buff.flip();
			System.out.println("Sending Packet");
			cJarret.sc.write(buff);
		}

	}

	private ByteBuffer createAnswerPacket(ClientTask cTask) throws HTTPException {
		Map<String,String> fields = new HashMap<>();
		String response = "POST Answer HTTP/1.1";
		fields.put("Host", serverAddress.getHostString());
		fields.put("Content-Type", "application/json");
		fields.put("Content-Length", String.valueOf(84+serverAddress.getHostString().length()));
		HTTPHeader postHeader = HTTPHeader.create(response, fields);
		ByteBuffer buff = ByteBuffer.allocate(BUFFER_SIZE);
		buff.put(ASCII_CHARSET.encode(postHeader.toString()));
		buff.put(UTF8_CHARSET.encode(cTask.convertToJSON(clientId)));
		return buff;
		
	}

}
