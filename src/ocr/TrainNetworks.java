package ocr;

import solver.Grid;
import solver.WordBuilder;

import java.awt.Button;
import java.awt.Component;
import java.awt.image.WritableRaster;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.joone.engine.FullSynapse;
import org.joone.engine.LinearLayer;
import org.joone.engine.Monitor;
import org.joone.engine.NeuralNetEvent;
import org.joone.engine.NeuralNetListener;
import org.joone.engine.SigmoidLayer;
import org.joone.engine.SoftmaxLayer;
import org.joone.engine.learning.TeachingSynapse;
import org.joone.io.MemoryInputSynapse;
import org.joone.net.*;

public class TrainNetworks implements NeuralNetListener {

	public static void main(String[] args) throws Exception {
		String[] files = { "res/grid1", "res/grid2", "res/grid3", "res/grid4" };

		WritableRaster[] wrs = new WritableRaster[files.length * 117];
		char[] ls = new char[files.length * 117];

		wrs = TrainNetworks.loadTileSet(files, new Button());
		ls = TrainNetworks.loadLetterSet(files);

		TrainNetworks.saveNets(TrainNetworks.learnTiles(wrs, ls, 81, new TrainNetworks()));
	}

	@SuppressWarnings("deprecation")
	public static NeuralNet[] learnTiles(WritableRaster[] wrs, char[] ls, int nInps, NeuralNetListener listener) {
		assert (wrs.length == ls.length);
		int[] colCharInd = colCharInd();
		int[][] colIndChar = colIndChar();

		// Find out how many tiles there are of each colour
		int[] nCol = new int[7];
		for (int i = 0; i < ls.length; i++) {
			nCol[WordBuilder.charColours[ls[i] - 'a']]++;
		}

		// Seven sets, one for each colour, lsOutByCol will hold the letter
		// number per colour. For example 'c' is 1 because it is the 2nd letter
		// of colour 2. This is then stored as [0,1,0,0,0].
		// inpsByCol holds the network input for each tile of each colour.
		double[][][] lsOutsByCol = new double[7][][];
		double[][][] inpsByCol = new double[7][][];

		for (int i = 0; i < 7; i++) {
			lsOutsByCol[i] = new double[nCol[i]][colIndChar[i].length];
			inpsByCol[i] = new double[nCol[i]][nInps];
		}

		int[] done = new int[7];
		for (int i = 0; i < ls.length; i++) {
			// Find the colour, store in first unused space (given by done
			// count) for that colour. Here we use colCharInd to find the
			// network output number for the letter.
			int col = WordBuilder.charColours[ls[i] - 'a'];
			lsOutsByCol[col][done[col]] = iToOutputArray(colIndChar[col].length, colCharInd[ls[i] - 'a']);
			
//			System.out.println(i + " of colour " + col + " output: ");
//			for (int k = 0; k < colIndChar[col].length; k++) {
//				System.out.print(lsOutsByCol[col][done[col]][k] + ", ");
//			}
//			System.out.println("Input:");

			// Now preprocess and store the network input for this tile.
			ImageManagement.extWhite(wrs[i]);
			ImageManagement.remHighlights(wrs[i]);
			ImageManagement.magicWand(wrs[i]);
			wrs[i] = ImageManagement.autoCrop(wrs[i]);
			
//			ImageManagement.printNetInput(ImageManagement.rasterToNetInput(wrs[i], nInps), nInps);
			inpsByCol[col][done[col]] = ImageManagement.rasterToNetInput(wrs[i], nInps);
			done[col]++;
		}

		// Train a net for each colour
		NeuralNet[] out = new NeuralNet[7];

		for (int i = 0; i < 7; i++) {
//			System.out.println("Training colour " + i + "\nData includes:");
			for (int j = 0; j < inpsByCol[i].length; j++) {
//				System.out.print("Tile " + j + " of " + inpsByCol[i].length);
//				System.out.println(" is letter ");
//				for (int k = 0; k < colIndChar[i].length; k++) {
//					System.out.print(lsOutsByCol[i][j][k] + ", ");
//				}
//				System.out.println(":");
//				ImageManagement.printNetInput(inpsByCol[i][j], nInps);
			}

			assert lsOutsByCol[i].length == inpsByCol[i].length;

			NeuralNet nnet = hiddenLNet(nInps, colIndChar[i].length, inpsByCol[i].length, listener);

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

			nnet.go();

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
		monitor.setTotCicles(1000);
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
		if (c % 100 == 0)
			System.out.println(c + " epochs remaining - RMSE = " + mon.getGlobalError());
	}
}
