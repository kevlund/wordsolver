package gui;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.border.LineBorder;
import javax.swing.event.MouseInputAdapter;

public class OCRTile extends JLabel {
	static final long serialVersionUID = 1;

	WritableRaster wr;
	Main main;

	public OCRTile(Main main) {
		super();
		this.main = main;
		setOpaque(true);
		setMinimumSize(new Dimension(14, 10));

		MyListener myListener = new MyListener();
		addMouseListener(myListener);
		addMouseMotionListener(myListener);
	}

	public void setWR(WritableRaster wr) {
		this.wr = wr;
		repaint();
	}

	public void paint(Graphics g) {
		if (wr != null) {
			RescaleOp op = new RescaleOp(new float[] { -1.0f, -1.0f, -1.0f, 1f }, new float[] { 255, 255, 255, 0 }, null);
			BufferedImage img = new BufferedImage(ColorModel.getRGBdefault(), op.filter(wr, null), false, null);
			setIcon(new ImageIcon(img.getScaledInstance(20, 20, Image.SCALE_FAST)));
		} else {
			super.setText("");
			super.setIcon(null);
			super.setBorder(null);
		}
		super.paint(g);
	}

	private class MyListener extends MouseInputAdapter {
		public void mouseClicked(MouseEvent e) {
			OCRTile source = (OCRTile) e.getSource();

			String correction = (String) JOptionPane.showInputDialog(source, "Enter correct letter:", "Correction", JOptionPane.QUESTION_MESSAGE, source
					.getIcon(), null, "");
			if (correction != null && correction.length() == 1) {
				source.setText(correction);
				source.setBorder(new LineBorder(Color.GREEN, 1, false));
				
				// Have to restart solving now
				main.resetSolver();
				main.op.startButton.setEnabled(true);
			}
		}
	}
}