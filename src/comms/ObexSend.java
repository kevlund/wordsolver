package comms;

import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import solver.Grid;
import gui.Main;

public class ObexSend implements Runnable {
	private Main main;

	public ObexSend(Main main) {
		this.main = main;
	}

	public void run() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
			DataOutputStream dos = new DataOutputStream(baos);
			// First the number of words/grids
			int num = main.sp.solvingListModel.size();
			dos.writeInt(num);
			System.out.println("Buffered num " + num);
			// Now the grids
			Grid tempGrid = main.sp.solvingGrid.clone();
			for (int i = 0; i < num; i++) {
				dos.writeChars(tempGrid.toTileLetters());
				tempGrid.removeLetters(main.sp.solvingSolution.get(i));
			}
			System.out.println("Buffered grids");
			// Next the length of each word
			for (Collection<Point> word : main.sp.solvingSolution) {
				dos.writeInt(word.size());
			}
			// Then write out the positions
			for (Collection<Point> word : main.sp.solvingSolution) {
				for (Point c : word) {
					dos.writeInt(c.x * 13 + c.y);
				}
			}
			System.out.println("Buffered words");
			dos.flush();
			// Open connection
			System.out.println("Connecting to " + Main.scanner.serverURL);
			Connection connection = Connector.open(Main.scanner.serverURL);
			ClientSession cs = (ClientSession) connection;
			HeaderSet hs = cs.createHeaderSet();
			cs.connect(hs);
			System.out.println("Connected");
			Operation po = cs.put(hs);
			OutputStream os = po.openOutputStream();
			System.out.println("Sending");
			os.write(baos.toByteArray());
			os.flush();
			System.out.println("Sent");
			// Close up and say good bye
			dos.close();
			os.close();
			po.close();
			cs.disconnect(null);
			connection.close();
			System.out.println("Disconnected");
		} catch (IOException e) {
			System.err.println(e.toString());
		}
	}
}
