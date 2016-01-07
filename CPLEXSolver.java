package phylTree;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;

import java.io.File;
import java.lang.reflect.Field;


public class CPLEXSolver {
	
	static {
		try {
			String workdir = new File(".").getCanonicalPath();
			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				// try to load Windows library
				if (System.getProperty("os.arch").toLowerCase().contains("64")) {
					System.setProperty("java.library.path", System.getProperty("java.library.path")+":"+workdir+"/os/windows/x86_64");
				}
				else if (System.getProperty("os.arch").toLowerCase().contains("86")) {
					System.setProperty("java.library.path", System.getProperty("java.library.path")+":"+workdir+"/os/windows/x86");
				}
				else {
					System.out.println("The os and/or arch is not supported.");
				}
				
			}
			else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
				// try to load Linux library
				if (System.getProperty("os.arch").toLowerCase().contains("64")) {
					System.setProperty("java.library.path", System.getProperty("java.library.path")+":"+workdir+"/os/linux/x86_64");
				}
				else if (System.getProperty("os.arch").toLowerCase().contains("86")) {
					System.setProperty("java.library.path", System.getProperty("java.library.path")+":"+workdir+"/os/linux/x86");
				}
				else {
					System.out.println("The os and/or arch is not supported.");
				}
			}
			else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
				// try to load Mac library
				if (System.getProperty("os.arch").toLowerCase().contains("64")) {
					System.setProperty("java.library.path", System.getProperty("java.library.path")+":"+workdir+"/os/macosx/x86_64");
				}
				else if (System.getProperty("os.arch").toLowerCase().contains("86")) {
					System.setProperty("java.library.path", System.getProperty("java.library.path")+":"+workdir+"/os/macosx/x86");
				}
				else {
					System.out.println("The os and/or arch is not supported.");
				}
			} 
			else {
				System.out.println("The os and/or arch is not supported.");
			}
			Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
			fieldSysPath.setAccessible( true );
			fieldSysPath.set( null, null );
			System.loadLibrary("cplex125");
		} catch (Exception e) {
			System.out.println("The dynamic link library for CPLEX could not be"
									+ "loaded.\nConsider using\njava -Djava.library.path=");
			e.printStackTrace();
		}
	}

	IloCplex solver;

	SplitTriples triples;//insieme triple
	
	IloNumExpr objFn; // funzione obiettivo
	
	int number_of_triples,nr_of_leafs;//n=numero di specie
	
	//indici dei nodi interni (inode), delle foglie e nodi nella numerazione e numero totale di nodi
	int first_node, first_inode, last_inode, first_leaf, last_leaf, last_node, nr_of_nodes, nr_of_inodes;
	
	IloIntVar[][] T;//albero T. vengono inserite solo variabili T_ij tali che i<j
	IloNumVar[][] Tc;//chiusura transitiva dell'albero T trasposto
	IloNumVar[][][] P;//3-cammini P_ikj
	IloNumVar[] u;//u[i] = nodo interno i presente?
	IloNumVar[][][] L;//L_ijk = 1 sse esistono cammini i->k e j->k in T trasposto
	
	public CPLEXSolver(SplitTriples triples) throws IloException{
		
		this.solver = new IloCplex();
		
		this.triples=triples;
		number_of_triples = triples.number_of_triples;
		
		nr_of_leafs=triples.number_of_species;
		
		first_node = 0;//primo nodo
		first_inode = 0;//primo nodo
		last_inode = nr_of_leafs-1;//ultimo nodo interno
		first_leaf = nr_of_leafs-1;//prima foglia
		last_leaf = 2*nr_of_leafs-1;//ultima foglia (nei cicli usare segno <)
		last_node = last_leaf;//ultimo nodo
		nr_of_nodes = 2*nr_of_leafs-1;	
		nr_of_inodes = nr_of_leafs-1;
		
		T = new IloIntVar[nr_of_inodes][nr_of_nodes];
		
		for(int i=first_inode;i<last_inode;i++)//solo nodi interni
			for(int j=i+1;j<last_node;j++)//tutti i nodi
				T[i][j] = solver.boolVar();
		
		u = new IloNumVar[nr_of_inodes];

		for(int i=first_inode;i<last_inode;i++)//solo nodi interni
			u[i] = solver.numVar(0,1);

		Tc = new IloNumVar[nr_of_nodes][nr_of_inodes];
		
		for(int j=first_inode;j<last_inode;j++)//arrivo solo nei nodi interni
			for(int i=j+1;i<last_node;i++)//partenza da tutti i nodi
				Tc[i][j] = solver.numVar(0,1);
		
		P = new IloNumVar[nr_of_nodes][nr_of_inodes][nr_of_inodes];
		
		for(int i=first_node;i<last_node;i++)//partenza da tutti i nodi. i->k->j
			for(int k=first_inode;k<last_inode;k++)//nodo intermedio: solo nodi interni
				for(int j=first_inode;j<last_inode;j++)//arrivo solo nei nodi interni
					if(i>k && k>j)
						P[i][k][j] = solver.numVar(0,1);
			
		L = new IloNumVar[nr_of_leafs][nr_of_leafs][nr_of_inodes];

		for(int i=first_leaf;i<last_leaf;i++)//i->k
			for(int j=i+1;j<last_leaf;j++)//j->k
				for(int k=first_inode;k<last_inode;k++)//nodo di arrivo: solo nodi interni
					//attenzione: dato che leafs iniziano da posizione nr_of_inodes, occorre sottrarre questo offset
					L[i-nr_of_inodes][j-nr_of_inodes][k] = solver.numVar(0,1);
		
	}//costruttore
		
	public String solve(int lb) throws IloException{
		
		//Funzione obiettivo
		IloNumExpr objFun = solver.constant(0);
		for(int i=first_inode;i<last_inode;i++)//solo nodi interni
			objFun = solver.sum(objFun, u[i]);
		
		solver.addMinimize(objFun);
		
		//per limitare i tempi di calcolo imponi un limite inferiore alla funzione obiettivo
		solver.addGe(objFun, lb);
		
		//Vincoli (2)
		
		for(int i=first_inode;i<last_inode;i++)//solo nodi interni
			for(int j=i+1;j<last_node;j++)//tutti i nodi
				solver.addGe(u[i], T[i][j]);
				
		//vincolo (3) : la radice deve essere presente
		
		solver.addEq(u[0], 1);
		
		//vincoli (4) : ogni nodo utilizzato ha uno e un solo padre
		
		//ogni nodo interno utilizzato ha uno e un solo padre
		for(int j=first_inode+1;j<last_inode;j++){//solo nodi interni esclusa la radice
			
			IloNumExpr sum = solver.constant(0);
			
			for(int i=first_inode;i<j;i++)//tutti i nodi
				sum = solver.sum(sum, T[i][j]);
			
			solver.addEq(sum, u[j]);
			
		}
		
		//ogni radice ha uno e un solo padre
		for(int j=first_leaf;j<last_leaf;j++){//tutte le foglie
			
			IloNumExpr sum = solver.constant(0);
			
			for(int i=first_inode;i<last_inode;i++)//tutti i nodi interni
				sum = solver.sum(sum, T[i][j]);
			
			solver.addEq(sum, 1);
			
		}
		
		//vincolo (5) : ogni nodo interno utilizzato ha almeno 2 figli
		
		for(int i=first_inode;i<last_inode;i++){//solo nodi interni
			
			IloNumExpr sum = solver.constant(0);
			
			for(int j=i+1;j<last_node;j++)//tutti i nodi
				sum = solver.sum(sum, T[i][j]);
			
			solver.addGe(sum, solver.prod(u[i],2));
			
		}
		
		//vincolo (8)
		
		for(int j=first_inode;j<last_inode;j++)//arrivo solo nei nodi interni
			for(int i=j+1;i<last_node;i++)//partenza da tutti i nodi
				solver.addGe(Tc[i][j],T[j][i]);
		
		//vincolo (9)
		
		for(int i=first_node;i<last_node;i++)
			for(int j=first_inode;j<last_inode;j++){//i->k->j, dove j,k nei nodi interni
										
				IloNumExpr sum = solver.constant(0);
				
				for(int k=first_inode;k<last_inode;k++)//tutti i nodi interni
					if(j<k && k<i) //deve valere i->k->j, cioè j<k<i
						sum = solver.sum(sum, P[i][k][j]);
						
				if(j<i){
					sum = solver.sum(sum, T[j][i]);
					solver.addLe(Tc[i][j],sum);
				}
				
			}

		//vincolo (10)
		
		for(int i=first_node;i<last_node;i++)//partenza da tutti i nodi. i->k->j
			for(int k=first_inode;k<last_inode;k++)//nodo intermedio: solo nodi interni
				for(int j=first_inode;j<last_inode;j++)//arrivo solo nei nodi interni
					if(i>k && k>j)//deve valere i->k->j, cioè j<k<i
						solver.addGe(Tc[i][j],P[i][k][j]);
		
		//vincoli (11)
		
		for(int i=first_node;i<last_node;i++)//partenza da tutti i nodi. i->k->j
			for(int k=first_inode;k<last_inode;k++)//nodo intermedio: solo nodi interni
				for(int j=first_inode;j<last_inode;j++)//arrivo solo nei nodi interni
					if(i>k && k>j){//deve valere i->k->j, cioè j<k<i
						solver.addGe( solver.sum(1,P[i][k][j]),solver.sum(Tc[i][k],Tc[k][j]) );
						solver.addLe(P[i][k][j],Tc[i][k]);
						solver.addLe(P[i][k][j],Tc[k][j]);
					}
		
		//vincoli (12)
		
		for(int i=first_leaf;i<last_leaf;i++)//i->k
			for(int j=i+1;j<last_leaf;j++)//j->k
				for(int k=first_inode;k<last_inode;k++){//nodo di arrivo: solo nodi interni
					//attenzione: dato che leafs iniziano da posizione nr_of_inodes, occorre sottrarre questo offset
					solver.addGe(solver.sum(1,L[i-nr_of_inodes][j-nr_of_inodes][k]),solver.sum(Tc[i][k],Tc[j][k]));
					solver.addLe(L[i-nr_of_inodes][j-nr_of_inodes][k],Tc[i][k]);
					solver.addLe(L[i-nr_of_inodes][j-nr_of_inodes][k],Tc[j][k]);
				}
		
		//vincolo (13) consistenza con l'insieme di triple
						
		for(int t=0;t<number_of_triples;t++){
			
			int i,j,k;//foglie

			i = triples.getTriple(t).A;
			j = triples.getTriple(t).B;
			k = triples.getTriple(t).C;
				
			IloNumExpr sum1 = solver.constant(0);
			IloNumExpr sum2 = solver.constant(0);
	
			for(int v=first_inode;v<last_inode;v++)//nodo di arrivo: solo nodi interni
				sum1 = solver.sum(L[i][j][v],sum1);//somma su tutti i possibili nodi di arrivo
			
			for(int v=first_inode;v<last_inode;v++)//nodo di arrivo: solo nodi interni
				sum2 = solver.sum(L[Math.min(i,k)][Math.max(i,k)][v],sum2);//somma su tutti i possibili nodi di arrivo
							
			sum2 = solver.sum(sum2,1);
			
			solver.addGe(sum1,sum2);
			
		}
		
		solver.solve();
		
		System.out.println("\nMatrice di adiacenza:\n");

		printT();
		
		System.out.println("\nNumero di nodi interni : "+((int)solver.getObjValue()-1));
		
		return getNewickTree(0) + ";";//restituisci newick tree radicato in 0 (radice)

	}//solve
	
	void printT() throws IloException{
		
		System.out.print("u     ");
		
		for(int j=first_node;j<last_node;j++)
			System.out.print(j+((int)j/10>=1?" ":"  "));
		System.out.println();
		
		for(int i=first_inode;i<last_inode;i++){//solo nodi interni
			System.out.print((int)solver.getValue(u[i])+"  "+i + ((int)i/10>=1?" ":"  "));
			for(int j=first_node;j<last_node;j++)//tutti i nodi
				if(j>i){
					System.out.print((int)solver.getValue(T[i][j])+"  ");
				}else{
					System.out.print("-  ");
				}
			System.out.println();
		}
		
	}
	
	boolean isLeaf(int i){
		return first_leaf <= i && i < last_leaf;
	}
	
	//ottiene ricorsivamente il newickTree del nodo i
	String getNewickTree(int i) throws IloException{
		
		String s = "";
		
		if(isLeaf(i))
			s = triples.species[i-nr_of_inodes];
		else{
			s="(";
			
			int last_children=0;
			
			for(int j=i+1;j<nr_of_nodes;j++)//trova ultimo figlio
				if((int)Math.round(solver.getValue(T[i][j]))==1)
					last_children=j;
					
			for(int j=i+1;j<nr_of_nodes;j++){
				
				if((int)Math.round(solver.getValue(T[i][j]))==1){//esiste l'arco i->j
					s = s + getNewickTree(j) + (j==last_children?"":",");
				}
				
			}
			s+=")";
		}
		
		return s;
		
	}
	
	
}
