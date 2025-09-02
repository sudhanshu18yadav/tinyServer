import java.io.*;
import java.util.*;
import java.net.*;


public class tinyServer{
	private static Integer port = 4200;
	private static ServerSocket serverSocket;

	public tinyServer(Integer port){
			try{
			serverSocket = new ServerSocket(port);
			while(true){
				ExtractData(serverSocket.accept());
			}
			} catch(IOException e){
				e.printStackTrace();
			}
	}


	public static void main(String[] args){

		new tinyServer(port);

		while(true){
			try{
				Thread.sleep(1000);
				System.out.println("running...");
			} catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}

	private void ExtractData(Socket socket){

	}
}