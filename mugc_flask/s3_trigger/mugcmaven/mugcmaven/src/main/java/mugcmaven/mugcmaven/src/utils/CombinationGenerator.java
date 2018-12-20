package mugcmaven.mugcmaven.src.utils;

public class CombinationGenerator {

	  int n, m;
	  //exhaust m and take n possibility.
	  int[] pre;//previous combination.

	  public CombinationGenerator(int n, int m) {
	   this.n = n;
	   this.m = m;
	  }

	  /**
	   * generate one combination
	   * if return null,all combination are generated.
	   */
	  public int[] next() {
	   if (pre == null) {//taken the first combination.
	    pre = new int[n];
	    for (int i = 0; i < pre.length; i++) {
	     pre[i] = i;
	    }
	    int[] ret = new int[n];
	    System.arraycopy(pre, 0, ret, 0, n);
	    return ret;
	   }
	   int ni = n - 1, maxNi = m - 1;
	   while (pre[ni] + 1 > maxNi) {//from right to left find all incremental space
	    ni--;
	    maxNi--;
	    if (ni < 0)
	     return null;//nothing find means the combination is exhausted.
	   }
	   pre[ni]++;
	   while (++ni < n) {
	    pre[ni] = pre[ni - 1] + 1;
	   }
	   int[] ret = new int[n];
	   System.arraycopy(pre, 0, ret, 0, n);
	   return ret;
	  }
	  
	  public int combinations() {
			
		  //choose k from n.
		  int t= 1; int k=m;
		  for (int i= Math.min(n, k-n), l= 1; i >= 1; i--, k--, l++) {
			  t*= k; t/= l;
		  }
			
		  return t;
	  }

}
