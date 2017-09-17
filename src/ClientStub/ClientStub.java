package ClientStub;

import java.io.*;
import java.net.*;

/**
 * VUnetID: rhm270
 **/
/*
 wolkje-89.cs.vu.nl
 5378
 */
class ClientStub
{
	BufferedReader in;
	PrintStream out;
	boolean exit;
	String name;
	String serverReply = null;

	ClientStub() {
		in   = new BufferedReader(new InputStreamReader(System.in));
		out  = new PrintStream(System.out);
		exit = false;
	}

	boolean sayHello(BufferedReader srvIn, PrintWriter srvOut) {
		name = null;
		String message = null;
		out.printf("Input your chat nickname (alphanummeric only!) here:");

		try {
			name  = in.readLine();
			message = "HELLO-FROM "+ name;
			srvOut.println(message);
		} catch (IOException e) {
			return false;
		}

		boolean read = false;
		while (!read){
			try{
				if(srvIn.ready()) {
					serverReply = srvIn.readLine();
					read = true;
				}
			}catch(Exception e){
				return false;
			}
		}	

		if(serverReply.equals("HELLO "+name)){   
			out.printf("You're succesfully logged in as %s\n", name);
		}else{
			if(serverReply.equals("BUSY")){
				out.println("Server has reached its userlimit");
				return false;
			}else{
				if(serverReply.equals("IN-USE")){
					out.println("Username is already in use");
				}
				if(serverReply.equals("BAD-RQST-BODY")){
					out.println("Username does not meet the servers conditions");
				}	
				while (!sayHello(srvIn,srvOut)){
					out.println("try again");
				}
			}	
		}
		return true;
	}

	void handleInput(BufferedReader srvIn, PrintWriter srvOut)
			throws IOException {
		String line = in.readLine();
		if(line == null) {
			out.println("could not read line from user");
			return;
		}

		/* supported commands:
       !who          - perform a WHO request to the server
       @<user> <msg> - send <msg> to <user>
       !exit         - stop the program */
		if(line.equals("!who")) {
			requestUserList(srvIn, srvOut);
		}
		else if(line.startsWith("@")) {
			sendMessage(line, srvIn, srvOut);
		}
		else if(line.equals("!exit")) {
			exit = true;
		}
		else {
			out.println("unknown command");
		}
	}

	void requestUserList(BufferedReader srvIn, PrintWriter srvOut)
			throws IOException {
		srvOut.println("WHO");
		String response  = srvIn.readLine();
		if(response.startsWith("WHO-OK")){
			String userList = response.substring(7);
			out.println(userList);
		}else{
			out.println("recieved format not correct while recieving UserList");
		}
	}

	void sendMessage(String line, BufferedReader srvIn, PrintWriter srvOut) 
			throws IOException {
		String message = "SEND " + line.substring(1);
		srvOut.println(message);

		String newMessage = srvIn.readLine();
		if(!newMessage.equals("SEND-OK")){
			if (srvIn.equals("BAD-RQST-BODY")){
				out.println("Message is not delivered due the wrong format");
			}
			if (srvIn.equals("UNKNOWN")){
				out.println("Message is not deliverd because the user doesn't exist");
			}
		}
	}

	void recvMessage(BufferedReader srvIn) 
			throws IOException {
		String newMessage = srvIn.readLine();

		if(newMessage.startsWith("DELIVERY")){
			newMessage = newMessage.substring(9);
			out.println(newMessage);
		}else{
			if(!newMessage.equals("SEND-OK")){
				out.printf("The folllowing server output could not be parsed:%s", newMessage);
			}
		}
	}

	void start(String[] argv) {
		if(argv.length != 2) {
			out.println("usage: java ClientStub <server> <port>");
			return;//exit to main
		}
		int portNumber = Integer.parseInt(argv[1]);
		Socket sock          = null;
		BufferedReader srvIn = null;
		PrintWriter srvOut   = null;

		try {
			sock = 		new Socket(argv[0], portNumber);
			srvOut = 	new PrintWriter(sock.getOutputStream(), true);
			srvIn = 	new BufferedReader(new InputStreamReader(sock.getInputStream()));
		} catch (Exception e1) {
			out.println("Error occured while setting up the socket");
			out.println(e1.getMessage());
			System.exit(1);
		}
		if(!sayHello(srvIn, srvOut)) {
			try {
				sock.close();
				out.println("Connection will be closed, Server association failed");
			} catch (Exception e) {
				out.println("Error while system closed the socket:");
				out.println(e.getMessage());
			}
			System.exit(1);
		}

		while(!exit) {
			try {
				if(in.ready()) {
					//out.printf("\b%s:",name);
					handleInput(srvIn, srvOut);
				}
				if(srvIn.ready()) {
					recvMessage(srvIn);
				}
				Thread.sleep(200);
			}
			catch(Exception e) {
				out.printf("Error occured");
				e.printStackTrace();
			}
		}

		try{
			sock.close();
		}catch(Exception e){
			out.println("Error when closing the connection during exit");
			out.println(e.getMessage());
		}
		System.exit(0);
	}

	public static void main(String[] argv) {
		new ClientStub().start(argv);
	}
}