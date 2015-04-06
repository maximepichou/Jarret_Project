package upem.jarret.worker;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.LinkedList;

//http://www.journaldev.com/2324/jackson-json-processing-api-in-java-example-tutorial
//http://repo1.maven.org/maven2/com/fasterxml/jackson/
//http://docs.oracle.com/javase/7/docs/api/java/net/URLClassLoader.html#URLClassLoader(java.net.URL[],%20java.lang.ClassLoader)
public class ClientJarret {
	private final String clientID;
	private InetSocketAddress serverAddress;
	private final SocketChannel sc;
	private static final int BUFFER_SIZE = 4096;
	private ByteBuffer buff;
	public static final Charset ASCII_CHARSET = Charset.forName("ASCII");

	public ClientJarret(String clientID, String adressServer, int portServer) throws IOException {
		this.clientID = clientID;
		sc = SocketChannel.open();
		serverAddress = new InetSocketAddress(adressServer, portServer);
		//connection to server
		sc.connect(serverAddress);
	}

	private static void usage() {
		System.out.println("ClientJarret clientID Adress Port");

	}
	
	public ByteBuffer getRequestPacket(){
		buff = ByteBuffer.allocate(BUFFER_SIZE);
		String request = "GET Task HTTP/1.1\r\n" + "Host: "+serverAddress.getHostString() + "\r\n" + "\r\n";
		buff.put(ASCII_CHARSET.encode(request));
		return buff;
	}

	public void sendPacket(ByteBuffer buff) throws IOException{
    	buff.flip();
    	sc.write(buff);
    	
    	buff.clear();
	}
	
	
	
	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			usage();
			return;
		}
		String clientID = args[0];
		String adress = args[1];
		int port = Integer.valueOf(args[2]);

		while(true){
			ClientJarret cJarret = new ClientJarret(clientID, adress, port);
			cJarret.sendPacket(cJarret.getRequestPacket());
			HTTPReader reader = new HTTPReader(cJarret.sc, cJarret.buff);
			HTTPHeader header = reader.readHeader();
			System.out.println(header.getFields());
			cJarret.buff.clear();
			cJarret.buff = reader.readBytes(header.getContentLength());
			ClientTask cTask = ClientTask.create(cJarret.buff, header);
			int sleep;
			if((sleep = cTask.haveToSleep()) != 0){
				long timeSlept = System.currentTimeMillis();
				while(System.currentTimeMillis() - timeSlept < sleep*1000){
					try {
						Thread.sleep(sleep * 1000 - (System.currentTimeMillis() - timeSlept));
					} catch (InterruptedException e) {
						//Do nothing
					}
				}
				continue;
			}
				try {
					cTask.doWork();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			
		}
		
		
		
	}

}
