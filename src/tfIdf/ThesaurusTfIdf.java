package tfIdf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import BM25System.Evaluation;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;
import tfIdf.Doc;

public class ThesaurusTfIdf {
	
	public static String path = "C:\\Users\\Rajkumar\\Documents\\Information Retrieval\\Project\\Thesauras-TfIDf-System\\";
	public static String outputFileName = path + "Thesaurus-TfIDf.txt";

	public static LinkedHashMap<String, String[]> queries = new LinkedHashMap<>();
	public static LinkedHashSet<String> docList = new LinkedHashSet<>();
	public static LinkedHashSet<Doc> documents = new LinkedHashSet<>();
	public static LinkedHashMap<String, Integer> docFrequency = new LinkedHashMap<>();
	public static ArrayList<String> stopList = new ArrayList<>();


	public static void getQueries(){
		getStopList();
		File f = new File("cacm.html");
		try {
			Document doc = Jsoup.parse(f, "UTF-8");
			Elements ele = doc.getElementsByTag("DOC");
			for(Element e : ele){
				String qID = e.getElementsByTag("DOCNO").first().ownText().trim();
				//Integer queryID = Integer.parseInt(qID);
				String text = e.ownText().toLowerCase();
				text = text.replaceAll("(?<![0-9])[\\,\\.]", " ");
				text = text.replaceAll("[\\,\\.](?![0-9])", " ");
				text = text.replaceAll("[\\/:;!?`\\(\\)\\[\\]\\{\\}\"\'\\|\\’]", " ");
				text = text.replaceAll("\\s+", " ");
				String[] queryTerms = text.split(" ");
				ArrayList<String> qT = new ArrayList<>();
				for(String term: queryTerms){
					if(!stopList.contains(term))
						qT.add(term);
				}
				String[] newQueryTerms = new String[qT.size()];
				newQueryTerms = qT.toArray(newQueryTerms);
				queries.put(qID, newQueryTerms);				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void getFileNames(){
		File folder = new File("Corpus/");
		File[] files = folder.listFiles();
		for(File f : files){
			docList.add(f.getName());
		}
	}

	public static void getDocumentFrequency(){
		for(Doc d : documents){
			LinkedHashMap<String,Integer> terms = new LinkedHashMap<>();
			terms = d.getTermCount();
			for(String term : terms.keySet()){
				if(docFrequency.containsKey(term)){
					int count = docFrequency.get(term) + 1;
					docFrequency.put(term, count);
				}
				else docFrequency.put(term, 1);
			}
		}
	}

	public static void retrieveDocuments(String queryID,String[] queryTerms){
		LinkedHashMap<String, Double> docs = new LinkedHashMap<>();
		for (Doc d : documents){
			double score = getTfIdfScore(d,queryTerms);
			docs.put(d.getDocName(), score);
		}

		Comparator<Map.Entry<String, Double>> byMapValues = new Comparator<Map.Entry<String, Double>>() {
			@Override
			public int compare(Map.Entry<String, Double> left, Map.Entry<String, Double> right) {
				return left.getValue().compareTo(right.getValue());
			}
		};
		List<Map.Entry<String, Double>> temp = new ArrayList<Map.Entry<String, Double>>();
		temp.addAll(docs.entrySet());
		Collections.sort(temp,byMapValues);
		try {
			File file = new File(outputFileName);
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter fileWritter = new FileWriter(outputFileName,true);
			BufferedWriter bufferWriter = new BufferedWriter(fileWritter);
			int count = 1;
			for(int i = temp.size() - 1; i >= temp.size() - 100 ; i--){
				String dName = temp.get(i).getKey();
				String name = dName.substring(0,dName.lastIndexOf("."));
				bufferWriter.write(queryID + "\t" + "Q0 \t" + name + "\t" + count + "\t" + temp.get(i).getValue() + "\tTF-IDF-Thesaurus\n");
				count++;
			}
			bufferWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public static double getTfIdfScore(Doc d, String[] queryTerms){
		double val = 0;
		for(String term : queryTerms){
			int termCount = 0;
			if(d.termCount.containsKey(term))
				termCount = d.getTermCount().get(term);
			double tf = Math.log(termCount + 1);
			int N = docList.size();
			int nk = N ;
			if(docFrequency.containsKey(term))
				nk = docFrequency.get(term);
			double idf = Math.log(N/nk);
			double product = tf*idf;
			val += product;			
		}	
		return val;		
	}

	public static void performThesaurus(String queryID,String[] queryTerms){
		ArrayList<String> newQueryTerms = new ArrayList<>();
		for(String q : queryTerms){
			newQueryTerms.add(q);
		}
		WordNetDatabase database = WordNetDatabase.getFileInstance();
		for(String s : queryTerms){
			Synset[] synsets = database.getSynsets(s);
			for(Synset syn: synsets){
				String[] tokens = syn.getWordForms();
				for(String t : tokens){
					newQueryTerms.add(t);
				}
			}
		}
		String[] newQueryTokens = new String[newQueryTerms.size()];
		newQueryTokens = newQueryTerms.toArray(newQueryTokens);
		retrieveDocuments(queryID, newQueryTokens);		
	}	

	public static void getStopList(){
		File stpList = new File("common_words.txt");
		try {
			Scanner sc = new Scanner(stpList);
			sc.useDelimiter("\n");
			while(sc.hasNext()){
				String str = sc.next().trim();
				if(str != "")
					stopList.add(str);
			}
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args){
		getQueries();
		getFileNames();
		for(String docName : docList){
			Doc doc = new Doc(docName);
			documents.add(doc);
		}
		getDocumentFrequency();
		File file = new File(outputFileName);
		if(file.exists())
			file.delete();
		for(String q : queries.keySet()){;
		performThesaurus(q,queries.get(q));
		}
		
		Evaluation.evaluateSystem(path, outputFileName, "Thesaurus-TfIdf");
		
	}
}
