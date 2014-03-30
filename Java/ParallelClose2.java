
import java.util.concurrent.*;
import java.util.*;

// compile (use java 7) with 
//
// % javac ParallelClose2.java
//
// and run with
//
// % java ParallelClose2 k
//
// for 2^k processes. k is optional, defaulting to 1.

// Some good ideas at
// http://homes.cs.washington.edu/~djg/teachingMaterials/spac/grossmanSPAC_forkJoinFramework.html

// timing with a = b = 1: 2 threads was the fastest, a liitle over 10 secs
//                          and 1 thread was over 19. 4 threads was slightly
//                          slower than 1
//
// with a = 43, b = 571: with 1  thread  64.8 seconds,
//                       with 2  threads 32.5 seconds,
//                       with 4  threads 20.6 seconds,
//                       with 8  threads 19.1 seconds,
//                       with 16 threads 18.3 seconds,
//                       with 32 threads 23.5 seconds,
//                       with 64 threads 34.6 seconds,

class Globals {
  static ForkJoinPool fjPool = new ForkJoinPool();
}

class ParallelClose2 extends RecursiveTask<Map<Integer,Integer>> {

  final int maxDepth;
  final int inc;
  final static int a = 43;
  final static int b = 571;
  final static int c = 100001;
  final static int itCount = 20001;
  final Map<Integer,Integer> map;
  final int depth;
  final int id;
  
  private int g(int x, int y) {
    return (a*x + b*y) % c;
  } 

  ParallelClose2(Map<Integer,Integer> map, 
                int depth, int maxDepth, int id) {
    this.map = map;
    this.depth = depth;
    this.maxDepth = maxDepth;
    int foo = 1;
    for (int i = 0; i < maxDepth; i++) {
      foo = 2 * foo;
    }
    inc = foo;
    this.id = id;
  }

  protected Map<Integer,Integer> compute() {
    try {
      if (depth >= maxDepth) return computeSerial();
      ParallelClose2 evens = new ParallelClose2(map, depth + 1, 
                                                maxDepth, 2 * id);
      ParallelClose2 odds = new ParallelClose2(map, depth + 1, 
                                               maxDepth, 2 * id + 1);
      evens.fork();
      odds.compute();  // don't really need the answer
      evens.join();
    }
    catch (InterruptedException ex) {System.out.println("Interrupted!"); }

    return map;
  }

  protected Map<Integer,Integer> computeSerial() throws InterruptedException {
    System.out.println("id = " + id);
    for (int i = 0; i < itCount; i++) {
      for (int j = id; j < itCount; j += inc) {
        int v = g(i,j);
        if (!map.containsKey(v)) map.put(v, id);
      }
    }
    return map;
  }

  static Map<Integer,Integer> fixMap(Map<Integer,Integer> map, int maxDep) {
    return Globals.fjPool.invoke(new ParallelClose2(map, 0, maxDep, 0));
  }

  static Map<Integer,Integer> fixMap(Map<Integer,Integer> map) {
    return Globals.fjPool.invoke(new ParallelClose2(map, 0, 1, 0));
  }


  public static void main(String[] args) {
    Map<Integer,Integer> map = new ConcurrentHashMap<Integer,Integer>();
    int max = -1;
    try {
      max = Integer.parseInt(args[0]);
    } catch (Exception ex) {}
    if (max == -1) max = 1;
    int pow = 1;
    for (int i = 0; i < max; i++) {
      pow = 2 * pow;
    }
    long t0 = System.currentTimeMillis();
    fixMap(map, max);
    long time = System.currentTimeMillis() - t0;

    int[] counts = new int[pow];
    for (Integer key : map.keySet()) {
      int v = map.get(key);
      counts[v]++;
    }
    System.out.println("counts: " + Arrays.toString(counts));
    System.out.println("total count: " + map.size());
    System.out.println("total execs: " + (itCount * itCount));
    System.out.println("time: " + time);

    boolean doSerial = false;
    if (doSerial) {
      map = new HashMap<Integer,Integer>();
      ParallelClose2 pclose = new ParallelClose2(map, 0, 0, 0);
      t0 = System.currentTimeMillis();
      try {
        pclose.computeSerial();
      } catch (Exception ex) {}
      time = System.currentTimeMillis() - t0;
      System.out.println("pure serial time: " + time);
    }
  }

}





