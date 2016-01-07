package phylTree;

public class SplitTriple implements Comparable<SplitTriple>{

	public int A,B,C;
	String a,b,c;
	
	public SplitTriple(String a, String b, String c){
		this.a=a;
		this.b=b;
		this.c=c;
	}
	
	public SplitTriple(int A, int B, int C){
		
		this.A=Math.min(A,B);
		this.B=Math.max(A,B);
		this.C=C;

	}

	public int compareTo(SplitTriple s){
		if(this.A > s.A){
			return 1;
		}else if(this.A == s.A){
			if(this.B>s.B){
				return 1;
			}else if(this.B==s.B){
				if(this.C>s.C){
					return 1;
				}else if(this.C==s.C){
					return 0;
				}
			}
		}
		return -1;
	}
		
}