package upem.jarret.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.upem.net.tcp.http.HTTPException;
import fr.upem.net.tcp.http.HTTPHeader;
import fr.upem.net.tcp.http.HTTPReader;

public class ServerJarret {

	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final Set<SelectionKey> selectedKeys;
	private static final int BUFFER_SIZE = 4096;
	private boolean is_shutdown = false;
	private static final int TIMEOUT = 300;
	private static Thread thread_server;
	private TaskGiver tg = TaskGiver.create();
	private final Charset ASCII_CHARSET = Charset.forName("ASCII");
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	public ServerJarret(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		selectedKeys = selector.selectedKeys();
	}

	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		System.out.println("server started");
		Object monitor = new Object();
		while (!Thread.interrupted()) {
			synchronized (monitor) {
				if (is_shutdown) {
					synchronized (thread_server) {
						thread_server.notify();
					}
					break;
				}
			}

			selector.select(TIMEOUT);
			processSelectedKeys();
			selectedKeys.clear();
		}
	}

	private void processSelectedKeys() throws IOException {
		for (SelectionKey key : selectedKeys) {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
			if (key.isValid() && key.isWritable()) {
				doWrite(key);
			}
			if (key.isValid() && key.isReadable()) {
				doRead(key);
			}
		}
	}

	private void doAccept(SelectionKey key) throws IOException {
		// only the ServerSocketChannel is register in OP_ACCEPT
		SocketChannel sc;
		try {
			sc = serverSocketChannel.accept();
		} catch (IOException e) {
			System.err.println("Can't accept Client");
			return;
		}
		if (sc == null)
			return; // In case, the selector gave a bad hint
		sc.configureBlocking(false);
		sc.register(selector, SelectionKey.OP_READ);

	}

	private void doRead(SelectionKey key) {
		SocketChannel client = (SocketChannel) key.channel();
		ByteBuffer buff = ByteBuffer.allocate(5);
		HTTPReader reader = new HTTPReader(client, buff);
		HTTPHeader header;
		try {
			header = reader.readHeader();
			System.out.println(header.getFields());
			String response = header.getResponse();
			System.out.println(response);
			if (response.contains("GET Task HTTP/1.1")) {
				String job = tg.giveJobByPriority();
				ByteBuffer bb = generateAnswer(job);
				// bb.flip();
				key.attach(bb);
				key.interestOps(SelectionKey.OP_WRITE);
			} else if (response.contains("POST Answer HTTP/1.1")) {
				ByteBuffer buff2 = reader.readBytes(header.getContentLength());
				buff2.flip();
				String content = UTF8_CHARSET.decode(buff2).toString();
				System.out.println(content);
				boolean validJson = TaskSavior.saveJob(content);
				System.err.println(validJson);
				buff2.clear();

				if (validJson) {
					tg.taskGiven(content);
					buff2.put(ASCII_CHARSET.encode("HTTP/1.1 200 OK\r\n\r\n"));
				} else {
					buff2.put(ASCII_CHARSET
							.encode("HTTP/1.1 400 Bad Request\r\n\r\n"));
				}
				key.attach(buff2);
				key.interestOps(SelectionKey.OP_WRITE);

			}
		} catch (IOException e) {
			// e.printStackTrace();
			System.err.println("Can't read from the client");
			silentlyClose(client);

		}

	}

	private void silentlyClose(SocketChannel client) {
		try {
			client.close();
		} catch (IOException e1) {
			// do Nothing
		}
	}

	public boolean isValidJSON(final String json) {
		boolean valid = true;
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.readTree(json);
		} catch (JsonParseException jpe) {
			valid = false;
		} catch (IOException ioe) {
			valid = false;
		}

		return valid;
	}

	private void doWrite(SelectionKey key) {
		SocketChannel client = (SocketChannel) key.channel();
		ByteBuffer bb = (ByteBuffer) key.attachment();
		bb.flip();
		try {
			client.write(bb);
			bb.compact();
			if (bb.hasRemaining()) {
				key.interestOps(SelectionKey.OP_READ);
			}
		} catch (IOException e) {
			System.err.println("Can't write to the client");
		}
	}

	private ByteBuffer generateAnswer(String job) throws HTTPException {
		Map<String, String> fields = new HashMap<>();
		String response = "HTTP/1.1 200 OK";
		fields.put("Content-Type", "application/json; charset=utf-8");
		fields.put("Content-Length", String.valueOf(job.length()));
		HTTPHeader headerAnswer = HTTPHeader.create(response, fields);
		ByteBuffer buff = ByteBuffer.allocate(BUFFER_SIZE);
		System.out.println(job);
		buff.put(ASCII_CHARSET.encode(headerAnswer.toString()));
		buff.put(UTF8_CHARSET.encode(job));
		return buff;

	}

	private void shutdown() throws InterruptedException, IOException {
		is_shutdown = true;
		synchronized (thread_server) {
			while (thread_server.isAlive()) {
				thread_server.wait();
			}
			serverSocketChannel.close();
		}
	}

	private void shutdownNow() throws IOException {
		thread_server.interrupt();
		serverSocketChannel.close();
	}

	private static void usage() {
		System.out.println("ServerJarret <port>");

	}

	public static void main(String[] args) throws IOException,
			InterruptedException {
		if (args.length != 1) {
			usage();
			return;
		}
		ServerJarret serverJarret = new ServerJarret(Integer.parseInt(args[0]));

		thread_server = new Thread(() -> {
			try {
				serverJarret.launch();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		thread_server.start();

		try (Scanner scan = new Scanner(System.in)) {
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				if (line.toLowerCase().equals("shutdown")) {
					System.out.println("Server is shuting down !");
					serverJarret.shutdown();
					System.out.println("Server stopped");
					return;

				}
				if (line.toLowerCase().equals("shutdown now")) {
					System.out.println("Server is shuting down Now !");
					serverJarret.shutdownNow();
					System.out.println("Server stopped");
					return;
				}
			}
		}
	}
}
