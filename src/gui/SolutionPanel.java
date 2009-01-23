package gui;

import gui.Main;
import solver.Grid;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class SolutionPanel implements ListSelectionListener, ActionListener {
	private Main main;
	public JPanel solvingPanel;
	public JPanel solvingStats;
	public JPanel solvingSeq;
	public JTextArea solvingSeqText;
	public Thread solvingThread;
	public JTextField highestScore;
	public JTextField currentRemain;
	public JTextField currentWC;
	public JTextField fewestRemain;
	public JTextField greatestWC;
	public DefaultListModel solvingListModel;
	public JList solvingList;
	public JScrollPane solvingScrollPane;
	public JButton sendButton;
	public Grid solvingGrid;
	public ArrayList<Collection<Point>> solvingSolution;
	public JPanel solvingGridPanel;
	private int nFewestRemain;
	private int nGreatestWC;

	public SolutionPanel(Main main) {
		this.main = main;
		nFewestRemain = Integer.MAX_VALUE;
		nGreatestWC = 0;
		solvingPanel = new JPanel();
		solvingPanel.setLayout(null);
		final Insets insets = main.op.ocrPanel.getInsets();
		// Word List
		solvingListModel = new DefaultListModel();
		solvingList = new JList(solvingListModel);
		solvingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		solvingList.addListSelectionListener(this);
		solvingScrollPane = new JScrollPane(solvingList);
		solvingScrollPane.setBounds(insets.left + 410, insets.top + 20, 370, 540);
		solvingScrollPane.setBorder(new BevelBorder(1));
		solvingPanel.add(solvingScrollPane);
		// Grid
		solvingGridPanel = new JPanel();
		solvingGridPanel.setBounds(insets.left + 20, insets.top + 20, 370, 250);
		solvingGridPanel.setBorder(new BevelBorder(1));
		GridLayout solvingGridLayout = new GridLayout(9, 13);
		solvingGridLayout.setHgap(1);
		solvingGridLayout.setVgap(1);
		solvingGridPanel.setLayout(solvingGridLayout);
		for (int i = 0; i < 117; i++) {
			JLabel temp = new JLabel();
			temp.setAlignmentX(JLabel.CENTER_ALIGNMENT);
			temp.setBorder(new LineBorder(Color.BLACK, 1, false));
			solvingGridPanel.add(temp);
		}
		solvingPanel.add(solvingGridPanel);
		// Stats
		solvingStats = new JPanel();
		solvingStats.setBounds(insets.left + 20, insets.top + 300, 370, 180);
		solvingStats.setBorder(new BevelBorder(1));
		solvingStats.setLayout(new BoxLayout(solvingStats, BoxLayout.Y_AXIS));
		highestScore = new JTextField("Current/highest score:");
		highestScore.setEditable(false);
		highestScore.setOpaque(false);
		solvingStats.add(highestScore);
		currentRemain = new JTextField("Current remaining letters:");
		currentRemain.setEditable(false);
		currentRemain.setOpaque(false);
		solvingStats.add(currentRemain);
		currentWC = new JTextField("Current words found:");
		currentWC.setEditable(false);
		currentWC.setOpaque(false);
		solvingStats.add(currentWC);
		JTextField pad = new JTextField();
		pad.setEditable(false);
		pad.setOpaque(false);
		solvingStats.add(pad);
		fewestRemain = new JTextField("Fewest remaining letters:");
		fewestRemain.setEditable(false);
		fewestRemain.setOpaque(false);
		solvingStats.add(fewestRemain);
		greatestWC = new JTextField("Most words found:");
		greatestWC.setEditable(false);
		greatestWC.setOpaque(false);
		solvingStats.add(greatestWC);
		solvingPanel.add(solvingStats);
		sendButton = new JButton("Send to phone");
		sendButton.setEnabled(false);
		sendButton.setBounds(insets.left + 20, insets.top + 510, 370, 50);
		sendButton.setActionCommand("start");
		sendButton.addActionListener(this);
		solvingPanel.add(sendButton);
	}

	public void processChain(Collection<? extends Collection<Point>> chain, Grid grid, int score) {
		// Update best solution set
		updateGrid(grid.clone());
		solvingSolution = new ArrayList<Collection<Point>>();
		solvingSolution.addAll(chain);
		// Update GUI list
		Grid tempGrid = grid.clone();
		solvingListModel.removeAllElements();
		for (Collection<Point> word : chain) {
			solvingListModel.addElement(tempGrid.pointsToString(word));
			tempGrid.removeLetters(word);
		}
		highestScore.setText("Current/highest score: " + score);
		currentRemain.setText("Current remaining letters:" + tempGrid.cellsRemaining());
		currentWC.setText("Current words found:" + chain.size());
		if (tempGrid.cellsRemaining() < nFewestRemain) {
			nFewestRemain = tempGrid.cellsRemaining();
			fewestRemain.setText("Fewest remaining letter:" + nFewestRemain);
		}
		if (chain.size() > nGreatestWC) {
			nGreatestWC = chain.size();
			greatestWC.setText("Most words found:" + nGreatestWC);
		}
	}

	public void updateGrid(Grid in) {
		this.solvingGrid = in;
		for (int i = 0; i < 117; i++) {
			JLabel thisCell = (JLabel) this.solvingGridPanel.getComponent(i);
			thisCell.setText("" + in.getCell(new Point(i / 13, i % 13)));
			thisCell.setBorder(new LineBorder(Color.BLACK, 1, false));
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false && solvingList.getSelectedIndex() > -1) {
			int wordIndex = solvingList.getSelectedIndex();
			// Compute the grid state at that letter
			Grid tempGrid = solvingGrid.clone();
			for (int i = 0; i < wordIndex; i++) {
				tempGrid.removeLetters(solvingSolution.get(i));
			}
			// Update the grid representation
			for (int i = 0; i < 117; i++) {
				JLabel thisCell = (JLabel) this.solvingGridPanel.getComponent(i);
				try {
					thisCell.setText("" + tempGrid.getCell(new Point(i / 13, i % 13)));
					thisCell.setBorder(new LineBorder(Color.LIGHT_GRAY, 1, false));
				} catch (NullPointerException e2) {
					thisCell.setText(null);
					thisCell.setBorder(null);
				}
			}
			// Highlight the correct letters with a shiny border
			Collection<Point> wordPoints = solvingSolution.get(wordIndex);
			int red = 255;
			for (Point letter : wordPoints) {
				((JLabel) this.solvingGridPanel.getComponent(letter.x * 13 + letter.y)).setBorder(new LineBorder(new Color(red, 100, 100), 3, false));
				if (red > 100) {
					red -= 20;
				}
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		if ("start".equals(e.getActionCommand())) {
			sendButton.setEnabled(false);
			main.solveThread.interrupt();
			main.obsThread.start();
		}
	}
}
