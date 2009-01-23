package ocr;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RescaleOp;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.LineBorder;
import javax.swing.event.MouseInputAdapter;
import java.util.Arrays;
import org.joone.engine.FullSynapse;
import org.joone.engine.LinearLayer;
import org.joone.engine.Monitor;
import org.joone.engine.NeuralNetEvent;
import org.joone.engine.NeuralNetListener;
import org.joone.engine.SigmoidLayer;
import org.joone.engine.SoftmaxLayer;
import org.joone.engine.learning.TeachingSynapse;
import org.joone.io.MemoryInputSynapse;
import org.joone.net.NeuralNet;
import solver.Grid;
import solver.WordBuilder;

public class OCRTrainer extends javax.swing.JApplet implements NeuralNetListener {
	private class SelectionArea extends JLabel {
		static final long serialVersionUID = 1;

		private class MyListener extends MouseInputAdapter {
			public void mouseReleased(MouseEvent e) {
				nCorners++;
				// See if we've start to outline a new section (drunk maybe?)
				if (nCorners == 5) {
					// Reset OCR & solver threads
					nCorners = 1;
				}
				// Add the new corner click to the array
				cs[nCorners - 1][0] = e.getX();
				cs[nCorners - 1][1] = e.getY();
				if (nCorners == 4) {
					drawOCRTiles();
					saveCorners();
				}
				repaint();
			}
		}
		public int[][] cs;
		private int nCorners;

		public SelectionArea() {
			super();
			MyListener myListener = new MyListener();
			addMouseListener(myListener);
			addMouseMotionListener(myListener);
			cs = new int[4][2];
			nCorners = 0;
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setXORMode(Color.white);
			for (int i = 0; i < nCorners; i++) {
				g.drawOval(cs[i][0] - 5, cs[i][1] - 5, 10, 10);
			}
		}
	}

	private class OCRTile extends JLabel {
		static final long serialVersionUID = 1;
		WritableRaster wr;

		public OCRTile() {
			super();
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
				}
			}
		}
	}
	static final long serialVersionUID = 1;
	private File f;
	private JTabbedPane jtp;
	private JPanel isp;
	private SelectionArea sa;
	private JPanel ocrp;
	private JPanel ocrg;

	public static void main(String[] args) throws Exception {
		// Corner selector
		// chooseCorners("res/crt1.jpg");
		// Average tile hue algorithm
		String[] files = { "res/crt4", "res/crt5", "res/crt6"};
		// bandHueSpace(calcAvgHues(files));
		// Train & serialise networks
		new OCRTrainer().train(files);
	}

	public static void chooseCorners(String f) {
		JFrame.setDefaultLookAndFeelDecorated(false);
		JFrame frame = new JFrame("WordSolver PC");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		OCRTrainer inst = new OCRTrainer(new File(f));
		frame.getContentPane().add(inst);
		((JComponent) frame.getContentPane()).setPreferredSize(inst.getSize());
		frame.pack();
		frame.setVisible(true);
	}

	private OCRTrainer() {
	}

	private OCRTrainer(File f) {
		super();
		this.f = f;
		drawPanels();
		setImage();
		// Inits
		setSize(800, 600);
		JPanel tp = new JPanel();
		tp.setLayout(new BorderLayout());
		getContentPane().add(tp);
		tp.add(jtp, BorderLayout.CENTER);
		this.setVisible(true);
	}

	private void drawPanels() {
		final Insets insets = getInsets();
		// Selection area
		isp = new JPanel();
		isp.setLayout(null);
		sa = new SelectionArea();
		sa.setBorder(new LineBorder(new java.awt.Color(0, 0, 0), 1, false));
		sa.setIcon(null);
		sa.setBounds(insets.left + 80, insets.top + 20, 640, 480);
		isp.add(sa);
		// Preview
		ocrp = new JPanel();
		ocrp.setLayout(null);
		ocrg = new JPanel();
		GridLayout ocrGridLayout = new GridLayout(9, 13);
		ocrGridLayout.setColumns(13);
		ocrGridLayout.setRows(9);
		ocrGridLayout.setHgap(2);
		ocrGridLayout.setVgap(2);
		ocrg.setLayout(ocrGridLayout);
		ocrg.setBounds(insets.left + 10, insets.top + 20, 780, 480);
		ocrg.setBorder(new LineBorder(new java.awt.Color(0, 0, 0), 1, false));
		for (int i = 0; i < 117; i++) {
			ocrg.add(new OCRTile());
		}
		ocrp.add(ocrg);
		// Set up tabs
		jtp = new JTabbedPane();
		jtp.addTab("Corners", isp);
		jtp.addTab("Tiles", ocrp);
	}

	private void setImage() {
		Toolkit tk = Toolkit.getDefaultToolkit();
		Image img = tk.getImage(f.getAbsolutePath()).getScaledInstance(640, 480, Image.SCALE_FAST);
		MediaTracker mt = new MediaTracker(this);
		mt.addImage(img, 1);
		try {
			mt.waitForAll();
		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
		}
		sa.setIcon(new ImageIcon(img));
	}

	private void drawOCRTiles() {
		WritableRaster gridRaster = ImageManagement.Loader.loadImgToRaster(f.getAbsolutePath(), this);
		WritableRaster[] tileRasters = ImageManagement.makeRasterTiles(sa.cs, gridRaster);
		for (int i = 0; i < 117; i++) {
			// Do image processing and set the picture
			ImageManagement.extWhite(tileRasters[i]);
			ImageManagement.remHighlights(tileRasters[i]);
			ImageManagement.magicWand(tileRasters[i]);
			tileRasters[i] = ImageManagement.autoCrop(tileRasters[i]);
			// Display preview tile
			((OCRTile) ocrg.getComponent(i)).setWR(tileRasters[i]);
		}
	}

	private void saveCorners() {
		String fileH = f.getPath();
		fileH = fileH.substring(0, fileH.lastIndexOf('.')) + ".cor";
		System.out.println("Saving " + fileH);
		ImageManagement.Loader.saveCorners(fileH, sa.cs);
	}

	public static float[] calcAvgHues(String[] files) throws Exception {
		// Load in the tile from all files
		WritableRaster[] wrs = loadTileSet(files, new Button());
		char[] ls = loadLetterSet(files);
		// Spaces for the colour number (0 to 6) and then the avg RGB for each
		// tile
		int[] corCols = new int[files.length * 117];
		int[][] rgbCols = new int[files.length * 117][3];
		// Populate then
		for (int i = 0; i < files.length * 117; i++) {
			corCols[i] = WordBuilder.charColours[ls[i] - 'a'];
			rgbCols[i] = avgTileColour(wrs[i]);
		}
		// Calculate statistics
		int[][] sumRGB = new int[7][3];
		int[] n = new int[7];
		for (int i = 0; i < 7; i++) {
			sumRGB[i] = new int[] { 0, 0, 0 };
			n[i] = 0;
		}
		for (int i = 0; i < corCols.length; i++) {
			n[corCols[i]]++;
			sumRGB[corCols[i]][0] += rgbCols[i][0];
			sumRGB[corCols[i]][1] += rgbCols[i][1];
			sumRGB[corCols[i]][2] += rgbCols[i][2];
		}
		// Average hue for each of the 7 colours
		float[] avgHue = new float[7];
		for (int i = 0; i < 7; i++) {
			int avgR = Math.round(sumRGB[i][0] / n[i]);
			int avgG = Math.round(sumRGB[i][1] / n[i]);
			int avgB = Math.round(sumRGB[i][2] / n[i]);
			avgHue[i] = rgbToHue(new int[] { avgR, avgG, avgB });
		}
		// Start of std dev.
		float[] stdHue = new float[7];
		for (int i = 0; i < 7; i++) {
			stdHue[i] = 0;
		}
		n = new int[7];
		for (int i = 0; i < 7; i++) {
			n[i] = 0;
		}
		for (int i = 0; i < rgbCols.length; i++) {
			float ad1 = Math.abs(avgHue[corCols[i]] - rgbToHue(rgbCols[i]));
			float ad2 = 360 - ad1;
			stdHue[corCols[i]] += Math.min(ad1, ad2);
			n[corCols[i]]++;
		}
		for (int i = 0; i < 7; i++) {
			stdHue[i] = stdHue[i] / n[i];
		}
		for (int i = 0; i < 7; i++) {
			System.out.print(i + " ");
			System.out.print(avgHue[i] + "\t");
			System.out.println(stdHue[i]);
		}
		return avgHue;
	}

	private static int[] avgTileColour(Raster ras) {
		int w = ras.getWidth();
		int h = ras.getHeight();
		int n = 0;
		float r, g, b;
		r = g = b = 0;
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				int[] pixel = ras.getPixel(x, y, (int[]) null);
				if (rgbToSat(pixel) > 50) {
					r += pixel[0];
					g += pixel[1];
					b += pixel[2];
					n++;
				}
			}
		}
		int[] out = new int[3];
		out[0] = Math.round(r / n);
		out[1] = Math.round(g / n);
		out[2] = Math.round(b / n);
		return out;
	}

	private static float rgbToHue(int[] rgb) {
		float r = rgb[0] / 255f;
		float g = rgb[1] / 255f;
		float b = rgb[2] / 255f;
		float max = Math.max(Math.max(r, g), b);
		float min = Math.min(Math.min(r, g), b);
		float h = 0;
		if (r == max && g >= b) {
			h = ((g - b) / (max - min)) * 60;
		} else if (r == max && g < b) {
			h = (((g - b) / (max - min)) * 60) + 360;
		} else if (g == max) {
			h = (((b - r) / (max - min)) * 60) + 120;
		} else if (b == max) {
			h = (((r - g) / (max - min)) * 60) + 240;
		}
		return h;
	}

	private static float rgbToSat(int[] rgb) {
		float r = rgb[0] / 255f;
		float g = rgb[1] / 255f;
		float b = rgb[2] / 255f;
		float max = Math.max(Math.max(r, g), b);
		float min = Math.min(Math.min(r, g), b);
		int s = 0;
		if (max != 0) {
			s = Math.round(((max - min) / max) * 100);
		}
		return s;
	}

	public static void bandHueSpace(float[] avg) throws Exception {
		float[] sAvg = new float[7];
		System.arraycopy(avg, 0, sAvg, 0, 7);
		Arrays.sort(sAvg);
		// Find the order the colours are in
		int[] co = new int[7];
		for (int i = 0; i < 7; i++) {
			co[i] = Arrays.binarySearch(sAvg, avg[i]);
		}
		System.out.println("Colour order");
		for (int i = 0; i < 7; i++) {
			System.out.println(co[i]);
		}
		// Identify middle split points
		int[] splits = new int[8];
		for (int i = 1; i < 7; i++) {
			int diff = Math.round(sAvg[i] - sAvg[i - 1]);
			splits[i] = Math.round(sAvg[i - 1]) + (diff / 2);
		}
		// Because range is cyclical first one is different
		int diff = Math.round((360 - sAvg[6]) + sAvg[0]);
		splits[0] = (Math.round(sAvg[6]) + (diff / 2)) % 360;
		// Last bound is always 360
		splits[7] = 360;
		System.out.println("Splits");
		for (int i = 0; i < 8; i++) {
			System.out.println(splits[i]);
		}
		ObjectOutput oo = new ObjectOutputStream(new FileOutputStream("res/bands.ser"));
		oo.writeObject(splits);
		oo.close();
	}

	public void train(String[] files) {
		WritableRaster[] wrs = new WritableRaster[files.length * 117];
		char[] ls = new char[files.length * 117];
		wrs = loadTileSet(files, new Button());
		ls = loadLetterSet(files);
		saveNets(learnTiles(wrs, ls, 81, this));
	}

	@SuppressWarnings("deprecation")
	public static NeuralNet[] learnTiles(WritableRaster[] wrs, char[] ls, int nInps, NeuralNetListener listener) {
		assert (wrs.length == ls.length);
		int[] colCharInd = colCharInd();
		int[][] colIndChar = colIndChar();
		// Find out how many tiles there are of each letter
		int[] nLet = new int[26];
		for (int i = 0; i < ls.length; i++) {
			nLet[ls[i] - 'a']++;
		}
		// Find the minimum number letters for each colour
		int[] nCol = new int[7];
		for (int i = 0; i < 7; i++) {
			int min = Integer.MAX_VALUE;
			for (int j = 0; j < colIndChar[i].length; j++) {
				int n = nLet[colIndChar[i][j]];
				if (n < min) {
					min = n;
				}
			}
			nCol[i] = min;
		}
		// Seven sets, one for each colour, lsOutByCol will hold the letter
		// number per colour. For example 'c' is 1 because it is the 2nd letter
		// of colour 2. This is then stored as [0,1,0,0,0].
		// inpsByCol holds the network input for each tile of each colour.
		// Finally nLetByCol to record a count of each letter processed.
		double[][][] lsOutsByCol = new double[7][][];
		double[][][] inpsByCol = new double[7][][];
		int[][] nLetByCol = new int[7][];
		for (int i = 0; i < 7; i++) {
			lsOutsByCol[i] = new double[nCol[i] * colIndChar[i].length][colIndChar[i].length];
			inpsByCol[i] = new double[nCol[i] * colIndChar[i].length][nInps];
			nLetByCol[i] = new int[colIndChar[i].length];
		}
		int[] done = new int[7];
		for (int i = 0; i < ls.length; i++) {
			int col = WordBuilder.charColours[ls[i] - 'a'];
			// See if we're allowed any more of this letter in the training set
			nLetByCol[col][colCharInd[ls[i] - 'a']]++;
			if (nLetByCol[col][colCharInd[ls[i] - 'a']] <= nCol[col]) {
				// Store in first unused space (given by done
				// count) for the colour. Here we use colCharInd to find
				// the network output number for the letter.
				lsOutsByCol[col][done[col]] = iToOutputArray(colIndChar[col].length, colCharInd[ls[i] - 'a']);
				// Now preprocess and store the network input for this tile.
				ImageManagement.extWhite(wrs[i]);
				ImageManagement.remHighlights(wrs[i]);
				ImageManagement.magicWand(wrs[i]);
				wrs[i] = ImageManagement.autoCrop(wrs[i]);
				inpsByCol[col][done[col]] = ImageManagement.rasterToNetInput(wrs[i], nInps);
				done[col]++;
			}
		}
		// Train a net for each colour
		NeuralNet[] out = new NeuralNet[7];
		for (int i = 0; i < 7; i++) {
			System.out.println("Training colour " + i + "\nData includes:");
			// for (int j = 0; j < inpsByCol[i].length; j++) {
			// System.out.print("Tile " + j + " of " + inpsByCol[i].length);
			// System.out.println(" is letter ");
			// for (int k = 0; k < colIndChar[i].length; k++) {
			// System.out.print(lsOutsByCol[i][j][k] + ", ");
			// }
			// System.out.println(":");
			// ImageManagement.printNetInput(inpsByCol[i][j], nInps);
			//			}
			assert lsOutsByCol[i].length == inpsByCol[i].length;
			NeuralNet nnet = hiddenLNet(nInps, colIndChar[i].length, inpsByCol[i].length, listener);
			nnet.setParam("colour", Integer.toString(i));
			// Now add the training data
			MemoryInputSynapse memInp = new MemoryInputSynapse();
			memInp.setFirstCol(1);
			memInp.setLastCol(nInps);
			nnet.getInputLayer().addInputSynapse(memInp);
			memInp.setInputArray(inpsByCol[i]);
			TeachingSynapse trainer = new TeachingSynapse();
			MemoryInputSynapse samples = new MemoryInputSynapse();
			samples.setFirstCol(1);
			samples.setLastCol(colIndChar[i].length);
			samples.setInputArray(lsOutsByCol[i]);
			trainer.setDesired(samples);
			nnet.getOutputLayer().addOutputSynapse(trainer);
			nnet.setTeacher(trainer);
			nnet.go(true, true);
			out[i] = nnet;
		}
		return out;
	}

	/**
	 * @param nInps
	 *            Number of network input nodes
	 * @param nOutps
	 *            Number of output nodes
	 * @param patterns
	 *            Number of training patterns to be presented to the network
	 * @param listener
	 *            A listener object for reporting
	 * @return A new neural network object with no input or output synapses
	 */
	private static NeuralNet hiddenLNet(int nInps, int nOutps, int patterns, NeuralNetListener listener) {
		// Set up the network
		LinearLayer input = new LinearLayer();
		SigmoidLayer hidden = new SigmoidLayer();
		SoftmaxLayer output = new SoftmaxLayer();
		input.setRows(nInps);
		hidden.setRows((nInps + nOutps) / 2);
		output.setRows(nOutps);
		FullSynapse synapse_IH = new FullSynapse();
		FullSynapse synapse_HO = new FullSynapse();
		input.addOutputSynapse(synapse_IH);
		hidden.addInputSynapse(synapse_IH);
		hidden.addOutputSynapse(synapse_HO);
		output.addInputSynapse(synapse_HO);
		NeuralNet nnet = new NeuralNet();
		nnet.addLayer(input, NeuralNet.INPUT_LAYER);
		nnet.addLayer(hidden, NeuralNet.HIDDEN_LAYER);
		nnet.addLayer(output, NeuralNet.OUTPUT_LAYER);
		Monitor monitor = nnet.getMonitor();
		monitor.setLearningRate(0.8);
		monitor.setMomentum(0.3);
		monitor.addNeuralNetListener(listener);
		monitor.setTrainingPatterns(patterns);
		monitor.setTotCicles(400);
		monitor.setLearning(true);
		return nnet;
	}

	/**
	 * @param n
	 *            The length of the array to return
	 * @param i
	 *            Which index to set to 1, all others are implicitly 0
	 * @return An array such as [0,0,1,0] for n=4 and i=2
	 */
	private static double[] iToOutputArray(int n, int i) {
		double[] out = new double[n];
		out[i] = 1;
		return out;
	}

	/**
	 * @return Returns an array derived from WordBuilder.charColours which gives
	 *         the output number for each letter in a network for its colour.
	 *         For example 'c' (index 2) is 1 because it is the 2nd letter of
	 *         colour 2.
	 */
	public static int[] colCharInd() {
		int[] count = new int[7];
		int[] out = new int[26];
		for (int i = 0; i < 26; i++) {
			out[i] = count[WordBuilder.charColours[i]];
			count[WordBuilder.charColours[i]]++;
		}
		return out;
	}

	/**
	 * @return This is the compliment to colCharInd(). The first index gives the
	 *         colour and the second the output number. The value at this
	 *         location gives the corresponding letter.
	 */
	public static int[][] colIndChar() {
		int[][] out = new int[7][];
		for (int i = 0; i < 7; i++) {
			// Need to know how many outputs (letters) this colour has.
			int n = 0;
			for (int j = 0; j < 26; j++) {
				if (WordBuilder.charColours[j] == i) {
					n++;
				}
			}
			out[i] = new int[n];
			// Now scan WordBuilder.charColours finding the jth output (letter)
			// for this colour. pos is used to track position in charColours.
			int pos = 0;
			for (int j = 0; j < n; j++) {
				while (WordBuilder.charColours[pos] != i) {
					pos++;
				}
				out[i][j] = pos;
				pos++;
			}
		}
		return out;
	}

	public static WritableRaster[] loadTileSet(String[] files, Component comp) {
		WritableRaster[] out = new WritableRaster[files.length * 117];
		for (int f = 0; f < files.length; f++) {
			int[][] c = ImageManagement.Loader.loadCorners(files[f] + ".cor");
			WritableRaster wr = ImageManagement.Loader.loadImgToRaster(files[f] + ".jpg", comp);
			WritableRaster[] rts = ImageManagement.makeRasterTiles(c, wr);
			for (int i = 0; i < 117; i++) {
				out[(f * 117) + i] = rts[i];
			}
		}
		return out;
	}

	public static char[] loadLetterSet(String[] files) {
		char[] out = new char[files.length * 117];
		try {
			for (int f = 0; f < files.length; f++) {
				ArrayList<ArrayList<Character>> ls = Grid.Loader.fileLoadGrid(files[f] + ".txt");
				for (int i = 0; i < 117; i++) {
					out[(f * 117) + i] = ls.get(i / 13).get(i % 13).charValue();
				}
			}
		} catch (InterruptedException e) {
			System.err.println(e);
		}
		return out;
	}

	public static void saveNets(NeuralNet[] nnets) {
		try {
			for (int i = 0; i < nnets.length; i++) {
				FileOutputStream stream = new FileOutputStream("res/net" + i + ".net");
				ObjectOutputStream out = new ObjectOutputStream(stream);
				out.writeObject(nnets[i]);
				out.close();
			}
		} catch (Exception excp) {
			excp.printStackTrace();
		}
	}

	public void netStarted(NeuralNetEvent e) {
		System.out.println("Training began");
	}

	public void netStopped(NeuralNetEvent e) {
		System.out.println("Training finished");
	}

	public void netStoppedError(NeuralNetEvent e, String str) {
		System.out.println("Training error: " + str);
	}

	public void errorChanged(NeuralNetEvent e) {
	}

	public void cicleTerminated(NeuralNetEvent e) {
		Monitor mon = (Monitor) e.getSource();
		long c = mon.getCurrentCicle();
		if (c % 100 == 0) {
			System.out.println( c + " epochs remaining - RMSE = " + mon.getGlobalError());
		}
	}
}
