package upem.jarret.worker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;

public class HTTPReader {

	private final Charset ASCII_CHARSET = Charset.forName("ASCII");
	private final SocketChannel sc;
	private final ByteBuffer buff;
	private static final byte CR = 13;
	private static final byte LF = 10;

	public HTTPReader(SocketChannel sc, ByteBuffer buff) {
		this.sc = sc;
		this.buff = buff;
	}

	/**
	 * @return The ASCII string terminated by CRLF
	 *         <p>
	 *         The method assume that buff is in write mode and leave it in
	 *         write-mode The method never reads from the socket as long as the
	 *         buffer is not empty
	 * @throws IOException
	 *             HTTPException if the connection is closed before a line could
	 *             be read
	 */
	public String readLineCRLF() throws IOException {
		
		StringBuilder sb = new StringBuilder();
		boolean justReadCR = false;
		boolean finished = false;
		while(true){
			buff.flip();
			/*if(!buff.hasRemaining()){
				buff.compact();
				sc.read(buff);
				buff.flip();
			}*/
			while(buff.hasRemaining() && !finished){
				byte current = buff.get();
				if(current == LF && justReadCR){
					finished = true;
				}
				justReadCR = (current == CR);
			}
			ByteBuffer tmp = buff.duplicate();
			tmp.flip();
			sb.append(ASCII_CHARSET.decode(tmp));
			buff.compact();
			if(finished){
				break;
			}
			if(sc.read(buff) == -1){
				//throw new IOException("Connection close before reading");
				HTTPException.ensure(false, "Connection close before reading");
			}
		}
		sb.delete(sb.length()-2, sb.length());
		return sb.toString();
		
	}

	/**
	 * @return The HTTPHeader object corresponding to the header read
	 * @throws IOException
	 *             HTTPException if the connection is closed before a header
	 *             could be read if the header is ill-formed
	 */
	public HTTPHeader readHeader() throws IOException {
		String statusLine = readLineCRLF();
		HashMap<String, String> map = new HashMap<>();
		
		while(true){
			String line = readLineCRLF();
			if(line.length() == 0){
				break;
			}
			int index_of_separator = line.indexOf(":");
			String s;
			if(null != (s = map.putIfAbsent(line.substring(0, index_of_separator), line.substring(index_of_separator+2)))){
				s.concat("; "+line);
				map.put(statusLine, s);
			}
		}
		return HTTPHeader.create(statusLine, map);
	}

	/**
	 * @param size
	 * @return a ByteBuffer in write-mode containing size bytes read on the
	 *         socket
	 * @throws IOException
	 *             HTTPException is the connection is closed before all bytes
	 *             could be read
	 */
	public ByteBuffer readBytes(int size) throws IOException {
		if(size <= 0){
			throw new IllegalArgumentException("Negative size of content-length");
		}
		ByteBuffer bb = ByteBuffer.allocate(size);
		buff.flip();
		if(buff.remaining() > size){
			int old_limit = buff.limit();
			buff.limit(buff.position()+bb.remaining());
			bb.put(buff);
			buff.limit(old_limit);
			buff.compact();
		}else{
			bb.put(buff);
			buff.compact();
			while(bb.hasRemaining()){
				if(-1 == sc.read(bb)){
					HTTPException.ensure(false, "Connection close before reading");
				}
			}
		}
		
		return bb;
	}

	public static void main(String[] args) throws IOException {
		Charset charsetASCII = Charset.forName("ASCII");
		String request = "GET / HTTP/1.1\r\n" + "Host: www.w3.org\r\n" + "\r\n";
		SocketChannel sc = SocketChannel.open();
		sc.connect(new InetSocketAddress("www.w3.org", 80));
		sc.write(charsetASCII.encode(request));
		ByteBuffer bb = ByteBuffer.allocate(50);
		HTTPReader reader = new HTTPReader(sc, bb);
		System.out.println(reader.readLineCRLF());
		System.out.println(reader.readLineCRLF());
		System.out.println(reader.readLineCRLF());
		sc.close();

		/*bb = ByteBuffer.allocate(50);
		sc = SocketChannel.open();
		sc.connect(new InetSocketAddress("www.w3.org", 80));
		reader = new HTTPReader(sc, bb);
		sc.write(charsetASCII.encode(request));
		System.out.println(reader.readHeader());
		sc.close();*/

		bb = ByteBuffer.allocate(50);
		sc = SocketChannel.open();
		sc.connect(new InetSocketAddress("www.w3.org", 80));
		reader = new HTTPReader(sc, bb);
		sc.write(charsetASCII.encode(request));
		HTTPHeader header = reader.readHeader();
		System.out.println(header);
		ByteBuffer content = reader.readBytes(header.getContentLength());
		content.flip();
		System.out.println(header.getCharset().decode(content));
		sc.close();

		/*bb = ByteBuffer.allocate(50);
		request = "GET / HTTP/1.1\r\n" + "Host: www.u-pem.fr\r\n" + "\r\n";
		sc = SocketChannel.open();
		sc.connect(new InetSocketAddress("www.u-pem.fr", 80));
		reader = new HTTPReader(sc, bb);
		sc.write(charsetASCII.encode(request));
		header = reader.readHeader();
		System.out.println(header);
		content = reader.readChunks();
		content.flip();
		System.out.println(header.getCharset().decode(content));
		sc.close();*/
	}
}