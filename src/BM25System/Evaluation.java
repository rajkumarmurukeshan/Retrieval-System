package BM25System;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;


public class Evaluation {

	public static LinkedHashMap<String, ArrayList<String>> Relevance = new LinkedHashMap<>();
	public static String path;
	public static String sysName;
	public static String inputFilename;
	public static LinkedHashMap<String, LinkedHashMap<String,Integer>> input = new LinkedHashMap<>();
	public static LinkedHashMap<String, LinkedHashMap<String,Double>> precisionMap = new LinkedHashMap<>();
	public static LinkedHashMap<String, LinkedHashMap<String,Double>> recallMap = new LinkedHashMap<>();

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

	public static void calculatePrecisionRecall(){
		for(String str : Relevance.keySet()){
			double noOfRelevantDocuments = 0;
			LinkedHashMap<String, Integer> docRank = new LinkedHashMap<>();
			docRank = input.get(str);
			LinkedHashMap<String, Double> docPrecision = new LinkedHashMap<>();
			LinkedHashMap<String, Double> docRecall = new LinkedHashMap<>();
			ArrayList<String> rel = Relevance.get(str);
			int count = 0;
			for(String doc : docRank.keySet()){
				if(rel.contains(doc))
					count++;
			}
			for(String doc : docRank.keySet()){
				if(rel.contains(doc))
					noOfRelevantDocuments++;
				double precision = (double)noOfRelevantDocuments/(double)docRank.get(doc);
				double recall = 0.00;
				if(count != 0){
					recall = (double) noOfRelevantDocuments/(double) count;
				}
				docPrecision.put(doc, precision);
				docRecall.put(doc, recall);				
			}
			precisionMap.put(str,docPrecision);
			recallMap.put(str,docRecall);			
		}
	}

	public static void writePrecisionRecall(){
		File file = new File(path+"Precision&Recall-"+sysName +".txt");
		try {
			FileWriter fw = new FileWriter(file);
			fw.write("QueryID DocumentName Precision Recall \n");
			for(String queryID : precisionMap.keySet()){
				LinkedHashMap<String, Double> docPrecision = precisionMap.get(queryID);
				LinkedHashMap<String, Double> docRecall = recallMap.get(queryID);				
				for(String doc : docPrecision.keySet()){
					fw.write(queryID + "\t" + doc + "\t" + docPrecision.get(doc) + "\t" + docRecall.get(doc) + "\n");
				}
			}			
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writePrecisionAtRankP(){
		File file = new File(path+"Precision@RankP-"+sysName +".txt");
		try {
			FileWriter fw = new FileWriter(file);
			fw.write("QueryID DocumentName Rank Precision\n");
			for(String queryID : precisionMap.keySet()){
				LinkedHashMap<String, Double> docPrecision = precisionMap.get(queryID);
				int count = 1;
				for(String doc : docPrecision.keySet()){
					if(count == 5 || count == 20){
						fw.write(queryID + "\t" + doc + "\t" + count + "\t" + docPrecision.get(doc) + "\n");
					}
					if (count == 20)
						break;
					count++;
				}
			}			
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static double calculateMRR(){
		double RR = 0.00;
		for(String queryID : Relevance.keySet()){
			ArrayList<String> rel = Relevance.get(queryID);
			LinkedHashMap<String, Integer> docRank = input.get(queryID);
			for(String doc : docRank.keySet()){
				if(rel.contains(doc)){
					RR += (double) 1/(double) docRank.get(doc);
					break;
				}
			}
		}
		double MRR = RR/(double) Relevance.size();
		return MRR;
	}

	public static double calculateMAP(){
		double avgprec = 0.00;
		for(String queryID : Relevance.keySet()){
			ArrayList<String> rel = Relevance.get(queryID);
			double prec = 0.00;
			int count = 0;
			for(String doc : precisionMap.get(queryID).keySet()){
				if(rel.contains(doc)){
					prec += precisionMap.get(queryID).get(doc);
					count++;
				}
			}
			double ap = 0.00;
			if(count !=0){
				ap = prec/(double) count;
			} 
			avgprec += ap;
		}
		double MAP = avgprec/(double)Relevance.size();
		return MAP;
	}

	public static void parseInputFile(){
		File file = new File(inputFilename);
		try {
			Scanner sc = new Scanner(file);
			sc.useDelimiter("\n");
			while(sc.hasNext()){
				String[] str = sc.next().split("\t");
				String queryId = str[0];
				String docName = str[2];
				int rank = Integer.parseInt(str[3]);
				LinkedHashMap<String,Integer> docRank = new LinkedHashMap<>();
				if(input.containsKey(queryId)){					
					docRank = input.get(queryId);
					docRank.put(docName, rank);
				} else {
					docRank.put(docName, rank);
				}
				input.put(queryId, docRank);
			}
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void evaluateSystem(String p, String i, String sys){
		getRelevanceDetails();
		path = p;
		sysName = sys;
		inputFilename = i;
		parseInputFile();
		calculatePrecisionRecall();
		writePrecisionRecall();
		double MAP = calculateMAP();
		double MRR = calculateMRR();
		writePrecisionAtRankP();
		File file = new File(path+"MAP&MRR-"+sysName +".txt");
		try {
			FileWriter fw = new FileWriter(file);
			fw.write("Mean Average Precision -> " + MAP + "\n");
			fw.write("Mean Reciprocal Rank -> " + MRR);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
