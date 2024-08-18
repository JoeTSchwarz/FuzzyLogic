package fuzzylogic;
//
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.util.concurrent.*;
// Joe T. Schwarz
/**
FuzzyCluser is purposed for a multithreading environnment.
* @author  Joe T. Schwarz
*/
public class FuzzyCluster {
  /**
    Constructor of FuzzyCluster
    @param cName String, Cluster Name
  */
  public FuzzyCluster(String cName) {
    this.cName = cName;
    old        = new HashMap<String, String>( );
    lock       = new HashMap<String, Integer>( );
    fCluster   = new HashMap<String, FuzzyEngine>( );
    future     = new HashMap<String, Future<Integer>>( );
  }
  /**
  execute FuzzyExpressions or a Fuzzy Script
  @param feID FuzzyEngine name in FuzzyCluster
  @param script FuzzyExpression script or file name
  @exception Exception thrown if feID is locked
  */
  public void execute(String feID, String script) throws Exception {
    String id = cName+"_"+feID;
    if (lock.containsKey(id))
      throw new Exception(feID+" is locked");
    FuzzyEngine fe = lockEngine(id);
    future.put(id, ForkJoinPool.commonPool().submit(() -> {
      try {
        fe.execute(script);
        return 0; // successful
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      return -1; // erroneous
    }));
    // wait for completion
    Future<Integer> f = future.remove(id);
    if (f != null) f.get();
    lock.remove(id);
  }
  /**
  defuzzify a FuzzyVariable
  @param feID FuzzyEngine name in FuzzyCluster
  @param fvName String, FuzzyVariable name
  @return double the defuztied value of FV
  @exception Exception thrown by JAVA
  */
  public double defuzzify(String feID, String fvName) throws Exception {
    String id = cName+"_"+feID;
    if (lock.containsKey(id))
      throw new Exception(feID+" is locked");
    FuzzyEngine fe = lockEngine(id);
    double d = fe.defuzzify(fvName);
    lock.remove(id);
    return d;
  }
  /**
  Execute a Java Method with a specified FuzzyEngine
  @param feID FuzzyEngine name
  @param java String, format: MyApp.JMethod(parm1, parm2, ...)
  @return Object Java object (returned value)
  @exception Exception thrown if feID is locked or feID is unknown
  */
  public Object execJava(String feID, String java) throws Exception {
    String id = cName+"_"+feID;
    if (lock.containsKey(id))
      throw new Exception(feID+" is locked");
    FuzzyEngine fe = lockEngine(id);
    if (fe == null) throw new Exception("Unknown "+feID);
    Object o = fe.execJava(java);
    lock.remove(id);
    return o;
  }
  /**
  get defuzziedValue from FV with fvName of FuzzyEngine feID
  @param feID FuzzyEngine name in FuzzyCluster
  @param fvName String, FuzzyVariable
  @return double
  @exception Exception thrown if feID is locked or feID is unknown
  */
  public double fuzzyValue(String feID, String fvName) throws Exception {
    String id = cName+"_"+feID;
    if (lock.containsKey(id))
      throw new Exception(feID+" is locked");
    FuzzyEngine fe = lockEngine(id);
    if (fe == null) throw new Exception("Unknown "+feID);
    double d = fe.getValue(fvName);
    lock.remove(id);
    return d;
  }
  /**
  register an instantiated Java Class Object.
  @param feID String, FuzzyEngine ID
  @param obj Object, instantiated object class
  @param oName String, object Name (e.g. MyApp)
  @exception Exception thrown if feID is locked or any parameter is unknown
  */
  public void regObject(String feID, Object obj, String oName) throws Exception {
    String id = cName+"_"+feID;
    if (lock.containsKey(id))
      throw new Exception(feID+" is locked");
    FuzzyEngine fe = lockEngine(id);
    if (fe == null || obj == null || oName == null)
      throw new Exception("Unknown "+feID+" or oName or obj is null.");
    fe.THIS = obj;
    fe.OBJ.put("this", obj);
    fe.OBJ.put(cName+"_"+oName, obj);
    lock.remove(id);
  }
  /**
  remove instantiated Object class.
  @param feID String, FuzzyEngine ID
  @param oName String, object Name
  @return Object or null
  @exception Exception thrown if feID is locked or feID is unknown
  */
  public Object removeObject(String feID, String oName) throws Exception {
    String id = cName+"_"+feID;
    if (lock.containsKey(id))
      throw new Exception(feID+" is locked");
    FuzzyEngine fe = lockEngine(id);
    if (fe == null) throw new Exception("Unknown "+feID);
    Object obj = fe.OBJ.remove(cName+"_"+oName);
    if (obj != null) {
      fe.OBJ.remove("this");
      fe.THIS = null;
    }
    lock.remove(id);
    return obj;
  }
  /**
  create a FuzzyEngine, override if feID already exists
  @param obj  instantiated java object class for THIS FuzzyEngine
  @param feID String, name of FE to be created (used for reference)
  */
  public void createFuzzyEngine(Object obj, String feID) {
    String id = cName+"_"+feID;
    FuzzyEngine fe = new FuzzyEngine(obj, id);
    fCluster.put(id, fe);
  }
  /**
  remove FuzzyEngine
  @param feID String, the FuzzyEngine name.
  @return FuzzyEngine with the referenced name 'feID', null if no FE was found
  @exception Exception feID is locked
  */
  public FuzzyEngine removeFuzzyEngine(String feID) throws Exception {
    String id = cName+"_"+feID;
    if (lock.containsKey(id))
      throw new Exception(feID+" is locked");
    lockEngine(id);
    old.remove(id);
    lock.remove(id);
    return fCluster.remove(id);
  }
  /**
  get FuzzyEngine
  @param feID String, the FuzzyEngine name.
  @return FuzzyEngine with the referenced name 'feID', null if no FE was found
  */
  public FuzzyEngine getFuzzyEngine(String feID) {
    return fCluster.get(cName+"_"+feID);
  }
  /**
  reset FuzzyCluster
  */
  public void reset( ) {
    fCluster.clear( );
    future.clear( );
    lock.clear( );
    old.clear( );
  }
  /**
  size of Cluster (number of FuzzyEngines)
  @return int the number of FEs
  */
  public int size( ) {
    return fCluster.size();
  }
  //
  private FuzzyEngine lockEngine(String feID) throws Exception {
    FuzzyEngine fe = fCluster.get(feID);
    if (fe != null) {
      while (lock.containsKey(feID)) TimeUnit.MICROSECONDS.sleep(1);
      lock.put(feID, 0); // lock this FuzzyEngine
      Future<Integer> f = future.remove(feID);
      if (f != null) f.get();
      return fe;
    }
    throw new Exception("Unknown "+feID);
  }
  //
  private String cName;
  private HashMap<String, String> old;
  private HashMap<String, FuzzyEngine> fCluster;
  private volatile HashMap<String, Integer> lock;
  private HashMap<String, Future<Integer>> future;
}
