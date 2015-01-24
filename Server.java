import sun.security.util.Length;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.SocketHandler;

public final class Server {
	private final int serverPort;
	private ServerSocket socket;
	private Socket mClientSocket;
	private DataOutputStream toClientStream;
	private BufferedReader fromClientStream;

	public void serverCleanup(){
		try {
			fromClientStream.close();
			toClientStream.close();
			mClientSocket.close();
		} catch (IOException e){
			System.out.println(e);
		}
	}
	public static void main(String argv[]) {
		Map<String, String> flags = Utils.parseCmdlineFlags(argv);
		if (!flags.containsKey("--serverPort")) {
			System.out.println("usage: Server --serverPort=12345");
			System.exit(-1);
		}

		int serverPort = -1;
		try {
			serverPort = Integer.parseInt(flags.get("--serverPort"));
		} catch (NumberFormatException e) {
			System.out.println("Invalid port number! Must be an integer.");
			System.exit(-1);
		}

		Server server = new Server(serverPort);
		server.bind();
		while(true) {
			try{
				if (server.acceptFromClient()) {
					// Parse Header
					ArrayList<String> x = server.getRequestHeader();
					String[] requests = x.get(0).split(" ");
					server.processRequest(requests[0], requests[1]);
					//Look for file in filesystem
//				server.serveFile(requests[1]);
					// Build response header
					// send response
				} else {
					System.out.println("Error accepting client connection.");
				}
			} catch (IOException e) {
				System.out.println("Error communicating with client. aborting. Details: " + e);
			}

			server.serverCleanup();
		}
	}

	public Server(int serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * Creates a socket + binds to the desired server-side port #.
	 *
	 * @throws {@link IOException} if the port is already in use.
	 */
	public void bind()  {
		try{
			socket = new ServerSocket(serverPort);
			System.out.println("Server bound and listening to port " + serverPort);
		} catch (IOException e) {
			System.out.println("Error binding to port " + serverPort);
		}
	}

	/**
	 * Waits for a client to connect, and then sets up stream objects for communication
	 * in both directions.
	 *
	 * @return {@code true} if the connection is successfully established.
	 * @throws {@link IOException} if the server fails to accept the connection.
	 */
	public boolean acceptFromClient() throws IOException {
		try {
			mClientSocket = socket.accept();
		} catch (SecurityException e) {
			System.out.println("The security manager intervened; your config is very wrong. " + e);
			return false;
		} catch (IllegalArgumentException e) {
			System.out.println("Probably an invalid port number. " + e);
			return false;
		}


		toClientStream = new DataOutputStream(mClientSocket.getOutputStream());
		fromClientStream = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream()));
		return true;
	}

	public ArrayList<String> getRequestHeader () throws IOException {
		ArrayList<String> strHeader = new ArrayList<String>();
		String strLine = null;
		while (true){
			strLine = fromClientStream.readLine();
			if (strLine.equals("")){
				break;
			} else {
				strHeader.add(strLine);
			}
		}
		return strHeader;
	}

	public void processRequest(String httpVerb, String resourcePath){
		System.out.println("Verb: " + httpVerb + " Resource: " + resourcePath);
		if (httpVerb.equals("GET")){
			this.get(resourcePath);
		} else if (httpVerb.equals("HEAD")) {
			this.head(resourcePath);
		} else {

		}
	}

	public void serveFile(String filePath){
		System.out.println(filePath);

	}

	public String buildHeader(int status, String phrase, String contentType, long length){
		String strHeader = "HTTP/1.1 " + status + " " + phrase + "\r\n";
		strHeader += "Date: " + getServerDate() + "\r\n";
		strHeader += "Content-Length: " + length + "\r\n";
		strHeader += "Content-Type: " + contentType + "\r\n\r\n";

		return strHeader;
//		HTTP/1.1 200 OK
//		Connection: close
//		Date: Tue, 09 Aug 2011 15:44:04 GMT
//		Server: Apache/2.2.3 (CentOS)
//				Last-Modified: Tue, 09 Aug 2011 15:11:03 GMT
//		Content-Length: 6821
//		Content-Type: text/html
	}

	// http://stackoverflow.com/questions/7707555/getting-date-in-http-format-in-java
	public String getServerDate(){
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(calendar.getTime());
	}

	public void sendResponse(String header, File file){

		try {
			toClientStream.writeBytes(header);
			if (file != null) {
				byte[] buffer = new byte[1000];
				FileInputStream in = new FileInputStream(file);
				while (in.available() > 0) {
					toClientStream.write(buffer, 0, in.read(buffer));
				}
			}
//			toClientStream.writeBytes(content);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void get(String resourcePath){
		System.out.println("laskjdglakdsj" + resourcePath);
		File resource = new File("www" + resourcePath);
		if (resource.exists()){
			String header = buildHeader(200, "OK", "text/html", resource.length());

//			try {
//				BufferedReader br = new BufferedReader(new FileReader("www" + resourcePath));
//				String line = null;
//				while ((line = br.readLine()) != null) {
//					header += line;
//				}
				sendResponse(header, resource);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		} else {
			// TODO: Look in defs file for redirect
			String header = buildHeader(404, "Not Found", "text/html", 0 );
			sendResponse(header, null);
		}

	}

	public void head(String resourcePath){

	}

	public void return403(){

	}

}

