import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public final class Server {
	private final int serverPort;
	private ServerSocket socket;
	private DataOutputStream toClientStream;
	private BufferedReader fromClientStream;

	public Server(int serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * Creates a socket + binds to the desired server-side port #.
	 *
	 * @throws {@link IOException} if the port is already in use.
	 */
	public void bind() throws IOException {
		socket = new ServerSocket(serverPort);
		System.out.println("Server bound and listening to port " + serverPort);
	}

	/**
	 * Waits for a client to connect, and then sets up stream objects for communication
 	 * in both directions.
	 *
	 * @return {@code true} if the connection is successfully established.
	 * @throws {@link IOException} if the server fails to accept the connection.
	 */
	public boolean acceptFromClient() throws IOException {
		Socket clientSocket;
		try {
			clientSocket = socket.accept();
		} catch (SecurityException e) {
			System.out.println("The security manager intervened; your config is very wrong. " + e);
			return false;
		} catch (IllegalArgumentException e) {
			System.out.println("Probably an invalid port number. " + e);
			return false;
		}

		toClientStream = new DataOutputStream(clientSocket.getOutputStream());
		fromClientStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		return true;
	}

	/**
	 * Loops forever, reading a line from the client, printing it to the screen,
	 * then echoing it back.
	 *
	 * @throws (@link IOException} if a communication error occurs.
	 */
	public void echoLoop() throws IOException {
		while (true) {
			String blob = fromClientStream.readLine();
			System.out.println(blob);
			toClientStream.writeBytes(blob + '\n');
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
		try {
			server.bind();
			if (server.acceptFromClient()) {
				server.echoLoop();
			} else {
				System.out.println("Error accepting client connection.");
			}
		} catch (IOException e) {
			System.out.println("Error communicating with client. aborting. Details: " + e);
		}
	}
}

