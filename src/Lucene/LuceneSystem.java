package Lucene;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

public class LuceneSystem {
	private static Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
    private String resultFolder;

    public void indexCorpus(File corpusFolder, File indexFolderPath) {
        if (!indexFolderPath.exists()) {
            indexFolderPath.delete();
        }
        indexFolderPath.mkdir();
        FileReader fileReader = null;
        IndexWriter writer = null;
        try {
            FSDirectory dir = FSDirectory.open(indexFolderPath);
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, analyzer);
            writer = new IndexWriter(dir, config);
            int originalNumDocs = writer.numDocs();
            for (File f : corpusFolder.listFiles()) {
                try {
                    Document doc = new Document();
                    fileReader = new FileReader(f);
                    doc.add(new TextField("contents", fileReader));
                    doc.add(new StringField("path", f.getPath(), Field.Store.YES));
                    doc.add(new StringField("filename", f.getName(),
                            Field.Store.YES));
                    writer.addDocument(doc);
                    System.out.println("Added: " + f);
                } catch (Exception e) {
                    System.out.println("Could not add: " + f);
                } finally {
                    fileReader.close();
                }
            }

            int newNumDocs = writer.numDocs();
            System.out.println("");
            System.out.println("************************");
            System.out
                    .println((newNumDocs - originalNumDocs) + " documents added.");
            System.out.println("************************");

            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void search(File indexFolder, String queryString, int id, Map<String,Map<String,String>> queryToDocumentsMap) {
        FileWriter fileWriter =  null;
        BufferedWriter bufferedWriter = null;
        IndexReader reader = null;
        //Map<String,Score> map = new HashMap<String,Score>();
        //Map<String, String> docIdToDocContentMap = new HashMap();
        //FileOperator fileOperator = new FileOperator();
        try {

            reader = DirectoryReader.open(FSDirectory.open(indexFolder));
            IndexSearcher searcher = new IndexSearcher(reader);
            TopScoreDocCollector collector = TopScoreDocCollector.create(100, true);

            Query q = new QueryParser(Version.LUCENE_47, "contents",
                    analyzer).parse(queryString);
            searcher.search(q, collector);
            TopDocs topDocs = collector.topDocs();
            ScoreDoc[] hits = topDocs.scoreDocs;
            File file = new File(resultFolder + "Lucene-Query-" + id + ".txt");
            fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            System.out.println("Found " + hits.length + " hits.");
            for (int i = 1; i <= hits.length; ++i) {
                int docId = hits[i-1].doc;
                Document d = searcher.doc(docId);
                String documentIdPath = d.get("path");
                int index1 = documentIdPath.lastIndexOf("\\");
                int index2 = documentIdPath.lastIndexOf(".");
                String documentId = documentIdPath.substring(index1+1, index2);
                //String content = fileOperator.parseHtmlContentForOnlyPreTag(fileOperator.getHtmlContentFromFileName(documentIdPath));
                //docIdToDocContentMap.put(documentId,content);
                bufferedWriter.write(id + "\t" + "Q0" + "\t" + documentId + "\t" + i + "\t" + hits[i-1].score + "\t" + "Manikandan_System");
                bufferedWriter.newLine();
            }


            //queryToDocumentsMap.put(queryString, docIdToDocContentMap);

            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
    
    public static void getQueries(){
    	
    }
    
    public static void main(String[] args){
    	String corpusFolder = "Corpus";
    	String indexFolderPath = "index1";
    	getQueries();
    }
    
}
