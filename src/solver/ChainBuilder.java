package solver;

import java.awt.Point;
import java.util.*;
import gui.Main;
import dictionary.Dictionary;

public class ChainBuilder {
	private class WordComparator implements Comparator<ArrayList<Point>> {
		public Dictionary dictionary;
		public Grid grid;
		// 1 is score, 2 is length, 3 is stats based
		public int mode = 1;

		public int compare(ArrayList<Point> a, ArrayList<Point> b) {
			if (mode == 1) {
				return WordBuilder.scoreWord(this.grid.pointsToString(b)) - WordBuilder.scoreWord(this.grid.pointsToString(a));
			} else {
				if (mode == 2 && a.size() > b.size()) {
					return -1;
				} else if (mode == 2 && b.size() > a.size()) {
					return +1;
				} else {
					return scoreWord(grid.pointsToString(b)) - scoreWord(grid.pointsToString(a));
				}
			}
		}

		private int scoreWord(String word) {
			int out = 0;
			for (int i = 0; i < word.length(); i++) {
				out += dictionary.stats.get(word.charAt(i));
			}
			return (out / word.length());
		}
	}
	private Main main;
	private WordBuilder root;
	private final WordComparator wc;
	private int bestScore;
	private long chainsProcessed;

	public ChainBuilder(Main main, Dictionary dictionary, Grid grid) {
		this.main = main;
		root = new WordBuilder(dictionary, grid);
		wc = new WordComparator();
		wc.mode = 1;
		wc.grid = grid;
		wc.dictionary = dictionary;
	}

	public void buildChainList() {
		this.bestScore = 0;
		this.chainsProcessed = 0;
		chainRecurse(new Stack<ArrayList<Point>>(), 0, root);
		System.out.println("Traversal complete");
	}

	private void chainRecurse(Stack<ArrayList<Point>> visited, int score, WordBuilder wb) {
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
		if (wb.grid.cellsRemaining() == 0) {
			score += 500;
		}
		wc.grid = wb.grid;
		Collections.sort(wb.buildWordList(), wc);
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
		System.out.println(wb.words.size() + " : " + score);
		if (wb.words.size() == 0 && score > this.bestScore) {
			this.bestScore = score;
			this.processChain(visited, score);
		}
		if (wb.words.size() > 0) {
			int n = Math.min(wb.words.size(), 2);
			for (ArrayList<Point> word : wb.words.subList(0, n)) {
				visited.push(word);
				int wordScore = WordBuilder.scoreWord(wb.grid.pointsToString(word));
				Grid newGrid = wb.grid.clone();
				newGrid.removeLetters(word);
				chainRecurse(visited, score + wordScore, new WordBuilder(wb.dictionary, newGrid));
				if (Thread.currentThread().isInterrupted()) {
					return;
				}
				visited.pop();
			}
		}
		if (Thread.currentThread().isInterrupted()) {
			return;
		}
		// All words processed so return
		this.chainsProcessed++;
	}

	private void processChain(Stack<ArrayList<Point>> visited, int score) {
		main.sp.processChain(visited, root.grid, score);
	}
}
