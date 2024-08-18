import fuzzylogic.FuzzyCluster;
// working with FuzzyCluster
// Joe T. Schwarz
public class ClusterHouse {
  private double tempA, tempB, stepA, stepB, outsideA, outsideB;
  private java.util.Random ran  = new java.util.Random();
  //
  public static void main(String... argv) throws Exception {
    new ClusterHouse( );
  }
  //
  public ClusterHouse( ) throws Exception {
    java.util.Random ran = new java.util.Random();
    outsideA = setValue(40); stepA = 0;
    outsideB = setValue(40); stepB = 0;
    // preset Room Temperature from outside
    tempA = outsideA * 0.9;
    tempB = outsideB * 0.9;
    // FuzzyLogic..............................
    FuzzyCluster cluster = new FuzzyCluster("ClusterHouse");
    cluster.createFuzzyEngine(this, "House_A");
    cluster.createFuzzyEngine(this, "House_B");
    //
    new Thread() {
      public void run() {
        try {
           System.out.printf("Outside A: %.2f Celsius, AirCon: %.2f\n",outsideA, stepA);
           cluster.execute("House_A", "script/house_A.txt");
        } catch (Exception ex) { ex.printStackTrace(); }
      }
    }.start( );
    //
    new Thread() {
      public void run() {
        try {
          System.out.printf("Outside B: %.2f Celsius, AirCon: %.2f\n",outsideB, stepB);
          cluster.execute("House_B", "script/house_B.txt");
        } catch (Exception ex) { ex.printStackTrace(); }
      }
    }.start( );
    //
  }
  //
  private double setValue(int i) {
    double d = (double)ran.nextInt(i);
    if (ran.nextInt(2) > 0) d = -d;
     // 0.85 % Isolation
    return d;
  }
  // 0: stepA, 1: stepB
  private double getStep(double d, int house) { 
    double step = house == 0? stepA:stepB;
    // Cooling: -40 .. +20: 10/60 = 0.1667
    // Heating: +20 .. +40: 10/20 = 0.5
    if (d < 20) step += 0.16667;
    else if (d > 22) step -= 0.5;
    if (house == 0) {
      stepA = step;
      tempA = d;
    } else {
      stepB = step;
      tempB = d;
    }
    return step;
  }
  //
  private void print(String msg, double temp, double step) {
    System.out.printf("Invoke by FuzzyScript. "+msg+" Room %.2f Celsius, AirCon %.2f\n", temp, step);
  }
}
