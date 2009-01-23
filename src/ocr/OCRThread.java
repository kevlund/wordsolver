package ocr;

import java.awt.Color;
import java.awt.image.WritableRaster;
import java.io.FileInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import javax.swing.border.LineBorder;
import org.joone.engine.DirectSynapse;
import org.joone.engine.Layer;
import org.joone.engine.Pattern;
import org.joone.net.NeuralNet;
import org.joone.net.NeuralNetLoader;
import gui.Main;
import gui.OCRTile;

public class OCRThread implements Runnable {
	private Main main;
	private DirectSynapse[] memInps;
	private DirectSynapse[] memOuts;
	private final int[] bands;

	public OCRThread(Main main) throws Exception {
		this.main = main;
		main.op.blankTiles();
		// Load colour bands
		ObjectInput oi = new ObjectInputStream(new FileInputStream("res/bands.ser"));
		bands = (int[]) oi.readObject();
		oi.close();
		// Set up the neural nets
		NeuralNet[] nnets = loadNets();
		memInps = new DirectSynapse[7];
		memOuts = new DirectSynapse[7];
		for (int i = 0; i < 7; i++) {
			// Replace training connections with live one
			// Hold on to the synapses in memXXXs to use later
			DirectSynapse memInp = new DirectSynapse();
			memInps[i] = memInp;
			Layer input = nnets[i].getInputLayer();
			input.removeAllInputs();
			input.addInputSynapse(memInp);
			DirectSynapse memOut = new DirectSynapse();
			memOuts[i] = memOut;
			Layer output = nnets[i].getOutputLayer();
			output.removeAllOutputs();
			output.addOutputSynapse(memOut);
			nnets[i].getMonitor().setLearning(false);
			nnets[i].go();
		}
	}

	public void run() {
		final WritableRaster gridRaster = ImageManagement.Loader.loadImgToRaster(Main.pp.imgFile.getAbsolutePath(), main);
		final WritableRaster[] tileRasters = ImageManagement.makeRasterTiles(Main.pp.imagePreview.corners, gridRaster);
		final int[][] colIndChar = TrainNetworks.colIndChar();
		int[] count = new int[7];
		for (int i = 0; i < 117; i++) {
			// Get the tiles colour
			int col = ImageManagement.tileColour(bands, tileRasters[i]);
			// Do image processing and set the picture
			ImageManagement.extWhite(tileRasters[i]);
			ImageManagement.remHighlights(tileRasters[i]);
			ImageManagement.magicWand(tileRasters[i]);
			tileRasters[i] = ImageManagement.autoCrop(tileRasters[i]);
			// Display preview tile
			((OCRTile) main.op.ocrGrid.getComponent(i)).setWR(tileRasters[i]);
			// Run the network
			Pattern inp = new Pattern(ImageManagement.rasterToNetInput(tileRasters[i], 81));
			inp.setCount(count[col] + 1);
			count[col]++;
			memInps[col].fwdPut(inp);
			// Get the winner and resolve to a letter
			Pattern outp = memOuts[col].fwdGet();
			int w = getWinner(outp.getArray());
			char l = Character.toChars(colIndChar[col][w] + 'a')[0];
			((OCRTile) main.op.ocrGrid.getComponent(i)).setText("" + Character.toUpperCase(l));
			((OCRTile) main.op.ocrGrid.getComponent(i)).setBorder(borderFromS(outp.getArray()[w]));
		}
		main.op.startButton.setEnabled(true);
	}

	private static NeuralNet[] loadNets() {
		NeuralNet[] out = new NeuralNet[7];
		try {
			for (int i = 0; i < 7; i++) {
				NeuralNetLoader loader = new NeuralNetLoader("res/net" + i + ".net");
				out[i] = loader.getNeuralNet();
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		return out;
	}

	private static int getWinner(double[] outps) {
		double max = 0;
		int maxi = 0;
		for (int i = 0; i < outps.length; i++) {
			double s = outps[i];
			if (s > max) {
				max = s;
				maxi = i;
			}
		}
		return maxi;
	}

	private static LineBorder borderFromS(double s) {
		float sf = (float) s;
		Color col = Color.getHSBColor(Math.max(0, sf - 0.7f), 1, 1);
		int w = 1;
		if (sf - 0.7f < 0.1) {
			w = 3;
		} else if ((sf - 0.7f < 0.2)) {
			w = 2;
		}
		return new LineBorder(col, w, false);
	}
}
