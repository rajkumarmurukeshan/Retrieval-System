package tfIdf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import BM25System.Evaluation;
import tfIdf.Doc;

public class tfIdfSystem {
	
	public static String path = "C:\\Users\\Rajkumar\\Documents\\Information Retrieval\\Project\\TfIdf-System\\";
	public static String outputFileName = path + "TfIdf-BaseSystem.txt";
	
	public static LinkedHashMap<String, String[]> queries = new LinkedHashMap<>();
	public static LinkedHashSet<String> docList = new LinkedHashSet<>();
	public static LinkedHashSet<Doc> documents = new LinkedHashSet<>();
	public static LinkedHashMap<String, Integer> docFrequency = new LinkedHashMap<>();
	
	
	public static void getQueries(){
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
				queries.put(qID, queryTerms);				
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
				bufferWriter.write(queryID + "\t" + "Q0 \t" + name + "\t" + count + "\t" + temp.get(i).getValue() + "\tTF-IDF-System\n");
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
		for(String q : queries.keySet()){
			String[] queryTerms = queries.get(q);
			retrieveDocuments(q,queryTerms);
		}
		
		Evaluation.evaluateSystem(path, outputFileName, "TfIdf-BaseSystem");
	}	
}
