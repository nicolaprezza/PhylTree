package phylTree;

import java.io.*;
import java.util.*;

public class SplitTriples {

	Vector<SplitTriple> triples;
	String species[];//da indice di specie alla specie
	
	int number_of_triples;
	int number_of_species;
	
	SplitTriples(String path){
		
		number_of_triples=0;
		triples = new Vector<SplitTriple>();
		Vector<SplitTriple> strtriples = new Vector<SplitTriple>();

		try{
			
		  FileInputStream fstream = new FileInputStream(path);
		  DataInputStream in = new DataInputStream(fstream);
		  BufferedReader br = new BufferedReader(new InputStreamReader(in));
		  String strLine;
		  String a,b,c;
		  while ((strLine = br.readLine()) != null){
			  StringTokenizer t = new StringTokenizer(strLine);
			  a=t.nextToken();
			  b=t.nextToken();
			  c=t.nextToken();
			  strtriples.add(new SplitTriple(a,b,c));
		  }
		  
		  number_of_triples=strtriples.size();
		  
		  in.close();
		  
		}catch (Exception e){
			  System.err.println("Errore: " + e.getMessage());
		}
	
		//inserisci le specie in un insieme ordinato
		TreeSet<String> speciesSet = new TreeSet<String>();
		for(int i=0;i<number_of_triples;i++){
			speciesSet.add(strtriples.elementAt(i).a);
			speciesSet.add(strtriples.elementAt(i).b);
			speciesSet.add(strtriples.elementAt(i).c);
		}
		
		number_of_species=speciesSet.size();
		species = new String[number_of_species];
		
		Iterator<String> it = speciesSet.iterator();
		
		int idx=0;
		while(it.hasNext())
			species[idx++]=it.next();

		int A,B,C;
		
		//converti nomi specie in numeri e crea triple numeriche
		for(int i=0;i<number_of_triples;i++){
			A = indexInSet(speciesSet,strtriples.elementAt(i).a);
			B = indexInSet(speciesSet,strtriples.elementAt(i).b);
			C = indexInSet(speciesSet,strtriples.elementAt(i).c);

			triples.add(new SplitTriple(A,B,C));
		}

		System.out.print("SPECIE: ");
		for(int i=0;i<number_of_species;i++)
			System.out.print(species[i] + " ");

		System.out.println("\nTRIPLE:");

		for(int i=0;i<number_of_triples;i++)
			System.out.println(strtriples.elementAt(i).a + "," +strtriples.elementAt(i).b + "|" + strtriples.elementAt(i).c );
		
		/*System.out.println("\nTRIPLE NUMERICHE:");

		for(int i=0;i<number_of_triples;i++)
			System.out.println(triples.elementAt(i).A + "," +triples.elementAt(i).B + "|" + triples.elementAt(i).C );
		*/
		
	}//costruttore
	
	//inefficiente, ma poco influente sui tempi totali
	int indexInSet(TreeSet<String> set, String el){
		Iterator<String> it = set.iterator();
		int idx=0;
		while(!it.next().equals(el))//si suppone che el sia presente in set
			idx++;
			
		return idx;
	}
	
	SplitTriple getTriple(int i){
		return triples.get(i);
	}
		
	
}//SplitTriples
