package solver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.util.*;
import java.awt.*;
import gui.Main;
import gui.OCRTile;

public class Grid implements Cloneable {

	public static class Loader {
		public static ArrayList<ArrayList<Character>> buildGrid(int height, int width) throws InterruptedException {

			ArrayList<ArrayList<Character>> theGrid = new ArrayList<ArrayList<Character>>(height);

			try {
				Process gridMaker = Runtime.getRuntime().exec("./makeBoard.pl " + height + ' ' + width);
				gridMaker.waitFor();

				BufferedReader gridIn = new BufferedReader(new InputStreamReader(gridMaker.getInputStream()));
				StringTokenizer st;

				while (gridIn.ready()) {
					st = new StringTokenizer(gridIn.readLine());
					ArrayList<Character> thisRow = new ArrayList<Character>(width);
					while (st.hasMoreTokens()) {
						thisRow.add(st.nextToken().charAt(0));
					}
					theGrid.add(thisRow);
				}
			} catch (IOException e) {
				System.err.println("borked on IO, sucker! :D  " + e);
			}

			if (theGrid.size() == 0) {
				System.err.println("erm, no grid :(  ");
			}

			return theGrid;
		}

		public static ArrayList<ArrayList<Character>> fileLoadGrid(String file) throws InterruptedException {

			ArrayList<ArrayList<Character>> theGrid = new ArrayList<ArrayList<Character>>();
			try {
				BufferedReader in = new BufferedReader(new FileReader(file));
				String str;
				while ((str = in.readLine()) != null) {
					ArrayList<Character> line = new ArrayList<Character>();
					for (char c : str.toCharArray()) {
						line.add(c);
					}
					theGrid.add(line);
				}
				in.close();
			} catch (IOException e) {
				System.err.println("borked on IO, sucker! :D  " + e);
			}

			if (theGrid.size() == 0) {
				System.err.println("erm, no grid :(  ");
			}
			return theGrid;
		}

		public static ArrayList<ArrayList<Character>> ocrLoadGrid(Main main) {
			ArrayList<ArrayList<Character>> theGrid = new ArrayList<ArrayList<Character>>();
			for (int r = 0; r < 9; r++) {
				ArrayList<Character> line = new ArrayList<Character>();
				for (int c = 0; c < 13; c++) {
					char ch = ((OCRTile) main.op.ocrGrid.getComponent((r * 13) + c)).getText().charAt(0);
					line.add(Character.toLowerCase(ch));
				}
				theGrid.add(line);
			}
			return theGrid;
		}

	}

	private ArrayList<ArrayList<Character>> grid;

	public Grid() {
		this.grid = new ArrayList<ArrayList<Character>>();
	}

	public Grid(ArrayList<ArrayList<Character>> grid) {
		this.grid = grid;
	}

	public void setGrid(ArrayList<ArrayList<Character>> grid) {
		this.grid = grid;
	}

	public ArrayList<ArrayList<Character>> getGrid() {
		return this.grid;
	}

	public Character getCell(Point c) {
		Character x = this.grid.get(c.x).get(c.y);
		if (x != null) {
			return this.grid.get(c.x).get(c.y);
		} else {
			throw new NullPointerException("Cell " + c.x + 'x' + c.y + " has been deleted");
		}
	}

	public Integer getWidth() {
		return grid.size();
	}

	public Integer getHeight() {
		return grid.get(0).size();
	}

	public ArrayList<Point> getSurrounding(Point p) {
		ArrayList<Point> out = new ArrayList<Point>();
		for (int x = p.x - 1; x <= p.x + 1; x++) {
			for (int y = p.y - 1; y <= p.y + 1; y++) {
				if (!(x == p.x && y == p.y)) {
					Point newCell = new Point(x, y);
					try {
						this.getCell(newCell);
						out.add(newCell);
					} catch (IndexOutOfBoundsException e) {
						// Just means we're off the grid...
						// ... well, either that or the letter has been
						// removed..
						// ... hopefully :)
					} catch (NullPointerException e) {
						// This def means the letters gone!
					}
				}
			}
		}
		return out;
	}

	public void removeLetters(Collection<Point> cells) {
		for (Point c : cells) {
			this.grid.get(c.x).set(c.y, null);
		}

		this.grid = Grid.dropLetters(this.grid);
		this.grid = Grid.slideLetters(this.grid);
	}

	public String toString() {
		String out = new String();
		for (ArrayList<Character> x : this.grid) {
			for (Character y : x) {
				if (y == null) {
					out += ' ';
				} else {
					out += y;
				}
				out += ' ';
			}
			out += "\n";
		}
		return out;
	}
	
	public String toTileLetters() {
		String out = "";
		int i = 0;
		for (ArrayList<Character> x : this.grid) {
			for (Character y : x) {
				if (y == null) {
					out += ' '; 
				} else {
					out += y; 
				}
				i++;
			}
		}
		return out;
	}

	public String pointsToString(Collection<Point> word) {
		String thisWord = new String();
		for (Point q : word) {
			thisWord += this.getCell(q);
		}
		return thisWord;
	}

	public Grid clone() {
		ArrayList<ArrayList<Character>> clone = new ArrayList<ArrayList<Character>>();

		for (ArrayList<Character> r : this.grid) {
			ArrayList<Character> cloneRow = new ArrayList<Character>();
			for (Character c : r) {
				try {
					cloneRow.add(new Character(c));
				} catch (NullPointerException e) {
					// Cloning of a deleted cell..
					cloneRow.add(null);
				}
			}
			clone.add(cloneRow);
		}

		return new Grid(clone);
	}

	public int cellsRemaining() {
		int out = 0;
		for (ArrayList<Character> x : this.grid) {
			for (Character y : x) {
				if (y != null) {
					out++;
				}
			}
		}
		return out;
	}

	private static ArrayList<ArrayList<Character>> dropLetters(ArrayList<ArrayList<Character>> grid) {
		Collections.reverse(grid);
		grid = Grid.floatLetters(grid);
		Collections.reverse(grid);
		return grid;
	}

	private static ArrayList<ArrayList<Character>> slideLetters(ArrayList<ArrayList<Character>> grid) {

		grid = Grid.transposeACW(grid);
		grid = Grid.floatLetters(grid);
		Collections.reverse(grid);
		grid = Grid.transposeACW(grid);
		Collections.reverse(grid);
		return grid;
	}

	private static ArrayList<ArrayList<Character>> transposeACW(ArrayList<ArrayList<Character>> in) {
		ArrayList<ArrayList<Character>> transpose = new ArrayList<ArrayList<Character>>();

		for (int i = in.get(0).size() - 1; i >= 0; i--) {
			ArrayList<Character> row = new ArrayList<Character>();
			for (int j = 0; j < in.size(); j++) {
				row.add(in.get(j).get(i));
			}
			transpose.add(row);
		}
		return transpose;
	}

	private static ArrayList<ArrayList<Character>> floatLetters(ArrayList<ArrayList<Character>> grid) {

		Iterator<ArrayList<Character>> rowIter;
		ArrayList<Character> lower;
		ArrayList<Character> upper;
		ListIterator<Character> lowerIter;
		ListIterator<Character> upperIter;
		Character upperChar;
		Character lowerChar;
		boolean modified = true;

		while (modified) {
			modified = false;

			rowIter = grid.iterator();
			upper = rowIter.next();

			while (rowIter.hasNext()) {
				lower = upper;
				upper = rowIter.next();
				upperIter = upper.listIterator();
				lowerIter = lower.listIterator();

				while (upperIter.hasNext()) {

					upperChar = upperIter.next();
					lowerChar = lowerIter.next();

					if (lowerChar == null && upperChar != null) {
						lowerIter.set(upperChar);
						upperIter.set(null);
						modified = true;
					}
				}
			}
		}

		return grid;
	}
}