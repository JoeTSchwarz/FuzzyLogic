import java.util.*;
import fuzzylogic.FuzzyEngine;
public class Fuzzy {
  public static void main(String... args) throws Exception {
    new Fuzzy(args);
  }
  public Fuzzy(String... args) throws Exception {
    FuzzyEngine engine = new FuzzyEngine(this, "Test");
    if (args.length > 1) engine.setCentroid(true);
    engine.execute(args[0]);
  }
}
