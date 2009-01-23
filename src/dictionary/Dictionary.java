package dictionary;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.regex.*;

public class Dictionary implements java.io.Serializable {
	static final long serialVersionUID = 1;
	
	public class Stats extends HashMap<Character, Integer> {
		static final long serialVersionUID = 1;
		
		public int totalChars;
		
		public Stats() {
			new HashMap<Character, Integer>(26);
			for(char c = 'a'; c <= 'z'; c++) {
				super.put(c, 0);
			}
			totalChars = 0;
		}
		
		public void processWord(String word) {
			for(int i = 0; i < word.length(); i++) {
				char c = word.charAt(i);
				super.put(c, super.get(c) + 1);
				totalChars++;
			}
		}
		
		public Integer get(Character c) {
			return totalChars - super.get(c);
		}
	}
	
	private final Letter root;
	public Stats stats;
	
	public Dictionary(String dictionaryFile) {
		this.stats = new Stats();
		this.root = this.buildTree(dictionaryFile);
	}
	
	public void addWord(String word) {
		this.root.processWord(word);
	}
	
	public Letter getRoot() {
		return this.root;
	}
	
	public Double scoreString(String prefix) {
		Letter thisLetter = root;
		
		while(prefix.length() > 0) {
			Character thisChar = prefix.charAt(0);
			prefix = prefix.substring(1);
			
			if(!thisLetter.hasChild(thisChar)) {
				return new Double(0);
			} else {
				thisLetter = thisLetter.getChild(thisChar);
			}
		}
		return thisLetter.score().doubleValue();
	}
	
	public boolean isWord(String word) {
		Letter thisLetter = root;
		
		while(word.length() > 0) {
			Character thisChar = word.charAt(0);
			word = word.substring(1);
			
			if(!thisLetter.hasChild(thisChar)) {
				return false;
			} else {
				thisLetter = thisLetter.getChild(thisChar);
			}
		}
		return thisLetter.isWord();
	}
	
	public void saveDictionary(String targetFile) {
		System.out.print("Serializing & Saving Tree");
		try {
			ObjectOutputStream objOut = new ObjectOutputStream (new FileOutputStream(targetFile));
			objOut.writeObject(this);
			} catch (IOException e) {
				System.err.println("Died saving dict. " + e);
			}
			System.out.println("\t\t[OK]");
	}
	
	public static Dictionary loadDictionary(String sourceFile) {
		System.out.print("Loading Dictionary Tree");
		Dictionary dict;
		try {
		ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(sourceFile));
		dict = (Dictionary) objIn.readObject();
		
		} catch (FileNotFoundException e) {
			System.err.println("dictionary not found! " + e);
			dict = null;
		} catch (IOException e) {
			System.err.println("dictionary died in transit! " + e);
			dict = null;
		} catch (ClassNotFoundException e) {
			System.err.println("there is no dictionary! " + e);
			dict = null;
		}
		System.out.println("\t\t[OK]");
		return dict;
	}
	
	private Letter buildTree(String dictionaryFile) {
		System.out.print("Building Dictionary Tree");
		Letter root = new Letter();
		
		try {
			BufferedReader dictionaryIn = new BufferedReader(new FileReader(dictionaryFile));
			
			while(dictionaryIn.ready()) {
				String s = dictionaryIn.readLine();
				if(Dictionary.validWord(s)) {
					root.processWord(s);
					stats.processWord(s);
				}
			}
		} catch (IOException e) {
			System.err.println("borked :D  " + e);
		}
		
		System.out.println("\t\t[OK]");
		return root;
	}
	
	private static final boolean validWord(String word) {
		Pattern test = Pattern.compile("[^A-z]");
		Matcher match = test.matcher(word);
		if(match.find()) {
			return false;
		} else {
			return true;
		}
	}
}