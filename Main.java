package phylTree;
import phylTree.CPLEXSolver;
import ilog.concert.IloException;

public class Main {

	public static void main(String[] args) {

		System.out.println("Lettura triple dal file " + args[0] + " ... ");
		SplitTriples triples = new SplitTriples(args[0]);
		
		try{
			
			CPLEXSolver solver = new CPLEXSolver(triples);
						
			String tree = solver.solve(Integer.parseInt(args[1]));
			
			System.out.println("\nNewick tree: " + tree);
			
			
		}catch(IloException e){
			e.printStackTrace();
		}
				
	}//main

}
