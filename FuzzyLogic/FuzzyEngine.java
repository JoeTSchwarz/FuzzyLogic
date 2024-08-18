package fuzzylogic;
//
import java.util.concurrent.TimeUnit;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.io.*;
/**
* <br>-----------------------------------------------------------------------------------
* <ul>
* <li>FV/FD for FuzzyVariable/FuzzyData and fvN/fdN for FV Name/FD Name
* <li>a value can be a double, along or an int constant (e.g. 12.24, -123), or
* <li>a value of a defuzzied FV, accessed by fvN
* <li>JavaMethod Invocation format: ClassName.MethodName(parm1, parm2, ... ) or this.MethodName( ... )
* <li>JavaVariable access format: ClassName:VariableName or this:VariableName
* <li>Comment must start with double slashes (//...) and always ends at the end of the line.
* <li>Keywords must be in lower case and names are case senstive
* </ul>
* <br>-----------------------------------------------------------------------------------
* Note: FuzzyEngine is NOT thread-safe. For a multithreading environment: FuzzyCluster
* @author  Joe T. Schwarz
*/
public class FuzzyEngine extends FuzzyFrame {
  /**
  Constructor.
  @param obj Object, JAVA app (class) that runs THIS FuzzyEngine (usually as 'this')
  @param objName String, name of the object class. Example new FuzzyEngine(this, "MyApp");
  */
  public FuzzyEngine(Object obj, String objName) {
    super(obj, objName);
  }
  /**
  execute a script or FuzzyExpressions
  @param script FuzzyExpression script or script file name
  @exception Exception thrown by JAVA
  */
  public void execute(String script) throws Exception {
    cnt = 0;
    idx = 0;
    lNo = 0;
    //
    ELSE.clear();
    LOOP.clear();
    LABEL.clear();
    BREAK.clear();
    // already sanitized?
    if (script.equals(old)) syntax( );
    else { // new script. Sanitize, syntax check & execution
      InputStream inp = THIS.getClass().getResourceAsStream("/"+script);
      if (inp != null) { // read from JAR file
        byte[] rec = new byte[inp.available()];
        int n = inp.read(rec);
        inp.close();
        fuzzy = (new String(rec, 0, n)).replace("\r", "");
      } else // read from computer
        fuzzy = (new String(new FileInputStream(script).readAllBytes())).replace("\r", "");
      while (true) { // remove all Comments
        int a = fuzzy.indexOf("//");
        if (a < 0) break;
        int e = fuzzy.indexOf("\n", a);
        fuzzy = fuzzy.replace(fuzzy.substring(a, e < 0? fuzzy.length(): e+1), "");
      }
      String tmp = registerFVs()
             .replace("(", " ( ").replace(")", " ) ").replace("+", " + ")
             .replace("-", " - ").replace("*", " * ").replace("/", " / ")
             .replace("=", " = ").replace(",", " , ").replace("\t", " ")
             .replace("||", " || ").replace("&&", " && ").replace("\n", " ")
             .replace(" and ", " && ").replace(" or ", " || ")
             .replaceAll("  *", " ").trim()
             .replaceAll(" ", "\n");
      fExp = tmp.split("\n");
      old = script;
      syntax( );
    }
  }
  /**
  clearEngine. clear all FD points of all FVs
  */
  public void clearEngine() {
    List<String> lst = new ArrayList<>(FVs.keySet());
    for (String fvName : lst) FVs.get(fvName).reset( );
  }
  /**
  reset FuzzyEngine. All caches are flushed, incl. script
  */
  public void resetEngine( ) {
    JOs.clear();    // registere Java primitives
    OBJ.clear();    // Registered JavaObjects
    FDs.clear();    // Registered FuzzyData
    FVs.clear();    // Registered FuzzyVariables
    fExp = null;    // FuzzyExpressions or FuzzyScript
    OBJ.put(objName, THIS); // restore java 'bearer'
    //
    centroid = false;
    LABEL.clear();
    BREAK.clear();
    LOOP.clear();
    ELSE.clear();
    old = "";       
  }
  /**
  register an instantiated object class named "objName"
  <br>If objName exists, old obj will be replaced.
  @param obj Object, instantiated object class
  @param objName String, name of object class
  */
  public void regClass(Object obj, String objName) {
    if (THIS == null) THIS = obj;
    if (!"this".equals(objName)) OBJ.put(objName, obj);
  }
  /**
  get an instantiated object class.
  @param objName String object class name
  @return Object null if unknown or an instantiated object class (must be cast)
  */
  public Object getClass(String objName) {
    return OBJ.get(objName);
  }
  /**
  remove Object class.
  @param objName String, 'this' or object class name
  @return Object or null
  */
  public Object removeClass(String objName) {
    if ("this".equals(objName)) THIS = null;
    return OBJ.remove(objName);
  }
  /**
  setCentroid set defuzzify Mode to Centroid
  @param centroid boolean, true for centroid. Default: surface
  */
  public void setCentroid(boolean centroid) {
    this.centroid = centroid;
  }
  /**
  stop Engine until it is resumed (see resume)
  */
  public void stop( ) {
    pause = true;
  }
  /**
  resume Engine (after stop)
  */
  public void resume( ) {
    pause = false;
  }
  /**
  Execute a Java Method
  @param java String, format: MyApp.JMethod(parm1, parm2, ...) or this.JMethod( .. )
  @return Object Java object (e.g. double, int, etc.)
  @exception Exception thrown by JAVA
  */
  public Object execJava(String java) throws Exception {
    int p = java.indexOf(".");
    if (p < 0) throw new Exception("Invalid Format: ClassName.MethodName(p1, p2,...)"+java);
    int a = java.indexOf("(", p);
    p = java.indexOf(")", a);
    if (a < 0 || p < 0 || a > p) throw new Exception("Invalid Format: missing '(' or ')':"+java);
    return callJava(java.substring(0, p));
  }
  //---------------------------------------------------------------------------------
  // register all FuzzyVariables and FuzzyData from 'declare'
  private String registerFVs( ) throws Exception {
    fuzzy = fuzzy.replaceAll("\r", "").trim();
    while (true) { // check FuzzyVariables
      int a = fuzzy.indexOf("declare");
      if (a < 0) break;
      //
      int e = fuzzy.indexOf("(", a);
      e = fuzzy.indexOf("(", e+1);
      while (true) {
        e = fuzzy.indexOf(")", e);
        int x = fuzzy.indexOf("(", e+1);
        int y = fuzzy.indexOf(")", e+1);
        
        if ((e+1) == y || x > y && y > 0) {
          e = y+1;
          break;
        } else {
          if (y < 0)
            throw new Exception("Expected )) for declare fvN(fdN(..),.., fdN()) at "+
                                fuzzy.substring(a, e)+"....");
          e = y;
        }
      }
      String S = fuzzy.substring(a, e);
      fuzzy = fuzzy.replace(S, ""); // remove this 'declare'
      S = S.replaceAll("\\,", " \\, ").replaceAll("\\)", " \\) ").replaceAll("\\(", " \\( ");
      while (S.indexOf("  ") >= 0) S = S.replaceAll("  ", " ");
      fExp = S.trim().split(" ");             
      buildFV();
    }
    idx = 0;
    // Check for literal string
    int a = fuzzy.indexOf("\"");
    if (a > 0)  for (int b = 0, e = 0, le = fuzzy.length(); ; ) {
      a = fuzzy.indexOf("\"");
      if (a < 0) return fuzzy;
      if (fuzzy.charAt(a-1) == '\\') {
        int x, i = a + 2;
        for (x = 0; i < le; i = x+1) {
          x = fuzzy.indexOf("\"", i);
          if (x < 0) break; // end
          if (fuzzy.charAt(x-1) != '\\') {
            x++; break; // found
          }
        }
        if (x < 0) throw new Exception("Missing closed quote (\"):"+fuzzy);
        e =  fuzzy.indexOf("\"", x);
      } else e = fuzzy.indexOf("\"", a+1);
      if (e < 0) throw new Exception("Missing closed quote (\"):"+fuzzy);
      String m = fuzzy.substring(a, e+1).replace("\\","");
      // replace the literal string with ?x
      fuzzy = fuzzy.replace(m, "?"+b+" ");
      // save ?x as name exclude the double quotes
      JOs.put("?"+b, m.substring(1, m.length()-1));
      ++b; // next index
    }
    return fuzzy;
  }
  // recursive Exec FuzzyCommands: Left side of IF
  private double synLeft( ) throws Exception {
    ++cnt;
    double val;
    double result = synIfValue( );
    char C = fExp[idx++].charAt(0);
    while (C == '&' || C == '|') {
      val = synIfValue( );
      if (C == '&') {
        if (result > val) result = val;  // and
      } else if (C == '|') {
        if (result < val) result = val;  // or
      } else throw new Exception("Unknown operation:"+C+" at line:"+atLine( ));
      C = fExp[idx++].charAt(0);
    }
    --cnt;
    return result;
  }
  private double synIfValue( ) throws Exception {
    String fvN = fExp[idx++];
    if (fvN.charAt(0) == '(') return synLeft( );
    if (!FVs.containsKey(fvN)) throw new Exception("Unknown FuzzyVariable "+fvN+
                                                   " at line:"+atLine( ));
    FuzzyVariable fv = FVs.get(fvN);                                               
    String op = fExp[idx++];
    String fdN = fExp[idx++];
    if (!fv.contains(fdN))
      throw new Exception(fdN+" is "+fvN+"-unknown at line:"+atLine());
    double d = fv.isFuzzy(fdN);
    switch(op) {
      case "is":   return d;              // is
      case "some": return d*d;            // some
      case "very": return Math.sqrt(d);   // very
      case "not":  return (1.0d - d);     // not
      default: 
      throw new Exception("Expected 'is' or 'some' or 'very' or 'not' at line:"+atLine( ));
    }
  }
  // Exec FuzzyCommands: Right side of IF
  private void synRight(double result) throws Exception {
    String fvN = fExp[idx++];
    if (!FVs.containsKey(fvN))
      throw new Exception("Unknown FuzzyVariable "+fvN+" at line:"+atLine());
    FuzzyVariable fv = FVs.get(fvN);
    String op  = fExp[idx++];
    String fdN = fExp[idx++];
    if (!fv.contains(fdN))
      throw new Exception(fdN+" is "+fvN+"-unknown at line:"+atLine());
    switch (op) {
      case "is":   fv.addPoint(fdN, result);
                   return;
      case "some": fv.addPoint(fdN, result*result);
                   return;
      case "very": fv.addPoint(fdN, Math.sqrt(result));
                   return;
      case "not":  fv.addPoint(fdN, 1.0d - result);
                   return;
      default: 
      throw new Exception("Expected 'is' or 'some' or 'very' or 'not' at line:"+atLine( ));
    }
  }
  // Syntax Check & Execution
  private void syntax( ) throws Exception {
    double result = 0;
    while (idx < fExp.length) {
      while (pause) try { // sleep 1 seconds
          TimeUnit.SECONDS.sleep(1);
      } catch (Exception ex) { }
      //
      lNo = idx; // @line
      String op = fExp[idx++];
      if ("if".equals(op)) {
        cnt = 0;
        if (isFuzzy()) { // fuzzy
          result = synLeft();
          if (cnt > 0) throw new Exception("Unbalanced bracket pair at line:"+atLine( ));
          --idx; // back to statement
        } else try { // conventional
          result = isCondition()? 1:0;
        } catch (Exception ex) {
          throw new Exception("Bad condition at line:"+atLine( ));
        }
        if ("then".equals(fExp[idx])) ++idx; // ignore 'then'
        int ie = atEnd("if", "else");  // index of else
        int ix = atEnd("if", "endif"); // index of endif
        if (ie < 0 && ix < 0) { // no else and no endif
          if (isFuzzy()) synRight(result); // Fuzzy on right
          else if (result == 0) idx = next(fExp[idx], idx+1);
        } else { // block with else or endif or both
          if (result == 0) { // conditions failed
            if (ie > 0) idx = ie;
            else { // no 'else', endif ?
              if (ix > 0) idx = ix; // endif
              else idx = next(fExp[idx], idx+1);
            }
          } else { // conditions met
            if (ie > 0) { // else block
              if (ix < 0) ix = next(fExp[ie], ie+1);
              ELSE.push(ix); // continue index @else
            }
          }
        }
      } else if ("endif".equals(op)) {
        result = 0;
      } else if ("else".equals(op)) {
        if (ELSE.isEmpty()) throw new Exception("Unexpected 'else' at line:"+atLine());
        result = 0;
        idx = ELSE.pop();
      } else if ("do".equals(op)) {
        if (!LOOP.contains(idx)) {
          LOOP.push(idx); // save Return Index after do
          int out = atEnd(op, "with");
          BREAK.push(out); // save end index
        }
      } else if ("with".equals(op)) {
        if (!"*".equals(fExp[idx])) onLoop(true);
        else idx = LOOP.peek(); // endless do-loop
      } else if ("while".equals(op)) {
        if (!LOOP.contains(idx-1)) {
          LOOP.push(idx-1); // save Return Index
          int out = atEnd("while", "endwhile");
          BREAK.push(out);  // save end index
        }
        if (!"*".equals(fExp[idx])) onLoop(false);
        else ++idx; // endless while-loop
      } else if ("endwhile".equals(op)) {
        idx = LOOP.peek();
      } else if ("break".equals(op)) {
        aBreak();
      } else if ("print".equals(op)) {
         print();
      } else if (op.indexOf(":") > 0) { // Java Variable
        char C = fExp[idx++].charAt(0);
        if (C == '=' || C == '+' || C == '-' || C == '*' || C == '/') {
          setValue(op, C, fExp[idx++]); // set Value to JavaVariable
        } else throw new Exception("Unknown operation '"+C+"' at line:"+atLine( ));
      } else if (op.indexOf(".") > 0) { // java Method
        prepareJava(op);
      } else if (FVs.containsKey(op)) {
        if ("is".equals(fExp[idx]) || "very".equals(fExp[idx]) ||
            "some".equals(fExp[idx]) || "not".equals(fExp[idx])) {
          --idx; // back to FV index
          synRight(result);
        } else {
          fvOperation(op);
        }
      } else if (JOs.containsKey(op)) {
        compute(op);
      } else if ("double".equals(op)||"string".equals(op)||
                 "long".equals(op)||"int".equals(op) ) {
        javaPrimitive(op); // index to double/string
      } else if ("set".equals(op)) {
        // set FV = value
        // or       FDName(a, b, c)
        // or       FDName(a, b, c, d)
        String fvN = fExp[idx++];
        if (!FVs.containsKey(fvN))
          throw new Exception("Unknown FuzzyVariable "+fvN+" at line:"+atLine());
        FuzzyVariable fv = FVs.get(fvN);
        op = fExp[idx++];
        if (!"=".equals(op)) throw new Exception("Expected = but found "+op+
                                                 " at Line:"+atLine());
        String fdN = fExp[idx++]; // FDName
        boolean neg = fdN.charAt(0) == '-';
        if (neg) fdN = fExp[idx++]; // FDName
        try {
          double d = Double.parseDouble(fdN);
          if (neg) fv.setSample(-d);
          else fv.setSample(d);
        } catch (Exception ex) {
          fv.set(buildFD(fdN));
        }
      } else if ("remove".equals(op)) {
        String fvN = fExp[idx++];
        if (!FVs.containsKey(fvN)) // remove fvN(fdN)
          throw new Exception("Unknown FuzzyVariable "+fvN+" at line:"+atLine());
        FuzzyVariable fv = FVs.get(fvN);
        if (fExp[idx++].charAt(0) != '(') 
          throw new Exception("Expected ( after "+fvN+"(...) at line:"+atLine());
        String fdN = fExp[idx++];
        if (fExp[idx++].charAt(0) != ')')
          throw new Exception("remove "+fvN+"("+fdN+" -> missing ) at line:"+atLine());
        if (!fv.contains(fdN))
          throw new Exception("remove "+fvN+"("+fdN+") -> unknown FuzzyData at line:"+atLine());
        fv.remove(fdN);
      } else if ("clear".equals(op)) {
        String fvN = fExp[idx++];
        if (fvN.charAt(0) == '*') { // clearEngine?
          clearEngine();
        } else {
          if (!FVs.containsKey(fvN))
            throw new Exception("Unknown FuzzyVariable "+fvN+" at line:"+atLine());
          FVs.get(fvN).clear( );
        }
      } else if ("reset".equals(op)) {
        String fvN = fExp[idx++];
        if (!FVs.containsKey(fvN))
          throw new Exception("Unknown FuzzyVariable "+fvN+" at line:"+atLine());
        FVs.get(fvN).reset( );
      } else if ("pause".equals(op)) {
        try { // sleep in milliseconds
          double d = (Double) getObject(fExp[idx++]);
          TimeUnit.MILLISECONDS.sleep((long)d);
        } catch (Exception ex) {        
          throw new Exception("Expected a value after 'pause' at line:"+atLine());
        }
      } else if ("exit".equals(op)) {
        return;
      } else if ("then".equals(op)) {
        throw new Exception("\"then\" without \"if\" at line:"+atLine());
      } else {
        String s = fExp[idx];
        if ("do".equals(s) || "while".equals(s)) {
          if (s.charAt(0) == 'w') LABEL.put(op, idx); // at while
          else LABEL.put(op, idx+1); // after do
        } else throw new Exception("Unknown "+op+" operation at line:"+atLine());
      }
    }
  }
  // condions for while/endwhile (false) and do/with (true)
  private void onLoop(boolean boo) throws Exception {
    if (isFuzzy()) { // FUZZY condition ?
      if (synLeft() > 0) idx = LOOP.peek();
      else {
        LOOP.pop();
        idx = BREAK.pop();        
      }
    } else { // CRISP condition 
      if (isCondition()) {
        if (boo) idx = LOOP.peek();
      } else {
        LOOP.pop();
        idx = BREAK.pop();        
      }
    }
  }
  // break of do, while
  private void aBreak( ) throws Exception {
    // check Label of while/do loop
    if (LABEL.containsKey(fExp[idx])) { 
      int q = LOOP.size();
      int p = q - LOOP.search(LABEL.get(fExp[idx]));
      if (p >= 0) { // include itself: i <= p
        for (int i = q; i > p; --i) {
          idx = BREAK.pop();
          LOOP.pop();
        }
      }
      return;
    }
    if (BREAK.isEmpty())
      throw new Exception("\"break\" without \"while\" or \"do\" at line:"+atLine());
    idx = BREAK.pop();
    LOOP.pop();
  }
  //
  private FuzzyData buildFD(String fdN) throws Exception {
    String op = fExp[idx++];
    if (!"(".equals(op)) throw new Exception("Expected ( but found "+op+
                                             " at Line:"+atLine());
    Double a = Double.parseDouble(fExp[idx++]);
    op = fExp[idx++];
    if (!",".equals(op)) throw new Exception("Expected , but found "+op+
                                             " at Line:"+atLine());
    Double b = Double.parseDouble(fExp[idx++]);
    op = fExp[idx++];
    if (!",".equals(op)) throw new Exception("Expected , but found "+op+
                                                   " at Line:"+atLine());
    Double c = Double.parseDouble(fExp[idx++]);
    op = fExp[idx++];
    // Pyramid FuzzyData left, top, right ?
    if (")".equals(op)) return new FuzzyData(fdN, a, b, c);
    if (!",".equals(op)) throw new Exception("Expected , but found "+op+
                                             " at Line:"+atLine());
    // Trapezoid FuzzyData: left, topLeft, topRight, right
    Double d = Double.parseDouble(fExp[idx++]);
    op = fExp[idx++];
    if (")".equals(op)) return new FuzzyData(fdN, a, b, c, d); 
    throw new Exception("Expected ) but found "+op+" at Line:"+atLine());                                           
  }
  // load declared FuzzyVariable
  private void buildFV() throws Exception {
    idx = 1;
    String fvN = fExp[idx++];
    FuzzyVariable fv = new FuzzyVariable(fvN);
    FVs.put(fvN, fv); // register FV
    // declare FV(FD, FD, ..)       
    if ("(".equals(fExp[idx])) {
      ++idx; // ignore bracket
      List<FuzzyData> fds = new ArrayList<>( );
      // until closed bracket is found
      while(idx < fExp.length) {
        String fdN = fExp[idx++];
        FDs.add(fdN);
        fv.add(buildFD(fdN));
        if (")".equals(fExp[idx++])) break;
        if (!",".equals(fExp[idx-1])) // NO next FD?           
          throw new Exception("Expected ')' or ',' but found:"+fExp[idx-1]+
                              " at Line "+atLine());
      }
    }
  }
  // next subsequent Statement
  private int next(String op, int ix) {
    for (String c : commands) if (c.equals(fExp[ix])) return ix;
    for (int i; ix < fExp.length; ++ix) {
      op = fExp[ix]; // check forcommand then Operator
      for (String c : commands) if (c.equals(op)) return ix;
      for (i = 0; i < OPs.length; ++i) if (OPs[i].equals(op)) break;
      if (i == OPs.length) { // no Operator found
        op = fExp[++ix]; // check for Statement, JO/FV
        for (String c : commands) if (c.equals(op)) return ix;
        if (op.indexOf(":") > 0 || op.indexOf(".") > 0 ||
            JOs.containsKey(op) || FVs.containsKey(op)) return ix;
      }
      
    }
    return ix;
  }
  //
  private int cnt;
  private boolean pause = false;
}
