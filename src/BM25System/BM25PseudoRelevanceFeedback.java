package BM25System;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import tfIdf.Doc;

public class BM25PseudoRelevanceFeedback {
	
	public static String path = "C:\\Users\\Rajkumar\\Documents\\Information Retrieval\\Project\\BM25-PseudoRelevanceFeedback-System\\";
	public static String outputFileName = path + "BM25-PseudoRelevanceFeedback.txt";

	public static LinkedHashMap<String, String[]> queries = new LinkedHashMap<>();
	public static LinkedHashSet<String> docList = new LinkedHashSet<>();
	public static LinkedHashSet<Doc> documents = new LinkedHashSet<>();
	public static LinkedHashMap<String, Integer> docFrequency = new LinkedHashMap<>();
	public static double k1 = 1.2;
	public static double b = 0.75;
	public static double k2 = 100;
	public static double avdl ;	
	public static LinkedHashMap<String, ArrayList<String>> Relevance = new LinkedHashMap<>();
	public static LinkedHashMap<String, Set<String>> docTermFreq = new LinkedHashMap<>();
	public static LinkedHashMap<String, LinkedHashMap<String,Integer>> docTermCountMap = new LinkedHashMap<>();
	public static int flag = 0;
	public static ArrayList<String> stopList = new ArrayList<>();

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

	public static void getRelevanceDetails(){
		File file = new File("cacm_rel.txt");
		try {
			Scanner sc = new Scanner(file);
			sc.useDelimiter("\n");
			while(sc.hasNext()){
				String[] str = sc.next().split(" ");
				String queryID = str[0];
				String fileName = str[2];
				fileName = fileName.substring(fileName.lastIndexOf("-") + 1);
				fileName = "CACM-" + "0000".substring(0,4-fileName.length()) + fileName;
				ArrayList<String> lst = new ArrayList<>();
				if(Relevance.containsKey(queryID)){
					lst = Relevance.get(queryID);
					lst.add(fileName);
					Relevance.put(queryID, lst);
				}
				else {
					lst.add(fileName);
					Relevance.put(queryID, lst);
				}
			}			
			sc.close();
		} catch (FileNotFoundException e) {
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

	public static void retrieveDocuments(String queryID, String[] queryTerms){
		LinkedHashMap<String, Double> docs = new LinkedHashMap<>();
		LinkedHashMap<String, Integer> qTerms = new LinkedHashMap<>();
		for(String q : queryTerms){
			if(qTerms.containsKey(q)){
				int count = qTerms.get(q) + 1;
				qTerms.put(q, count);
			}
			else {
				qTerms.put(q, 1);
			}
		}
		double R = 0.00;
		if(Relevance.containsKey(queryID)){
			R = (double) Relevance.get(queryID).size();
		}
		for(Doc d : documents){
			double bm25Score = calculateBM25Rank(queryID,d,qTerms,R);
			docs.put(d.getDocName(), bm25Score);
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
		if(flag == 0){
			LinkedHashSet<String> topDocs = new LinkedHashSet<>();
			for(int i = temp.size() - 1; i >= temp.size() - 5 ; i--){
				topDocs.add(temp.get(i).getKey());
			}
			performPseudoRelevance(queryID,queryTerms,topDocs);
		}
		else {
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
					bufferWriter.write(queryID + "\t" + "Q0 \t" + name + "\t" + count + "\t" + temp.get(i).getValue() + "\tPRF-BM25-System\n");
					count++;
				}
				bufferWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void performPseudoRelevance(String queryID,String[] queryTerms,LinkedHashSet<String> topDocs){
		getStopList();
		ArrayList<String> newQuery = new ArrayList<>();
		for(String tokens : queryTerms){
			newQuery.add(tokens);
		}
		for(String d : topDocs){
			ArrayList<String> newTokens = getTokens(d);
			for(String tokens : newTokens){
				newQuery.add(tokens);
			}
		}
		flag = 1;
		String[] newQueryTerms = new String[newQuery.size()];
		newQueryTerms = newQuery.toArray(newQueryTerms);
		retrieveDocuments(queryID,newQueryTerms);		
	}
	
	public static ArrayList<String> getTokens(String d){
		ArrayList<String> newTokens = new ArrayList<>();
		LinkedHashMap<String,Integer> termCounts = new LinkedHashMap<>();
		termCounts = docTermCountMap.get(d);
		for(String s : stopList){
			termCounts.remove(s);
		}
		Comparator<Map.Entry<String, Integer>> byMapValues = new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Map.Entry<String, Integer> left, Map.Entry<String, Integer> right) {
				return left.getValue().compareTo(right.getValue());
			}
		};
		List<Map.Entry<String, Integer>> temp = new ArrayList<Map.Entry<String, Integer>>();
		temp.addAll(termCounts.entrySet());
		Collections.sort(temp,byMapValues);
		for(int i = temp.size() - 1; i >= temp.size() - 5 ; i--){
			newTokens.add(temp.get(i).getKey());
		}		
		return newTokens;
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

	public static double calculateBM25Rank(String queryID,Doc d, LinkedHashMap<String, Integer> qTerms, double R){
		double K = k1 * ((1-b) + (b * (d.getLength()/avdl)));
		double bm25Rank = 0.00;
		for(String term : qTerms.keySet()){
			double qf = (double)qTerms.get(term);
			double fi = 0.00;
			if(d.getTermCount().containsKey(term)){
				fi = (double)d.getTermCount().get(term);
			}
			double ni = 0.00;
			if(docFrequency.containsKey(term)){
				ni = (double)docFrequency.get(term);
			}
			double N = (double)documents.size();
			double ri = 0.00;
			if(Relevance.containsKey(queryID)){
				ri = calculateRI(queryID, term);
			}
			bm25Rank += getBM25ForTerm(ni, N, qf, fi, K, ri, R);
		}		
		return bm25Rank;
	}

	public static double calculateRI(String queryID, String term){
		int count = 0;
		ArrayList<String> docNames = new ArrayList<>();
		docNames = Relevance.get(queryID);
		for(String docName : docNames){
			String name = docName + ".html";
			if(docTermFreq.containsKey(name)){
				if(docTermFreq.get(name).contains(term))
					count++;
			}			
		}
		double ri = (double) count;
		return ri;
	}

	public static double getBM25ForTerm(double ni,double N,double qf,double fi,double K,double ri,double R){
		double out;
		double first = ((ri + 0.5)/(R - ri + 0.5))/((ni - ri + 0.5)/(N - ni - R + ri +0.5));
		double second = ((k1 + 1) * fi)/(K + fi);
		double third = ((k2+1) * qf)/(k2+qf);
		out = Math.log(first) * second * third;
		return out;
	}


	public static void main(String[] args) {
		getQueries();
		getRelevanceDetails();
		getFileNames();
		for(String docName : docList){
			Doc doc = new Doc(docName);
			documents.add(doc);
			Set<String> tokens = doc.getTermCount().keySet();
			docTermFreq.put(docName, tokens);
			docTermCountMap.put(docName, doc.getTermCount());
		}
		getDocumentFrequency();
		long totalLength = 0;
		for(Doc d : documents){
			totalLength += d.getLength();
		}
		avdl = (double)totalLength/(double)documents.size();
		File file = new File(outputFileName);
		if(file.exists())
			file.delete();
		for(String q : queries.keySet()){
			String[] queryTerms = queries.get(q);
			retrieveDocuments(q,queryTerms);
		}
		
		Evaluation.evaluateSystem(path, outputFileName, "BM25-PseudoRelevanceFeedback");
	}
}
