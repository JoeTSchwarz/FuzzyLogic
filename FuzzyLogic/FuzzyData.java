package fuzzylogic;
/**
FuzzyData (FD) for FuzzyVariable. Two kinds of FD:
<ul>
<li>Pyramid with 3 double values: Left, Top and right
<li>Trapezoid with 4 double values: Left, LeftTop, RightTop and right
</ul>
@author Joe T. Schwarz
*/
public class FuzzyData {
    /**
    Contructor, Pyramid FuzzyData
    @param name  String, FuzzyData name
    @param left  double, left value (must be less than top)
    @param top   double, Pyramid top
    @param right double, right value (must be greater than top)
    @exception Exception thrown by JAVA
    */
    public FuzzyData(String name, double left, double top,
                     double right) throws Exception {
        init(name, left, top, top, right);
    }
    /**
    Contructor, trapezoid FuzzyData
    @param name  String, FuzzyData name
    @param left  double, left value (must be less than lTop)
    @param lTop  double, the Trapezoidal left top
    @param rTop  double, the Trapezoidal right top (must be greater than lTop)
    @param right double, right value (must be greater than rTop)
    @exception Exception thrown by JAVA
    */
    public FuzzyData(String name, double left, double lTop,
                     double rTop, double right) throws Exception {
        init(name, left, lTop, rTop, right);
    }
    //
    private void init(String name, double left, double lTop,
                      double rTop, double right) throws Exception {
        if (lTop < left || rTop < lTop || rTop > right)
          throw new Exception("Invalid Data: "+left+", "+lTop+", "+rTop+", "+right);
        this.right = right;
        this.left = left;
        this.name = name; 
        this.lTop = lTop;
        this.rTop = rTop;       
        this.dLeft = lTop - left;
        this.dRight = right - rTop;
        point = 0;
        cnt = 0;
    }
    /**
    is equal to the given FuzzyData (FD)
    @param FD FuzzyData
    @return boolean true if equals
    */
    public boolean equals(FuzzyData FD) {
        return right  == FD.right  && 
               left   == FD.left   &&
               lTop   == FD.lTop   &&
               rTop   == FD.rTop   &&       
               dLeft  == FD.dLeft  &&
               dRight == FD.dRight &&
               cnt    == FD.cnt    &&
               point  == FD.point  &&
               name.equals(FD.getName());
    }
    /**
    get fuzzified value of sample X
    @param X double, sampled Value
    @return double, fuzzified value of X 
    */
    public double fuzzify(double X) {
        if (X <  left) return 0;
        if (X <  lTop) return (X-left)/dLeft;
        if (X <= rTop) return 1;
        if (X <  right) return (right-X)/dRight;
        return 0;
    }
    /**
    add a Point to this FuzzyData
    @param d double of FuzzyData Point
    */
    public void addPoint(double d) {
      point += d;
      ++cnt;
    }
    /**
    has this FuzzyData some Point
    @return boolean, true if Point exists
    */
    public boolean hasPoint() {
        return cnt > 0;
    }
    /**
    get fuzzified Point 
    @return double of fuzzified Point
    */
    public double getPoint() {
        return point / cnt;
    }  
    /**
    reset FD point
    */
    public void reset() {
        point = 0;
        cnt = 0;
    }
    /**
    get FuzzyData Centroid of FD (pyramid or trapezoid)
    @return a double of the FuzzyData samples
    */
    public double centroid() {
        return (0.5*(lTop - left + right - rTop)) + (rTop - lTop);
    }
    /**
    get FuzzyData name
    @return String, this FuzzyData name 
    */
    public  String getName() {
        return name;
    }
    /**
    get min value of FuzzyData
    @return the left value
    */
    public double getMin() {
        return left;
    }
    /**
    get max value of FuzzyData
    @return the right value
    */
    public  double getMax() {
        return right;
    }
    /**
    toString()
    @return String FD desciption
    */
    public String toString() {
      return name+"("+left+","+rTop+","+(rTop == lTop? "":lTop)+","+right+",points:"+point+")";
    }
    protected int cnt;
    protected String name;
    protected double left, right, lTop, rTop, dLeft, dRight, point;
}
