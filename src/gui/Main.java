package gui;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import comms.ObexReceive;
import comms.ObexSend;
import comms.Scanner;
import dictionary.Dictionary;
import ocr.OCRThread;
import solver.SolveThread;

public class Main extends javax.swing.JApplet implements WindowListener {
	static final long serialVersionUID = 1;
	public static final Dictionary dict = new Dictionary("res/wordlist.txt");
	private JTabbedPane tabbedPane;
	public static PicturePanel pp;
	public static Scanner scanner;
	public OCRPanel op;
	public SolutionPanel sp;
	public Thread ocrThread;
	public Thread solveThread;
	public ObexReceive obr;
	public Thread obsThread;
	

	public Main() {
		super();
		pp = new PicturePanel(this);
		op = new OCRPanel(this);
		sp = new SolutionPanel(this);
		scanner = new Scanner(this);
		ObexReceive.start();
		try {
			ocrThread = new Thread(new OCRThread(this));
			ocrThread.setName("ocrThread");
			solveThread = new Thread(new SolveThread(this));
			solveThread.setName("solveThread");
			obsThread = new Thread(new ObexSend(this));
			// GUI inits
			setSize(800, 600);
			JPanel topPanel = new JPanel();
			topPanel.setLayout(new BorderLayout());
			getContentPane().add(topPanel);
			tabbedPane = new JTabbedPane();
			tabbedPane.addTab("Image Selection", pp.imageSelectionPanel);
			tabbedPane.setMnemonicAt(0, KeyEvent.VK_I);
			tabbedPane.addTab("Character Recognition", op.ocrPanel);
			tabbedPane.setMnemonicAt(1, KeyEvent.VK_C);
			tabbedPane.addTab("Solution Generation", sp.solvingPanel);
			tabbedPane.setMnemonicAt(2, KeyEvent.VK_S);
			topPanel.add(tabbedPane, BorderLayout.CENTER);
			this.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		JFrame.setDefaultLookAndFeelDecorated(false);
		JFrame frame = new JFrame("WordSolver PC");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Main inst = new Main();
		frame.getContentPane().add(inst);
		frame.addWindowListener(inst);
		((JComponent) frame.getContentPane()).setPreferredSize(inst.getSize());
		frame.pack();
		frame.setVisible(true);
	}

	public void resetOCR() {
		System.out.println("Aborting OCR.");
		ocrThread.interrupt();
		try {
			ocrThread.join();
		} catch (InterruptedException e) {
			System.err.println("Interrupted waiting for OCR to clean up. " + e);
		}
		try {
			ocrThread = new Thread(new OCRThread(this));
			ocrThread.setName("ocrThread");
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	public void resetSolver() {
		System.out.println("Aborting Solve.");
		solveThread.interrupt();
		try {
			solveThread.join();
		} catch (InterruptedException e) {
			System.err.println("Interrupted waiting for solver to clean up. " + e);
		}
		solveThread = new Thread(new SolveThread(this));
		solveThread.setName("solveThread");
	}

	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	public void windowClosing(WindowEvent e) {
		System.out.println("Shutting down bluetooth servers.");
		ObexReceive.stop();
	}

	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
	}
}
