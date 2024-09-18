package fuzzylogic;
//
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
/**
* FuzzyFrame is the generic frame of Fuzzy Logic
* @author  Joe T. Schwarz
*/
public abstract class FuzzyFrame {
  /**
  Constructor
  @param obj Object, instantiated Java object class that runs the Fuzzy Logic script
  @param objName String, name of Object class
  */
  protected FuzzyFrame(Object obj, String objName) {
    init();
    THIS = obj;
    this.objName = objName;
    OBJ.put(objName, obj);
  }
  /**
  execute a FuzzyScript
  @param script String, Fuzzy script
  @exception Exception thrown by JAVA  
  */
  public abstract void execute(String script) throws Exception;
  /**
  setPrinter. Default: System.out
  @param printer OutputStream (can be a FileOutputStream)
  */
  public void setPrinter(OutputStream printer) {
    this.out = out;
  }
  /**
  defuzzify a FuzzyVariable
  @param fvName String, FV name
  @return double defuzzied value of FV
  @exception Exception thrown by JAVA
  */
  protected double defuzzify(String fvName) throws Exception {
    FuzzyVariable fv = FVs.get(fvName);
    if (fv == null) throw new Exception("Unknown FuzzyVaribale:"+fvName);
    int max = 0; // assume NO sample is assigned. UNIT: 0.001 by 1000
    double samples[] = new double[1000];
    double unit = (fv.maxValue - fv.minValue)*0.001;
    Iterator<FuzzyData> ite = fv.cache.values().iterator();
    while(ite.hasNext()) {
      FuzzyData fd = ite.next();
      if (fd.hasPoint()) {
        double val = fd.getPoint();
        int min = (int)((fd.left - fv.minValue)/unit);
        int mx = 1 + (int)((fd.right - fv.minValue)/unit);
        if (samples.length < mx) mx = samples.length;
        if (max < mx) max = mx; // this max
        if (min < 0) min = 0;
        for (double d = fv.minValue + min*unit; min < max; ++min) {
          samples[min] += val * fd.fuzzify(d);
          d += unit;
        }
      }
    }
    if (max == 0) throw new Exception("No Sample was assigned to "+fvName);
    double entire = 0, volume = 0;
    if (centroid) {
      double d = fv.minValue;
      for(int i = 0; i < samples.length; ++i) {
        volume += d * samples[i];
        entire += samples[i];
        d += unit;
      }
      if (entire != 0) return volume / entire;
    } else { // area surface
      int left = 0, f = 1;
      double leftArea = 0;
      double rightArea = 0;
      int right = samples.length - 1;
      while (right > left+f) { // calculate the section
        if (leftArea < rightArea) leftArea += samples[left++];
        else rightArea += samples[right--];
      }
      entire = leftArea + rightArea;
      volume = right*rightArea+left*leftArea;
      if (entire != 0) return fv.minValue+(volume/entire)*unit;
    }
    throw new Exception(fvName+"'s MaxValue or MinValue is a ZERO !");
  }
  /**
  isKeyword
  @param word String
  @return boolean true if word is a Fuzzy Keyword
  */
  protected boolean isKeyword(String word) {
    if (word != null) {
      if (word.indexOf(":") > 0 || word.indexOf(".") > 0 ||
          JOs.containsKey(word) || FVs.containsKey(word)) return true;    
      for (String c : commands) if (c.equals(word)) return true;
      for (String o : OPs) if (o.equals(word)) return true;
    }
    return false;
  }
  /**
  isCommand
  @param cmd String
  @return boolean true if word is a Fuzzy Command like if, let, do, while, ...
  */
  protected boolean isCommand(String cmd) {
    if (cmd != null) {
      if (cmd.indexOf(":") > 0 || cmd.indexOf(".") > 0) return true;
      for (String c : commands) if (c.equals(cmd)) return true;
    }
    return false;
  }  
  /**
  prepare Java Native Method string for callJava()
  @param op  string, format: JavaObjectName.JavaMethodName(parm1, parm2, ..)
  @return Object of any type: object, double, int, etc.
  @exception Exception thrown by JAVA
  */
  protected Object prepareJava(String op) throws Exception {
    String java = fExp[idx++];
    StringBuilder sb = new StringBuilder(op+java);
    if (java.charAt(0) != '(') throw new Exception("callJava: missing '(' at line:"+atLine());
    for (; idx < fExp.length; ++idx) {
      java = fExp[idx];
      if (java.charAt(0) == ')') {
        ++idx;
        break;
      }
      sb.append(java);
    }
    if (java.charAt(0) == ')') return callJava(sb.toString());
    throw new Exception("callJava: missing ')' at line:"+atLine());
  }
  /**
  Invoke Java Native Method from script or FuzzyEngine
  @param java  string, format: JavaObjectName.JavaMethodName(parm1, parm2, ..)
  @return Object of any type: object, double, int, etc.
  @exception Exception thrown by JAVA
  */
  protected Object callJava(String java) throws Exception {
    Object[] pObj = null;
    String[] pNames = null;
    int p = java.indexOf(".");
    int a = java.indexOf("(", p);
    if (a < 0) a = java.length( );
    String mName = java.substring(p+1, a++);
    //
    Object obj = javaObject(java.substring(0, p));
    Class<?> cls = obj.getClass(); // registered instance
    if (a < java.length()) pNames = java.substring(a).split(",");
    if (pNames == null) { // Invocation of Method without parm
      Method meth = cls.getDeclaredMethod(mName, new Class[] {});
      meth.setAccessible(true);
      return meth.invoke(obj, pObj);
    }
    p = 0; // all methods
    Method[] meths = cls.getDeclaredMethods();
    OUT: for (Method meth : meths) {
      ++p; // next Index of MethodName
      if (meth.getName().equals(mName)) {
        Parameter[] parms = meth.getParameters();
        if (pNames.length != parms.length) {
          for (a = p; a < meths.length; ++a)
          if (meths[a].getName().equals(mName)) continue OUT;
          throw new Exception("Mismatched number of Parameters:"+java);
        }
        meth.setAccessible(true);
        pObj = new Object[parms.length];
        for (a = 0; a < parms.length; ++a) {
          String pN = pNames[a]; // String Parameter?
          if (pN.charAt(0) == '?')       pObj[a] = JOs.get(pN);
          else try { // Unknown type ...
            Type t = parms[a].getType();
            if      (t.equals(double.class)||
                     t.equals(Double.class))          pObj[a] = getValue(pN);
            else if (t.equals(String.class)) {
              String S = (String) JOs.get(pN);
              pObj[a] = S == null? pN : S;
            } else if (t.equals(FuzzyVariable.class)) pObj[a] = FVs.get(pN);
            else if (t.equals(int.class)||
                     t.equals(Integer.class)) {
              Integer In = (Integer)JOs.get(pN);
              pObj[a] = In == null? Integer.parseInt(pN):In;
            } else if (t.equals(short.class)||
                       t.equals(Short.class))         pObj[a] = Short.parseShort(pN);
            else if (t.equals(long.class)||
                     t.equals(Long.class)) {
              Long L = (Long) JOs.get(pN);
              pObj[a] = L == null? Long.parseLong(pN):L;
            } else                                    pObj[a] = Float.parseFloat(pN);
          } catch (Exception ex) { // unknown Java Object
            pObj[a] = pN;
          }
        }
        try {
          return meth.invoke(obj, pObj);
        } catch (Exception e) { }
      }
    }
    throw new Exception("Unknown JavaMethod or invalid JavaParameters:"+java);
  }
  /**
  getObject
  @param val  string, variable name (FV, double, JavaVariableName)
  @return Object of any type: object, double, int, etc.
  @exception Exception thrown by JAVA
  */
  protected Object getObject(String val) throws Exception {
    // ClassName:JavaVaribaleName or Constant or FV
    if (JOs.containsKey(val)) return JOs.get(val);
    if (!FVs.containsKey(val)) { // not a FV
      if (val.indexOf(":") > 0) return loadJava(val);
      try { // could be a constant
        return Double.parseDouble(val);
      } catch (Exception ex) { }
      throw new Exception("Unknown "+val+" at line:"+atLine());
    }
    try { // fuzzyVariable
      return defuzzify(val);
    } catch (Exception ex) { }
    return FVs.get(val).getSample();
  }
  /**
  getValue from a variable (FV, local or java)
  @param val  string, value name (FV, double, JavaVariableName)
  @return Double
  @exception Exception thrown by JAVA
  */
  protected double getValue(String val) throws Exception {
    // ClassName:JavaVaribaleName or Constant or FV
    if (val.startsWith("this.")) return (double)callJava(getMethod(val));
    if (val.indexOf(":") > 0) try {
      Field f = THIS.getClass().getDeclaredField(val.substring(5));
      f.setAccessible(true);
      return (Double)f.get(THIS);
    } catch (Exception ex) { }
    if (!FVs.containsKey(val)) {
      Object obj = null;
      int p = val.indexOf(":"); // constant?
      if (p < 0) obj = JOs.get(val);
      else obj = loadJava(val);
      if (obj instanceof Integer) return ((Integer) obj).doubleValue();
      else if (obj instanceof Long) return ((Long)obj).doubleValue();
      //
      Double d = (Double)obj;
      if (d != null) return d;
      d = Double.parseDouble(val);
      return d;
    }
    try { // fuzzyVariable
      return defuzzify(val);
    } catch (Exception ex) { }
    return FVs.get(val).getSample();
  }
  /**
  setValue to a Variable (FV, local or java)
  @param vn  string, VariableName
  @param op   char, operator
  @param val  string, value (double, int)
  @exception Exception thrown by JAVA
  */
  protected void setValue(String vn, char op, String val) throws Exception {
    // ClassName:JavaVaribaleName or Constant or FV
    int p = vn.indexOf(":"); // parsing
    String name  = vn.substring(p+1);
    Object obj   =  javaObject(vn.substring(0, p));
    Class<?> cls = obj.getClass();
    Field field = cls.getDeclaredField(name);
    field.setAccessible(true);
    Object p1 = null; // check for String
    if (val.charAt(0) == '?') p1 = JOs.get(val);
    else p1 = getValue(val);
    try {  // don't use getXXX(...)
      if (op == '=') field.set(obj, p1);
      else {
        double d1 = (Double) p1;
        double d0 = (Double) field.get(obj);
        if      (op == '+')  field.set(obj, d0+d1);
        else if (op == '-')  field.set(obj, d0-d1);
        else if (op == '*')  field.set(obj, d0*d1);
        else                 field.set(obj, d0/d1);
      }
    } catch (Exception ex) {
      throw new Exception("Trouble with "+vn+" Reason:"+ex.toString());
    }
  }
  /**
  FuzzyVariable operations (add, sub, mul, div) from script. Example: a = (b * c) / d
  @param op  string, Variable name
  @exception Exception thrown by JAVA
  */
  protected void fvOperation(String op) throws Exception {
    char C = fExp[idx++].charAt(0);
    String val = fExp[idx++];
    boolean neg = (val.charAt(0) == '-');
    if (neg) val = fExp[idx++];
    // exec JavaMethod or JavaVariable or value or FuzzyValue
    double d = 0d;
    if (val.indexOf(".") > 0) { // JavaMethod?
      StringBuilder sb = new StringBuilder(val);
      val = fExp[idx++];
      if (val.charAt(0) != '(') throw new Exception("callJava: missing '(' at line:"+atLine( ));
      for (sb.append(val); idx < fExp.length;) {
        val = fExp[idx++];
        if (val.charAt(0) == ')') break;
        sb.append(val);
      }
      if (idx == fExp.length) throw new Exception("callJava: missing ')' at line:"+atLine( ));
      d = (Double)callJava(sb.toString());
    } else {
      d = getValue(val.charAt(0) != '='? val:fExp[idx++]);
    }
    if (neg) d = -d;
    if (C == '=') {
      FVs.get(op).setSample(d);
    } else {
      double dd = FVs.get(op).getSample();
      if      (C == '+') FVs.get(op).setSample(dd + d);
      else if (C == '-') FVs.get(op).setSample(dd - d);
      else if (C == '*') FVs.get(op).setSample(dd * d);
      else if (C == '/') FVs.get(op).setSample(dd / d);
      else throw new Exception(op+" Unknown operation at line:"+atLine( ));
    }
  }
  /**
  Reset all FuzzyVariables.
  */
  public void resetVariables( ) {
    for (Iterator<FuzzyVariable> fvs = FVs.values().iterator(); fvs.hasNext(); ) fvs.next().reset();
  }
  /**
  get the JavaMethod expression, e.g. print("Hello"), from script
  @param op  string, method name
  @return String contains the name of Java Method and variable
  */
  protected String getMethod(String op) {
    String val = fExp[idx++];
    StringBuilder sb = new StringBuilder(op+val);
    for (; idx < fExp.length;) {
      val = fExp[idx++];
      if (val.charAt(0) == ')') break;
      sb.append(val);
    }
    return sb.toString();
  }
  /**
  JavaPrimitives as Fuzzy primitives handling (recursive)
  @param S String of FuzzyScript
  @exception Exception thrown by JAVA
  */
  protected void javaPrimitive(String S) throws Exception {
    int j = idx++;
    String val = null;
    if (fExp[idx++].equals("=")) val = fExp[idx++];
    else throw new Exception("Missing = at line:"+atLine( ));
    boolean neg = val.charAt(0) == '-';
    if (neg) val = fExp[idx++];
    if (FVs.containsKey(val)) {
      double d = 0;
      try {
        d = defuzzify(val);
      } catch (Exception ex) { }
      JOs.put(fExp[j], d);
      return;
    } else if (S.equals("double")) {
      double D = 0.0d;
      if (val.indexOf(":") > 0) D = (Double)loadJava(val);
      else if (val.indexOf(".") > 0) D = (Double) callJava(getMethod(val));
      else D = Double.parseDouble(val);
      if (neg) D = -D;
      JOs.put(fExp[j], D);
      if (fExp[idx].charAt(0) == ',') {
        fExp[idx] = "double"; // more?
        javaPrimitive(S);
      }
      return;
    } else if (S.equals("int")) {
      int I = 0;
      if (val.indexOf(":") > 0) {
        double d = (Double)loadJava(val);
        I = (int) d;
      } else if (val.indexOf(".") > 0) {
        I = (Integer) callJava(getMethod(val));
      } else I = Integer.parseInt(val);
      if (neg) I = -I;
      JOs.put(fExp[j], I);
      // int a, b = 0, c, ...
      if (fExp[idx].charAt(0) == ',') {
        fExp[idx] = "int"; // more?
        javaPrimitive(S);
      }
      return;
    } else if (S.equals("long")) {
      long L = 0;
      if (val.indexOf(":") > 0) {
        double d = (Double)loadJava(val);
        L = (long) d;
      } else if (val.indexOf(".") > 0) {
        L = (Long) callJava(getMethod(val));
      } else L = Long.parseLong(val);
      if (neg) L = -L;
      JOs.put(fExp[j], L);
      if (fExp[idx].charAt(0) == ',') {
        fExp[idx] = "long"; // more?
        javaPrimitive(S);
      }
      return;
    }
    String s = null;
    if (neg) val = fExp[idx-1]; // back to the String
    if (val.indexOf(":") > 0) s = (String)loadJava(val);
    else s = (String) JOs.get(val);
    JOs.put(fExp[j], s);
    if (fExp[idx].charAt(0) == ',') {
      fExp[idx] = "string"; // more?
      javaPrimitive(S);
    }
  }
  /**
  compute() with JAVA Primitives used as FuzzyPrimitives in FuzzyScript
  <br>FuzzyPrimitives: int, long, double and String (no byte, char, float, short and other Objects)
  @param val  string, operation (while or case)
  @exception Exception thrown by JAVA
  */
  protected void compute(String val) throws Exception {
    char C = fExp[idx++].charAt(0);
    if (C != '=') 
      throw new Exception("Expected = after "+val+" but found:"+C+" at line:"+atLine());
    Object result = doMath( );
    Object type = getObject(val);
    if (type instanceof Integer) {
      if (FVs.containsKey(val)) FVs.get(val).setSample((double)toInt(result));
      else JOs.put(val, toInt(result));
    } else if (type instanceof Long) {
      if (FVs.containsKey(val)) FVs.get(val).setSample((double)toLong(result));
      else JOs.put(val, toLong(result));
    } else if (type instanceof Double) {
      if (FVs.containsKey(val)) FVs.get(val).setSample(toDouble(result));
      else JOs.put(val, toDouble(result));
    } else if (type instanceof String) {
      JOs.put(val, (String)result);
    } else throw new Exception("Invalid Type of "+val+" at line:"+atLine( ));
 }
  // recursive
  private Object doMath( ) throws Exception {
    String val = fExp[idx++];
    char C = val.charAt(0);
    boolean neg = C == '-';
    if (neg) val = fExp[idx++]; // negative?
    Object obj1 = C == '('? doMath( ):getObject(val);
    if (obj1 instanceof Double) obj1 = (neg? -toDouble(obj1):toDouble(obj1)); 
    if (obj1 instanceof Integer) obj1 = (neg? -toInt(obj1):toInt(obj1)); 
    if (obj1 instanceof Long) obj1 = (neg? -toLong(obj1):toLong(obj1)); 
    // is an assignment, not a calculation ?   
    if (isCommand(fExp[idx])) return obj1;
    while (idx < fExp.length) {
      val = fExp[idx++];
      C = val.charAt(0);
      if (C == ')') return obj1;
      //
      int i = 0; // only one assignment?
      for (; i < math.length; ++i) if (math[i].equals(val)) break;
      if (i == math.length) {
        --idx;
        return obj1;
      }
      // Computing
      val = fExp[idx++];
      Object obj2 = val.charAt(0) == '('? doMath( ):getObject(val);
      if (C == '+') { // add
        if (obj1 instanceof Integer) obj1 = toInt(obj1) + toInt(obj2);
        else if (obj1 instanceof Long) obj1 = toLong(obj1) + toLong(obj2);
        else if (obj1 instanceof Double) obj1 = toDouble(obj1) + toDouble(obj2); 
        else if (obj1 instanceof String) obj1 = ""+obj1 + obj2;
        else throw new Exception(val+" is invalid at line:"+atLine( ));
      } else if (C == '-') { // sub
        if (obj1 instanceof Integer) obj1 = toInt(obj1) - toInt(obj2);
        else if (obj1 instanceof Long) obj1 = toLong(obj1) - toLong(obj2);
        else if (obj1 instanceof Double) obj1 = toDouble(obj1) - toDouble(obj2); 
        else throw new Exception(val+" is invalid at line:"+atLine( ));
      } else if (C == '*') { // mul
        if (obj1 instanceof Integer) obj1 = toInt(obj1) * toInt(obj2);
        else if (obj1 instanceof Long) obj1 = toLong(obj1) * toLong(obj2);
        else if (obj1 instanceof Double) obj1 = toDouble(obj1) * toDouble(obj2); 
        else throw new Exception(val+" is invalid at line:"+atLine( ));
      } else { // div
        if (obj1 instanceof Integer) obj1 = toInt(obj1) / toInt(obj2);
        else if (obj1 instanceof Long) obj1 = toLong(obj1) / toLong(obj2);
        else if (obj1 instanceof Double) obj1 = toDouble(obj1) / toDouble(obj2); 
        else throw new Exception(val+" is invalid at line:"+atLine( ));
      }
    }
    throw new Exception("Math Operation is invalid at line:"+atLine( ));
  }
  /**
  aLine points to the syntax-errornous line
  @return String CodeLine where syntax error is found
  */
  protected String atLine( ) {
    StringBuilder sb = new StringBuilder();
    for (int l = lNo, i = 0; i < 5 && l < fExp.length; ++l, ++i) sb.append(fExp[l]+" ");
    return sb.append("...").toString();
  }
  /**
  is FuzzCondition?
  @return boolean true if the expression is fuzzy
  */
  protected boolean isFuzzy() {
    int ix = idx;
    String k = fExp[ix++];
    while (k.charAt(0) == '(') k = fExp[ix++];
    return (FVs.containsKey(k) && ("some".equals(fExp[ix]) ||
            "is".equals(fExp[ix]) || "not".equals(fExp[ix]) ||
            "very".equals(fExp[ix])));
  }
  /**
  Fuzzy print, built-in print
  @exception Exception thrown by JAVA
  */
  protected void print() throws Exception {
    StringBuilder sb = new StringBuilder();
    // US decimal point for double
    Locale.setDefault(Locale.US);
    while (idx < fExp.length) {
      String op = fExp[idx++];
      if (op.indexOf(".") > 0) {
        Object o = prepareJava(op);
        if (o instanceof Double) sb.append(String.format("%.2f", o));
        else sb.append(o);
        if (idx == fExp.length || !"+".equals(fExp[idx])) break;
      } else if (JOs.containsKey(op) || FVs.containsKey(op) || op.indexOf(":") > 0) {
        Object o = getObject(op);
        if (o instanceof Double) sb.append(String.format("%.2f", o));
        else sb.append(o);
        if (idx == fExp.length || !"+".equals(fExp[idx])) break;
      } else for (String c : commands) if (c.equals(op)) {
        --idx;
        break;
      }
    }
    sb.append(System.lineSeparator());
    out.write(sb.toString().getBytes());
  }
  /**
  get end of a block
  @param a String, keyword do, while, or if
  @param e String, keyword with, endwhile, else, endif
  @return int token index to next token
  @exception Exception thrown by JAVA
  */
  protected int atEnd(String a, String e) throws Exception {
    lNo = idx;
    for (int ix = idx, n = 1; ix < fExp.length;) {
      String op = fExp[ix++];
      if (op.equals(a)) ++n;
      else if (e.equals(op)) {
        if (--n == 0) {
          if (!"with".equals(e)) return ix;
          return skipConditions(ix);
        } else if (n < 0) break;
      }
    }
    if ("if".equals(a)) return -1; 
    throw new Exception("Can't find end of loop for "+fExp[idx-1]+" at line"+atLine());
  }
  /**
  skipConditions and move to the next statement
  @param ix  int, index of starting conditional token 
  @return int token index to next statement token
  */
  protected int skipConditions(int ix) {
    while (ix < fExp.length) {
      if (fExp[ix].charAt(0) == '(') ix = skipConditions(ix+1);
      else if (FVs.containsKey(fExp[ix]) || JOs.containsKey(fExp[ix])) ix += 3; 
      else { // index to next conditional term. Continue by && and ||
        if (ix >= fExp.length) return ix; // return @end of script...
        if (fExp[ix].charAt(0) == '&' || fExp[ix].charAt(0) == '|') ++ix;
        else if (fExp[ix].charAt(0) == ')') return ix+1; // brackets  
        else break;
      }
    }
    return ix;
  }
  /**
  onCondition
  @return boolean for Conditions like a &gt; b &amp;&amp; c == d
  @exception Exception thrown by JAVA
  */
  protected boolean onCondition( ) throws Exception {
    if (fExp[idx].charAt(0) == '*') {
      ++idx; // true forever
      return true;
    }
    cnt = 0;
    boolean boo = query( );
    if (cnt != 0) throw new Exception("Unbalanced brackets at line:"+atLine( ));
    return boo;
  }
  //
  private boolean query( ) throws Exception {
    boolean boo = conditioning( );
    String op = fExp[idx]; // return if not && or ||
    while ("&&".equals(op) || "||".equals(op)) {
      ++idx; // index for next token
      boolean b = conditioning( );
      if (op.charAt(0) == '&') 
           boo = boo && b;
      else boo = boo || b;
      op = fExp[idx];
    }
    return boo;
  }
  //
  private boolean conditioning( ) throws Exception {
    String val = fExp[idx++];
    if (val.charAt(0) == '(') {
      ++cnt; // count bracket
      boolean bool = query( );
      if (fExp[idx].charAt(0) == ')') {
        --cnt; // remove bracket pair
        ++idx; // next Token
      }
      return bool;
    }
    boolean neg = val.charAt(0) == '-';
    if (neg) val = fExp[idx++];
    double d0 = getValue(val);    // 1st value
    if (neg) d0 = -d0;             // negative value
    String con = fExp[idx++];     // conditional operator
    val = fExp[idx++];             // get varriable
    neg = val.charAt(0) == '-';   // check sign
    if (neg) val = fExp[idx++];
    double d1 = getValue(val);    // 2nd value
    if (neg) d1 = -d1;             // negative value
    //
    if (con.equals(">"))  return d0 >  d1;
    if (con.equals("<"))  return d0 <  d1;
    if (con.equals("!=")) return d0 != d1;
    if (con.equals("==")) return d0 == d1;
    if (con.equals("<=")) return d0 <= d1;
    if (con.equals(">=")) return d0 >= d1;
    throw new Exception("Unknown Conditional Operator at line:"+atLine( ));
  }
  //
  private Object loadJava(String val) throws Exception {
    // ClassName:JavaVaribaleName
    int p = val.indexOf(":"); // separate the String val
    Object obj   = javaObject(val.substring(0, p)); // Object with className
    Class<?> cls = obj.getClass(); // Class of this Object
    Field field  = cls.getDeclaredField(val.substring(p+1)); // JavaVariableName
    field.setAccessible(true);
    try { // return Object
      return field.get(obj);
    } catch (Exception ex) {
      throw new Exception("Trouble with "+val+" Reason:"+ex.toString());
    }
  }
  //
  private Object javaObject(String clz) throws Exception {
    Object obj = "this".equals(clz)? THIS:OBJ.get(clz);
    if (obj == null) { // unregistered JavaObject
      Class<?> cls = Class.forName(clz); // new class
      obj = cls.getDeclaredConstructor().newInstance();
      OBJ.put(clz, obj); // register this instance
    }
    return obj;
  }
  //
  private int toInt(Object obj) throws Exception {
    if (obj instanceof Double) {
      double x = (Double)obj;
      return (int) x;
    } else if (obj instanceof Long) {
      long x = (Long)obj;
      return (int)x;
    } else if (obj instanceof Short) {
      return (Short)obj;
    } else if (obj instanceof Integer) {
      return (Integer) obj;
    } else if (obj instanceof Float) {
      float x = (Float) obj;
      return (int) x;
    }
    throw new Exception("Invalid/unsupported object:"+obj);
  }
  private long toLong(Object obj) throws Exception {
    if (obj instanceof Double) {
      double x = (Double)obj;
      return (long) x;
    } else if (obj instanceof Long) {
      return (Long)obj;
    } else if (obj instanceof Short) {
      return (long)(Short)obj;
    } else if (obj instanceof Integer) {
      return (long)(Integer) obj;
    } else if (obj instanceof Float) {
      float x = (Float) obj;
      return (long)x;
    }
    throw new Exception("Invalid object:"+obj);
  }
  private double toDouble(Object obj) throws Exception {
    if (obj instanceof Double) {
      return (Double)obj;
    } else if (obj instanceof Long) {
      return (Long)obj;
    } else if (obj instanceof Short) {
      return (Short)obj;
    } else if (obj instanceof Integer) {
      return (Integer) obj;
    } else if (obj instanceof Float) {
      return (Float) obj;
    }
    throw new Exception("Invalid object:"+obj);
  }
  //
  private void init() {
    centroid = false;
    FDs   = new ArrayList<>( );
    OBJ   = new HashMap<>( );
    LABEL = new HashMap<>( );
    FVs   = new HashMap<>(10);
    JOs   = new HashMap<>(10);
    //
    LOOP  = new Stack<>( );
    BREAK = new Stack<>( );
    ELSE = new Stack<>( );
  }
  // ---------------------------------------------------------
  protected Object THIS;
  protected boolean centroid;
  protected int lNo, idx, cnt;
  protected ArrayList<String> FDs;
  protected OutputStream out = System.out;
  protected Stack<Integer> LOOP, BREAK, ELSE;
  protected String fExp[], old, fuzzy, objName;
  //
  protected HashMap<String, Integer> LABEL;
  protected HashMap<String, Object> OBJ, JOs;
  protected HashMap<String, FuzzyVariable> FVs;
  // Implemented Fuzzy Statements
  protected String commands[] = { "if", "else", "endif", "do", "with", "while", "endwhile",
                                  "break", "set", "double", "long", "int", "string", "then",
                                  "declare", "clear", "reset", "pause", "print", "remove",
                                  "continue", "exit"
                                };
  // Operators
  protected String[] OPs = { "is", "+", "-", "*", "/", "=", "||", "==", ">=", "<=", "!=",
                             "&&", ">", "<", "+", "-", "*", "/", "some", "very", "not",
                             "&", "|"
                          };
  // ---------------------------------------------------------
  // Math operators
  private String[] math = { "+", "-", "*", "/" };
 // ---------------------------------------------------------
}
