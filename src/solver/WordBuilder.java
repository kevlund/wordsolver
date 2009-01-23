package solver;

import java.util.*;
import java.awt.*;
import dictionary.Dictionary;
import dictionary.Letter;

public class WordBuilder {
	Grid grid;
	Dictionary dictionary;
	ArrayList<Point> roots;
	ArrayList<ArrayList<Point>> words;
	public static final int[] colourValues = { 1, 2, 3, 4, 5, 8, 10 };
	// a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z
	// public static final int[] charColours =
	// {0,3,2,1,0,4,2,3,1,5,4,2,3,1,0,2,6,1,0,0,2,4,3,5,3,6}; //Puzzle Word
	public static final int[] charColours = { 0, 2, 2, 1, 0, 3, 1, 3, 2, 5, 4, 4, 2, 1, 0, 2, 6, 0, 0, 0, 1, 3, 3, 5, 3, 6 }; // Words

	// Up
	public WordBuilder(Dictionary dictionary, Grid grid) {
		this.grid = grid;
		this.dictionary = dictionary;
		this.roots = new ArrayList<Point>();
		for (int x = 0; x < grid.getWidth(); x++) {
			for (int y = 0; y < grid.getHeight(); y++) {
				try {
					Point thePoint = new Point(x, y);
					this.grid.getCell(thePoint);
					roots.add(thePoint);
				} catch (NullPointerException e) {
					// The cell doesn't exist on this grid
				}
			}
		}
		this.words = new ArrayList<ArrayList<Point>>();
	}

	public ArrayList<ArrayList<Point>> buildWordList() {
		for (Point r : roots) {
			Stack<Point> visited = new Stack<Point>();
			visited.push(r);
			this.wordRecurse(visited, this.dictionary.getRoot().getChild(this.grid.getCell(r)));
		}
		return this.words;
	}

	private void wordRecurse(Stack<Point> visited, Letter l) {
		// See if we're at a word
		if (l.isWord() && visited.size() > 2) {
			this.processWord(visited);
		}
		// Compute remaining child cells
		ArrayList<Point> children = this.grid.getSurrounding(visited.peek());
		children.removeAll(visited);
		// Process each child node
		for (Point np : children) {
			char nextChar = this.grid.getCell(np);
			if (l.hasChild(nextChar)) {
				visited.push(np);
				this.wordRecurse(visited, l.getChild(nextChar));
				visited.pop();
			}
		}
	}

	private void processWord(Collection<Point> word) {
		ArrayList<Point> wordClone = new ArrayList<Point>();
		wordClone.addAll(word);
		words.add(wordClone);
	}

	public static int scoreWord(String wrd) {
		int score = 0;
		for (char c : wrd.toCharArray()) {
			score += colourValues[charColours[c - 'a']];
		}
		return score * wrd.length();
	}
}