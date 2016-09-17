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

import tfIdf.Doc;


public class BM25StemmedSystem {
	
	public static String path = "C:\\Users\\Rajkumar\\Documents\\Information Retrieval\\Project\\BM25-Stemmed-System\\";
	public static String outputFileName = path + "BM25-Stemming.txt";

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

	public static void getQueries(){
		File queryFile = new File("cacm_stem_query.txt");
		try {
			Scanner sc = new Scanner(queryFile);
			sc.useDelimiter("\n");
			int count = 1;
			while(sc.hasNext()){
				String str = sc.next();
				String[] queryTerms = str.split(" ");
				String queryID = Integer.toString(count);
				count++;
				queries.put(queryID, queryTerms);
			}
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
	}

	public static void getDocuments(){
		File doc = new File("cacm_stem.txt");
		try {
			Scanner sc = new Scanner(doc);
			sc.useDelimiter("#");
			while(sc.hasNext()){
				String s = sc.next().trim();
				s = s.replaceAll("\n", " ");
				s = s.replaceAll("\\s+", " ");
				long length = s.length();
				String[] tokens = s.split(" ");
				String docName = tokens[0];
				docName = "CACM-" + "0000".substring(0,4-docName.length()) + docName;
				String[] docTerms = new String[tokens.length-1];
				System.arraycopy(tokens, 1, docTerms, 0, docTerms.length);
				Doc d = new Doc(docName,docTerms,length);
				documents.add(d);
				docList.add(docName);
				Set<String> newTokens = d.getTermCount().keySet();
				docTermFreq.put(docName, newTokens);
			}
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
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
		try {
			File file = new File(outputFileName);
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter fileWritter = new FileWriter(outputFileName,true);
			BufferedWriter bufferWriter = new BufferedWriter(fileWritter);
			int count = 1;
			for(int i = temp.size() - 1; i >= temp.size() - 100 ; i--){
				String name = temp.get(i).getKey();
				bufferWriter.write(queryID + "\t" + "Q0 \t" + name + "\t" + count + "\t" + temp.get(i).getValue() + "\tStemmed-BM25-System\n");
				count++;
			}
			bufferWriter.close();
		} catch (IOException e) {
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
			bm25Rank += getBM25ForTerm(ni, N, qf, fi, K, ri, R);
		}		
		return bm25Rank;
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
		getDocuments();
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
	}

}
