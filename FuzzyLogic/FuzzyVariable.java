package fuzzylogic;
import java.util.*;
// Joe T. Schwarz
/**
FuzzyVariable (FV), like all other programming languages, has its own variables.
<br>a FV always has its own set of FuzzyData (FD)
<br>FD can be added, replaced (set) or removed
<br>Sample for FD is set by setSample
<br>satisfied query result is set by addPoint
*/
public class FuzzyVariable {    
    /**
    Constructor
    @param name String, FV name
    */
    public FuzzyVariable(String name) {
        this.name = name;
        FD = new ArrayList<FuzzyData>(10);
        cache = new HashMap<String, FuzzyData>();
    }
    /**
    add a FuzzyData (FD)
    @param fd FuzzyData
    */
    public void add(FuzzyData fd) {
        FD.add(fd);
        cache.put(fd.getName(), fd);
        //Check if there should be a change in min and max support values
        if (minMaxSet) {
            double v = fd.getMin();
            if(v < minValue) minValue = v;
            v = fd.getMax();
            if(v > maxValue) maxValue = v;
        } else {
            minValue = fd.getMin();
            maxValue = fd.getMax();
            minMaxSet = true;
        }
    }
    /**
    set a FuzzyData (FD)
    @param fd FuzzyData (overwritten old FD if it exists). 
    */
    public void set(FuzzyData fd) {
      String fdName = fd.getName();
      FuzzyData old = cache.remove(fdName);       
      if (old != null) {
        minValue = old.getMin();
        maxValue = old.getMax(); 
        Iterator<FuzzyData> it = cache.values().iterator();
        while (it.hasNext()) {
          FuzzyData f = it.next();
          if (minValue > f.getMin()) minValue = f.getMin();
          if (maxValue < f.getMax()) maxValue = f.getMax();                   
        }
        FD.remove(old);
      }
      add(fd);
    }
    /**
    remove a FuzzyData (FD)
    @param fdName FuzzyData name
    @return FuzzyData which is removed, null if FD is unknown
    */
    public FuzzyData remove(String fdName) {
        FuzzyData fd = cache.remove(fdName);       
        if (fd != null) {
          minValue = fd.getMin();
          maxValue = fd.getMax(); 
          Iterator<FuzzyData> it = cache.values().iterator();
          while (it.hasNext()) {
              FuzzyData f = it.next();
              if (minValue > f.getMin()) minValue = f.getMin();
              if (maxValue < f.getMax()) maxValue = f.getMax();                   
          }
        }
        return fd;
    }
    /**
    getFDList - list of FD names
    @return ArrayList of FuzzyData
    */
    public ArrayList<FuzzyData> getFDList() {
        return new ArrayList<>(FD);
    }
    /**
    equals compare with a FV
    @param FV FuzzyVariable
    @return boolean
    */
    public boolean equals(FuzzyVariable FV) {
        if (FV.cache.size() == cache.size() && minMaxSet == FV.minMaxSet && sample == FV.sample &&
            minValue == FV.minValue && maxValue == FV.maxValue && name.equals(FV.name)) {
            Iterator<FuzzyData> e1 = cache.values().iterator();
            Iterator<FuzzyData> e2 = FV.cache.values().iterator();
            while (e1.hasNext()) if (!e1.next().equals(e2.next())) return false;
            return true;
        }
        return false;
    }
    /**
    isFuzzy checks if the specified fdName is fuzzificable
    @param fdName String, the FuzzyData name
    @return double fuzzified setValue
    @exception Exception if fdName is unknown
    */
    public double isFuzzy(String fdName) throws Exception {
        FuzzyData fd = cache.get(fdName);
        if (fd != null) return fd.fuzzify(sample);
        throw new Exception("FD "+fdName+" is unrknown in "+name);
    }
    /**
    reset all FuzzyData
    */
    public void clear( ) {
        for (Iterator<FuzzyData> fds = cache.values().iterator(); fds.hasNext(); ) fds.next().reset();
    }
    /**
    reset FV (FDs are reset, and sample is st to 0)
    */
    public void reset( ) {
        sample = 0.0d;
        clear();
    }
    /**
    add Point to fdName
    @param fdName FuzzyData name
    @param d   double, the adding point
    @exception Exception thrown if fdName is unknown
    */
    public void addPoint(String fdName, double d) throws Exception {
        if (!cache.containsKey(fdName))
          throw new Exception("FD "+fdName+" is unknown in "+name);
        cache.get(fdName).addPoint(d);
    }
    /**
    FV setSample
    @param d double, setting value
    */
    public void setSample(double d) {
        sample = d;
    }
    /**
    get FV sample
    @return double, setting value
    */
    public double getSample() {
        return sample;
    }
    /**
    get FuzzyData
    @param fdName String, the FD name
    @return FuzzyData with the given name or null
    */
    public FuzzyData getFuzzyData(String fdName) {
        return cache.get(fdName);
    }
    /**
    contains a FuzzyData with the given fdName
    @param fdName String, the FD name
    @return boolean true if FV contains FD
    */
    public boolean contains(String fdName) {
        return cache.containsKey(fdName);
    }
    /**
    contains a FuzzyData
    @param fd FuzzyData
    @return boolean true if FV contains FD with this name
    */
    public boolean contains(FuzzyData fd) {
      if (fd == null) return false;
      return cache.keySet().contains(fd.getName());
    }
    /**
    getName
    @return String FV name
    */
    public String getName() {
        return name;
    }
    /**
    toString
    @return String FV Description
    */
    public String toString() {
        StringBuilder S = new StringBuilder(name+"(");
        for (Iterator<FuzzyData> ite = cache.values().iterator(); ite.hasNext();) {
          FuzzyData fd = ite.next();
          S.append(fd.toString()+",");
        }
        return (S.toString()+")");
    }
    protected String name;
    protected ArrayList<FuzzyData> FD;
    protected boolean minMaxSet = false;
    protected Map<String, FuzzyData> cache;
    protected double minValue, maxValue, sample;
}
