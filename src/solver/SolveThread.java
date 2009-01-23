package solver;

import gui.Main;
import dictionary.Dictionary;
import solver.ChainBuilder;
import solver.Grid;

public class SolveThread implements Runnable {
	private Main main;
	private Dictionary dict;
	private Grid grid;
	private boolean running;

	public SolveThread(Main main) {
		this.main = main;
		this.running = false;
		this.dict = Main.dict;
	}

	public void run() {
		if (!running) {
			grid = new Grid(Grid.Loader.ocrLoadGrid(main));
			ChainBuilder cb = new ChainBuilder(main, dict, grid);
			cb.buildChainList();
		}
	}
}