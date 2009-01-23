package gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.event.MouseInputAdapter;

public class PicturePanel {
	public class SelectionArea extends JLabel {
		static final long serialVersionUID = 1;

		private class MyListener extends MouseInputAdapter {
			public void mouseReleased(MouseEvent e) {
				nCorners++;
				// See if we've start to outline a new section (drunk maybe?)
				if (nCorners == 5) {
					// Reset OCR & solver threads
					nCorners = 1;
					main.resetOCR();
					main.resetSolver();
				}
				// Add the new corner click to the array
				corners[nCorners - 1][0] = e.getX();
				corners[nCorners - 1][1] = e.getY();
				// See if that was the final corner
				if (imgFile != null && nCorners == 4) {
					nCorners = 1;
					main.ocrThread.start();
					imageStatusField.setText("Starting OCR\t(" + imgFile.getName() + ")");
				}
				repaint();
			}
		}
		public int[][] corners;
		private int nCorners;

		public SelectionArea() {
			super();
			MyListener myListener = new MyListener();
			addMouseListener(myListener);
			addMouseMotionListener(myListener);
			corners = new int[4][2];
			nCorners = 0;
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setXORMode(Color.white);
			for (int i = 0; i < nCorners; i++) {
				g.drawOval(corners[i][0] - 5, corners[i][1] - 5, 10, 10);
			}
		}
	}
	private Main main;
	volatile public File imgFile;
	public JPanel imageSelectionPanel;
	public SelectionArea imagePreview;
	public JTextField imageStatusField;
	public JTextField serverStatusField;

	public PicturePanel(Main main) {
		this.main = main;
		imageSelectionPanel = new JPanel();
		imageSelectionPanel.setLayout(null);
		final Insets insets = imageSelectionPanel.getInsets();
		imagePreview = new SelectionArea();
		imagePreview.setBorder(new LineBorder(new java.awt.Color(0, 0, 0), 1, false));
		imagePreview.setIcon(null);
		imagePreview.setBounds(insets.left + 80, insets.top + 20, 640, 480);
		imageSelectionPanel.add(imagePreview);
		// Sever status
		JLabel serverStatus = new JLabel("Server Status:");
		serverStatus.setBounds(insets.left + 80, insets.top + 530, 100, 20);
		imageSelectionPanel.add(serverStatus);
		serverStatusField = new JTextField();
		serverStatusField.setOpaque(false);
		serverStatusField.setEditable(false);
		serverStatusField.setBounds(insets.left + 200, insets.top + 530, 460, 20);
		imageSelectionPanel.add(serverStatusField);
		// Image status
		JLabel imageStatus = new JLabel("Image Status:");
		imageStatus.setBounds(insets.left + 80, insets.top + 510, 100, 20);
		imageSelectionPanel.add(imageStatus);
		imageStatusField = new JTextField();
		imageStatusField.setOpaque(false);
		imageStatusField.setEditable(false);
		imageStatusField.setBounds(insets.left + 200, insets.top + 510, 460, 20);
		imageSelectionPanel.add(imageStatusField);
		 haveImage(new File("res/DSC00048.JPG"));
	}

	public void haveImage(File f) {
		imgFile = f;
		imageStatusField.setText("Loading " + f.getName() + " ...");
		Toolkit tk = Toolkit.getDefaultToolkit();
		Image img = tk.getImage(f.getAbsolutePath()).getScaledInstance(640, 480, Image.SCALE_FAST);
		MediaTracker mt = new MediaTracker(main);
		mt.addImage(img, 1);
		try {
			mt.waitForAll();
		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
		}
		imagePreview.setIcon(new ImageIcon(img));
		imageStatusField.setText("Loaded " + f.getName());
	}
}
