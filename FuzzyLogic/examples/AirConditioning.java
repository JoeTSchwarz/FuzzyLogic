import fuzzylogic.FuzzyEngine;
// Joe T. Schwarz
public class AirConditioning {
  public AirConditioning( ) { }
  private void go(String... args) throws Exception {
    String script = "script/airconditioning.txt";
    outside = -10d;
    step = 0d;
    if (args.length == 1) script = args[0];
    else if (args.length > 1) {
      try {
        outside = Double.parseDouble(args[0]);
        if (args.length > 1) script = args[1];
      } catch (Exception ex) {
        script = args[0];
        outside = -10d;
      }
    } else {
      java.util.Random ran = new java.util.Random();
      step = (double)ran.nextInt(10);
      outside = (double)ran.nextInt(40);
      if (ran.nextInt(2) > 0) outside = -outside;
      if (ran.nextInt(2) > 0) step = -step;
    }
    // preset Room Temperature between -10 to 35 Celsius
    temp = outside < -10d? -10d:(outside > 32)? 32:outside;
    // FuzzyLogic..............................
    FuzzyEngine eng = new FuzzyEngine(this, "AirConditioning");
    // the values between 20d and 22d are the so-called LEARNED values
    long t = System.nanoTime();
    eng.execute(script);
    System.out.printf("Processing time:%.3f milliSec.\n", ((double)(System.nanoTime()-t))/1000000);
  }
  private void print( ) {
    System.out.printf("Invoke by FuzzyScript: Outside %2.2f Celsius, Room %.2f Celsius, AirCon %.2f\n",
                      outside, temp, step);
  }
  private void print(String msg) {
    System.out.println("Invoke by FuzzyScript: "+msg);
  }
  private void print(String msg, double d) {
    temp = d; // set the new temperature
    System.out.printf("Invoke by FuzzyScript: %s %.2f\n",msg,d);
  }
  private double achieve(double fs, double fr) {
    System.out.printf("Invoke by FuzzyScript: Outside:%2.2f, AirCon:%2.2f ("+
                      (fs > 0?"warming":"cooling")+"), RoomTemp:%2.2f\n", outside,fs, fr);
    return step;
  }
  private double temp, step, outside;
  //
  public static void main(String... args) throws Exception {
    AirConditioning air = new AirConditioning();
    air.go(args);
  }
}
