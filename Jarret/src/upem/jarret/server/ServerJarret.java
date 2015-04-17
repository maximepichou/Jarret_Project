package upem.jarret.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;


public class ServerJarret {
	
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final Set<SelectionKey> selectedKeys;
	private final static int TIMEOUT = 300;
	
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
		while (!Thread.interrupted()) {
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
		
		
	}

	private void doWrite(SelectionKey key) throws IOException {
		
	}

	public static void main(String[] args) throws NumberFormatException,
			IOException {
		JsonData.create();
		new ServerJarret(Integer.parseInt(args[0])).launch();

	}
}
