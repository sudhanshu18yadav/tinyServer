import java.io.*;
import java.util.*;
import java.net.*;


public class tinyServer{
	private static Integer port = 4200;
	private static ServerSocket serverSocket;


//// Common Helper

	public static final String
            MIME_PLAINTEXT = "text/plain",
            MIME_HTML = "text/html",
            MIME_DEFAULT_BINARY = "application/octet-stream",
            MIME_XML = "text/xml";

    public static final String
            HTTP_OK = "200 OK",
            HTTP_REDIRECT = "301 Moved Permanently",
            HTTP_FORBIDDEN = "403 Forbidden",
            HTTP_NOTFOUND = "404 Not Found",
            HTTP_BADREQUEST = "400 Bad Request",
            HTTP_INTERNALERROR = "500 Internal Server Error",
            HTTP_NOTIMPLEMENTED = "501 Not Implemented";

 private static java.text.SimpleDateFormat gmtFrmt;

    static {
        gmtFrmt = new java.text.SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private static Hashtable theMimeTypes = new Hashtable();

    static {
        StringTokenizer st = new StringTokenizer(
                "css		text/css " +
                        "js			text/javascript " +
                        "htm		text/html " +
                        "html		text/html " +
                        "txt		text/plain " +
                        "asc		text/plain " +
                        "gif		image/gif " +
                        "jpg		image/jpeg " +
                        "jpeg		image/jpeg " +
                        "png		image/png " +
                        "mp3		audio/mpeg " +
                        "m3u		audio/mpeg-url " +
                        "pdf		application/pdf " +
                        "doc		application/msword " +
                        "ogg		application/x-ogg " +
                        "zip		application/octet-stream " +
                        "exe		application/octet-stream " +
                        "class		application/octet-stream ");
        while (st.hasMoreTokens())
            theMimeTypes.put(st.nextToken(), st.nextToken());
    }

//////

	public tinyServer(Integer port){
		Runnable run =()->{
			try{
			serverSocket = new ServerSocket(port);
			while(true){
				new HTTPSession(serverSocket.accept());
			}
			} catch(IOException e){
				e.printStackTrace();
			}
		};
		Thread t1 = new Thread(run);
		t1.setDaemon(true);
		t1.start();
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

	private class HTTPSession implements Runnable{
		private Socket socket;
		
		public HTTPSession(Socket s){
			socket = s;
			Thread t = new Thread(this);
			t.setDaemon(true);
			t.start();
		}
		public void run(){
			try{
				int bufferSize = 8192;
				byte[] buffer = new byte[bufferSize];
				InputStream stream = socket.getInputStream();
				int end = stream.read(buffer, 0, bufferSize);
				if(end <= 0) return;

				ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
				BufferedReader reader = new BufferedReader(new InputStreamReader(bais));
				Properties header = new Properties();
				Properties params = new Properties();
				Properties files = new Properties();
				Properties pre = new Properties();

				decodeHeader(reader, header, params, pre);
			} catch(IOException e){
				e.printStackTrace();
			}
		}

        private void decodeParms(String parms, Properties p) throws InterruptedException {
            if (parms == null)
                return;

            StringTokenizer st = new StringTokenizer(parms, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                p.put(
                        decodePercent((sep >= 0) ? e.substring(0, sep) : e).trim(),
                        (sep >= 0) ? decodePercent(e.substring(sep + 1)) : ""
                );
            }
        }

		private void decodeHeader(BufferedReader reader, Properties headers, Properties params, Properties pre) throws IOException{
			String str = reader.readLine();
			StringTokenizer st = new StringTokenizer(str);

			while(st.hasMoreElements()){
				System.out.println("--> "+st.nextElement());
			}
		}


		private void sendError(String status, String msg) throws InterruptedException {
            sendResponse(status, MIME_PLAINTEXT, null, new ByteArrayInputStream(msg.getBytes()));
            throw new InterruptedException();
        }

        /**
         * Sends given response to the socket.
         */
        private void sendResponse(String status, String mime, Properties header, InputStream data) {
            try {
                if (status == null)
                    throw new Error("sendResponse(): Status can't be null.");

                OutputStream out = socket.getOutputStream();
                PrintWriter pw = new PrintWriter(out);
                pw.print("HTTP/1.0 " + status + " \r\n");

                if (mime != null)
                    pw.print("Content-Type: " + mime + "\r\n");

                if (header == null || header.getProperty("Date") == null)
                    pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");

                if (header != null) {
                    Enumeration e = header.keys();
                    while (e.hasMoreElements()) {
                        String key = (String) e.nextElement();
                        String value = header.getProperty(key);
                        pw.print(key + ": " + value + "\r\n");
                    }
                }

                pw.print("\r\n");
                pw.flush();

                if (data != null) {
                    byte[] buff = new byte[2048];
                    while (true) {
                        int read = data.read(buff, 0, 2048);
                        if (read <= 0)
                            break;
                        out.write(buff, 0, read);
                    }
                }
                out.flush();
                out.close();
                if (data != null)
                    data.close();
            } catch (IOException ioe) {
                // Couldn't write? No can do.
                try {
                    socket.close();
                } catch (Throwable t) {
                }
            }
        }

	}
}