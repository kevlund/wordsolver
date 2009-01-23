package comms;

import gui.Main;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import javax.obex.ServerRequestHandler;
import javax.obex.SessionNotifier;

public class ObexReceive {
	private static class ObexReceiveThread extends ServerRequestHandler implements Runnable {
		private final String myServiceName = "WordSolverObex";
		private final UUID myUUID = new UUID("3737", true);
		private final String myURL = "btgoep://localhost:" + myUUID + ";name=" + myServiceName + ";authenticate=false;master=false;encrypt=false";
		protected SessionNotifier sn;

		public void run() {
			try {
				Main.pp.serverStatusField.setText("Waiting for a client to connect");
				sn = (SessionNotifier) Connector.open(myURL);
				sn.acceptAndOpen(this);
			} catch (IOException e) {
				if (e.getMessage() == "Service Revoked") {
					System.out.println("Bluetooth Service Revoked");
				} else {
					System.err.println(e);
				}
			}
		}

		public int onConnect(HeaderSet request, HeaderSet reply) {
			Main.pp.serverStatusField.setText("The client has created an OBEX session");
			return ResponseCodes.OBEX_HTTP_OK;
		}

		public void onDisconnect(HeaderSet req, HeaderSet resp) {
			Main.pp.serverStatusField.setText("The client has disconnected the OBEX session");
			start();
		}

		public int onPut(Operation op) {
			Main.pp.serverStatusField.setText("Receving file");
			try {
				java.io.InputStream is = op.openInputStream();
				File f = new File("res/" + (String) op.getReceivedHeaders().getHeader(HeaderSet.NAME));
				FileOutputStream fos = new FileOutputStream(f);
				byte b[] = new byte[4000];
				int len;
				int recevied = 0;
				while (is.available() > 0 && (len = is.read(b)) > 0) {
					recevied += len;
					fos.write(b, 0, len);
				}
				fos.close();
				is.close();
				op.close();
				sn.close();
				System.gc();
				Main.pp.haveImage(f);
				run();
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
			return ResponseCodes.OBEX_HTTP_OK;
		}
	}
	private static Thread t;
	private static ObexReceiveThread ort;
	
	public static void start() {
		ort = new ObexReceiveThread();
		t = new Thread(ort);
		t.start();
	}
	
	public static void stop() {
		try {
			ort.sn.close();
		} catch (IOException e) {
				System.out.println(e.toString());
		}
	}
}
