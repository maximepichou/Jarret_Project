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

import com.esotericsoftware.minlog.Log;
import com.fasterxml.jackson.core.JsonProcessingException;

import fr.upem.logger.MyLogger;
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
	private final TaskGiver taskGiver;
	private final TaskSavior taskSavior;
	private final Charset ASCII_CHARSET = Charset.forName("ASCII");
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	public ServerJarret(int port, int comeBackInSeconds, String pathJobs, String pathLogs)
			throws IOException {
		Log.setLogger(new MyLogger(pathLogs));
		Log.TRACE();
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		selectedKeys = selector.selectedKeys();
		taskGiver = TaskGiver.create(comeBackInSeconds);
		taskSavior = new TaskSavior(pathJobs);
	}
	
	
	/**
	 * Start the server on non-blocking mode.
	 * @throws IOException
	 */
	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		Log.info("Server started");
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
	
	/**
	 * Compute selected keys.
	 */
	private void processSelectedKeys(){
		for (SelectionKey key : selectedKeys) {
			if (key.isValid() && key.isAcceptable()) {
				try{
				doAccept(key);
				}catch(IOException e){
					//Do nothing
				}
			}
			if (key.isValid() && key.isWritable()) {
				doWrite(key);
			}
			if (key.isValid() && key.isReadable()) {
				doRead(key);
			}
		}
	}
	
	/**
	 * Accept new client.
	 * @param key - the future client key.
	 * @throws IOException - if I/O error occurs.
	 */
	private void doAccept(SelectionKey key) throws IOException {
		SocketChannel sc;
		try {
			sc = serverSocketChannel.accept();
			Log.info("New client accepted");
		} catch (IOException e) {
			Log.warn("Can't accept Client");
			return;
		}
		if (sc == null)
			return; // In case, the selector gave a bad hint
		sc.configureBlocking(false);
		sc.register(selector, SelectionKey.OP_READ);

	}

	/**
	 * Read the client and prepare answer for the client.
	 * @param key - The key that contains the channel to read.
	 */
	private void doRead(SelectionKey key) {
		SocketChannel client = (SocketChannel) key.channel();
		ByteBuffer buff = ByteBuffer.allocate(5);
		HTTPReader reader = new HTTPReader(client, buff);
		HTTPHeader header;
		try {
			header = reader.readHeader();
			String response = header.getResponse();
			if (response.contains("GET Task HTTP/1.1")) {
				String job = null;
				try {
					job = taskGiver.giveJobByPriority();
				} catch (JsonProcessingException jpe) {
					Log.error("Error while giving task to the client !");
					silentlyClose(client);
					return;
				}
				ByteBuffer bb = generateAnswer(job);
				key.attach(bb);
				key.interestOps(SelectionKey.OP_WRITE);
			} else if (response.contains("POST Answer HTTP/1.1")) {
				ByteBuffer buff2 = reader.readBytes(header.getContentLength());
				buff2.flip();
				String content = UTF8_CHARSET.decode(buff2).toString();
				Log.info("Receiving new complete task");
				boolean validJson = taskSavior.saveJob(content);
				Log.info("The new task is valid JSON ? " + validJson);
				buff2.clear();

				if (validJson) {
					Log.info("Sending answer with code 200 OK\n\n");
					buff2.put(ASCII_CHARSET.encode("HTTP/1.1 200 OK\r\n\r\n"));
				} else {
					Log.info("Sending answer with code 400 Bad Request cause of non valid JSON\n\n");
					buff2.put(ASCII_CHARSET
							.encode("HTTP/1.1 400 Bad Request\r\n\r\n"));
				}
				key.attach(buff2);
				key.interestOps(SelectionKey.OP_WRITE);

			}
		} catch (IOException e) {
			Log.error("Can't read from the client");
			silentlyClose(client);

		}

	}

	/**
	 * Write an answer to the client.
	 * @param key - The key of the client to write to him.
	 */
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
			Log.error("Can't write to the client");
		}
	}
	
	/**
	 * Close the connection with the client if an error occurs.
	 * @param client - SocketChannel to close.
	 */
	private void silentlyClose(SocketChannel client) {
		try {
			client.close();
		} catch (IOException e) {
			// Do Nothing
		}
	}

	/**
	 * Write into a buffer the answer for the client with HTTP Header and the job to solve.
	 * @param job - String of the job in JSON format that the client have to solve.
	 * @return ByteBuffer of the answer to send to the client.
	 * @throws HTTPException
	 */
	private ByteBuffer generateAnswer(String job) throws HTTPException {
		Map<String, String> fields = new HashMap<>();
		String response = "HTTP/1.1 200 OK";
		fields.put("Content-Type", "application/json; charset=utf-8");
		fields.put("Content-Length", String.valueOf(job.length()));
		HTTPHeader headerAnswer = HTTPHeader.create(response, fields);
		ByteBuffer buff = ByteBuffer.allocate(BUFFER_SIZE);
		buff.put(ASCII_CHARSET.encode(headerAnswer.toString()));
		buff.put(UTF8_CHARSET.encode(job));
		return buff;

	}
	
	/**
	 * Stop new connection with client and shutdown server when all clients disconnected.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void shutdown() throws InterruptedException, IOException {
		is_shutdown = true;
		synchronized (thread_server) {
			while (thread_server.isAlive()) {
				thread_server.wait();
			}
			serverSocketChannel.close();
		}
	}

	/**
	 * Close immediately all connections with clients and shutdown the server.
	 * @throws IOException
	 */
	private void shutdownNow() throws IOException {
		thread_server.interrupt();
		serverSocketChannel.close();
	}
	
	/**
	 * Display information about the server.
	 */
	private void info(){
		Log.info(" There is " + (selector.keys().size()-1) + " client(s) connected");
		taskGiver.info();
	}

	public static void main(String[] args) throws IOException,
			InterruptedException {

		JarretConfigurator jConfigurator = JarretConfigurator.create();

		ServerJarret serverJarret = new ServerJarret(jConfigurator.getPort(),
				jConfigurator.getTimeToSleep(), jConfigurator.getPathJobs(), jConfigurator.getPathLogs());

		thread_server = new Thread(() -> {
			try {
				serverJarret.launch();
			} catch (IOException e) {
				Log.error("Cannot start server !");
			}
		});
		thread_server.start();

		try (Scanner scan = new Scanner(System.in)) {
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				if (line.equals("SHUTDOWN")) {
					Log.warn("Server is shuting down !");
					serverJarret.shutdown();
					Log.warn("Server stopped");
					return;

				}
				if (line.equals("SHUTDOWN NOW")) {
					Log.warn("Server is shuting down Now !");
					serverJarret.shutdownNow();
					Log.warn("Server stopped");
					return;
				}
				if (line.equals("INFO")) {
					serverJarret.info();
				}
			}
		}
	}
}
