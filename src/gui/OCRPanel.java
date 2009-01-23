package gui;

import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

public class OCRPanel implements ActionListener {
	private Main main;
	public JPanel ocrPanel;
	public JPanel ocrGrid;
	public JButton startButton;

	public OCRPanel(Main main) {
		this.main = main;
		ocrPanel = new JPanel();
		ocrPanel.setLayout(null);
		final Insets insets = ocrPanel.getInsets();
		ocrGrid = new JPanel();
		GridLayout ocrGridLayout = new GridLayout(9, 13);
		ocrGridLayout.setColumns(13);
		ocrGridLayout.setRows(9);
		ocrGridLayout.setHgap(2);
		ocrGridLayout.setVgap(2);
		ocrGrid.setLayout(ocrGridLayout);
		ocrGrid.setBounds(insets.left + 10, insets.top + 20, 780, 480);
		ocrGrid.setBorder(new LineBorder(new java.awt.Color(0, 0, 0), 1, false));
		for (int i = 0; i < 117; i++) {
			ocrGrid.add(new OCRTile(main));
		}
		ocrPanel.add(ocrGrid);
		startButton = new JButton("Start solving");
		startButton.setEnabled(false);
		startButton.setBounds(insets.left + 300, insets.top + 510, 200, 50);
		startButton.setActionCommand("start");
		startButton.addActionListener(this);
		ocrPanel.add(startButton);
	}

	public void blankTiles() {
		for (int i = 0; i < 117; i++) {
			((OCRTile) ocrGrid.getComponent(i)).setWR(null);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if ("start".equals(e.getActionCommand())) {
			startButton.setEnabled(false);
			main.solveThread.start();
		}
	}
}
