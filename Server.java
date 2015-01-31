import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class Server {
	private final int serverPort;
	private ServerSocket socket;
	private Socket mClientSocket;
	private DataOutputStream toClientStream;
	private BufferedReader fromClientStream;
	private HashMap<String,String> mRedirects;

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
		server.loadRedirects();
		server.bind();
		// loop so server will begin listening again on the port once terminating a connection
		while(true) {
			try{
				if (server.acceptFromClient()) {
					ArrayList<String> x = server.getRequestHeader();
					// split the first line of the request
					String[] requests = x.get(0).split(" ");
					// process the request
					server.processRequest(requests[0], requests[1]);
				} else {
					System.out.println("Error accepting client connection.");
				}
			} catch (IOException e) {
				System.out.println("Error communicating with client. aborting. Details: " + e);
			}
			// close sockets and buffered readers
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
			if (strLine.isEmpty()){
				break;
			} else {
				strHeader.add(strLine);
			}
		}
		return strHeader;
	}

	public void processRequest(String httpVerb, String resourcePath){
		System.out.println("Verb: " + httpVerb + " Resource: " + resourcePath);
		File resource = new File("www" + resourcePath);

		if (resource.isDirectory()){
			// TODO: return 404
			return;
		}

		// if the requested file exists
		if (resource.exists()) {
			if (httpVerb.equals("GET")) {
				this.get(resource);
			} else if (httpVerb.equals("HEAD")) {
				this.head(resource);
			} else {
				// TODO: return 403 as we do not handle POST/PUT/DELETE
			}
		} else if (hasRedirect(resource)) {  //if the file exists in the redirects
			// TODO: redirect to proper path (301)

		} else { // no file or redirect
			// TODO: 404 error
		}

	}

	public boolean hasRedirect(File resource){
		// TODO: Add check to see if resource path exists in mRedirects
		return false;
	}

	public void loadRedirects(){
		mRedirects = new HashMap<String, String>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader("www/redirect.defs"));
			String line;
			while((line = reader.readLine()) != null){
				String[] parts = line.split(" ");
				mRedirects.put(parts[0], parts[1]);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getRedirect(String resourcePath){
		// TODO: Add code to read from mRedirects
		return "";
	}

	public String buildHeader(int status, String phrase, String contentType, long length){
		String strHeader = "HTTP/1.1 " + status + " " + phrase + "\r\n";
		strHeader += "Date: " + getServerDate() + "\r\n";
		strHeader += "Content-Length: " + length + "\r\n";
		strHeader += "Content-Type: " + contentType + "\r\n\r\n";
		// TODO: set close connection header

		return strHeader;
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void get(File resource){
		String contentType = getContentType(resource.getName());
		String header = buildHeader(200, "OK", contentType, resource.length());

		sendResponse(header, resource);
	}

	// Figure out what MIME type to return
	public String getContentType(String filePath){
		if(filePath.contains(".html")){
			return "text/html";
		}
		else if (filePath.contains(".txt")){
			return "text/plain";
		}
		else if (filePath.contains(".pdf")){
			return "application/pdf";
		}
		else if (filePath.contains(".png")) {
			return "image/png";
		}
		else if (filePath.contains(".jpeg")){
			return "image/jpeg";
		}
		else {
			return "invalid content type";
		}
	}

	public void head(File resource){
		// TODO: Handle head request (We know at this point that the resource exists)
	}

	public void serverCleanup(){
		try {
			fromClientStream.close();
			toClientStream.close();
			mClientSocket.close();
		} catch (IOException e){
			System.out.println(e);
		}
	}

}

