package dictionary;
import java.util.*;
import java.io.*;

public class Letter implements Comparable<Letter>, Serializable {
	static final long serialVersionUID = 1;
	
	private HashMap<Character, Letter> children;
	private Boolean isWord;
	
	public Letter() {
		this.children 	= new HashMap<Character, Letter>(26);
		this.isWord		= false;
	}
	
	public boolean isWord() {
		return this.isWord;
	}
	
	public void getWords() {
		
		if(this.isWord) {
			System.out.println();
		}
		
		for(Character nextLet : this.children.keySet()) {
			System.out.print(nextLet);
			this.children.get(nextLet).getWords();
		}
	}
	
	public void processWord(String word) {
		if(word.length() == 0) {
			this.isWord = true;
		} else {
			this.getChildWithMake(word.charAt(0)).processWord(word.substring(1));
		}
	}
	
	public Integer score() {
		Integer total = 0;
		for(Character nextLet : this.children.keySet()) {
			total += this.children.get(nextLet).score();
		}
		
		if(this.isWord)	{total++;}
		
		return total;
	}
	
	public boolean hasChild(Character c) {
		return this.children.containsKey(c);
	}
	
	public Letter getChild(Character c) {
		return this.children.get(c);
	}
	
	public Collection<Letter> getAllChildren() {
		return this.children.values();
	}
	
	public int compareTo(Letter l) {
		return this.score() - l.score();
	}
	
	private Letter getChildWithMake(Character c) {
		
		if(this.children.containsKey(c)) {
			return this.children.get(c);
		} else {
			Letter myLetter = new Letter();
			this.children.put(c, myLetter);
			return myLetter;
		}
	}
}