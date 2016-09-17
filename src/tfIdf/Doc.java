package tfIdf;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Doc {
	String docName;
	long length;
	LinkedHashMap<String,Integer> termCount = new LinkedHashMap<>();

	public String getDocName() {
		return docName;
	}

	public long getLength() {
		return length;
	}

	public LinkedHashMap<String, Integer> getTermCount() {
		return termCount;
	}


	public Doc(String docName) {
		this.docName = docName;		
		File f = new File("Corpus/" + docName);
		this.length = f.length();
		try {
			Document doc = Jsoup.parse(f, "UTF-8");
			Elements ele = doc.select("pre");
			for(Element e : ele){
				String s = e.ownText().toLowerCase().trim();
				s = s.replaceAll("\\n"," ");
				s = s.replaceAll("(?<![0-9])[\\,\\.]", " ");
				s = s.replaceAll("[\\,\\.](?![0-9])", " ");
				s = s.replaceAll("[-\\/:;!?`\\(\\)\\[\\]\\{\\}\"\'\\|\\’]", " ");
				s = s.replaceAll("\\s+", " ");
				Scanner sc = new Scanner(s);
				sc.useDelimiter(" ");
				while(sc.hasNext()){
					String str = sc.next();
					if(termCount.containsKey(str)){
						int count = termCount.get(str) + 1;
						termCount.put(str,count);
					}
					else {
						termCount.put(str, 1);
					}
				}
				sc.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public Doc(String docName, String[] terms, long length){
		this.docName = docName;
		this.length = length;
		LinkedHashMap<String,Integer> tc = new LinkedHashMap<>();
		for(String t : terms){
			if(tc.containsKey(t)){
				int count = tc.get(t) + 1;
				tc.put(t, count);
			}
			else tc.put(t, 1);
		}
		this.termCount = tc;
	}

}

