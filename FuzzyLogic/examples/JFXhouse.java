//
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
//
import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.text.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.*;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.scene.control.ButtonBar.ButtonData;
//
import fuzzylogic.FuzzyEngine;
// Joe T. Schwarz
public class JFXhouse extends Application {
  // JFX start
  public void start(Stage stage) {
    stage.setTitle("JFXhouse controlled by FuzzyLogic");
    ran = new java.util.Random();
    // FuzzyLogic..............................
    eng = new FuzzyEngine(this, "JFXhouse");
    //
    Canvas canvas = new Canvas(400, 200);
    gc = canvas.getGraphicsContext2D();
    oTf = new TextField();
    rTf = new TextField();
    oTf.setPrefSize(50, 20);
    rTf.setPrefSize(50, 20);
    oTf.setEditable(false);
    rTf.setEditable(false);
    //
    fz = true;
    slider = new Slider();
    slider.setMin(-10d);
    slider.setMax(10d);
    slider.setValue(0);
    slider.setMajorTickUnit(2.5);
    slider.setShowTickLabels(true);
    slider.setShowTickMarks(true);        
    slider.setBlockIncrement(0.5d);
    slider.setStyle("-fx-base: #f08080;");
    
    vlab = new Label("AirCondition scale: "+slider.getValue());
    slider.valueProperty().addListener((obs, oldVal, newVal)-> {
      if (fz) { // FUZZY at work
        slider.setValue(scale);
        return;
      }
      scale = (Double) newVal;
      // scale: 20/(40+40) = 0.125
      if (temp < 0) temp += Math.abs(scale * 0.5);
      else if (temp < 10) temp += Math.abs(scale * 0.25);
      else if (temp < 19.5) temp += Math.abs(scale * 0.125);
      else if (temp < 25) temp -= Math.abs(scale * 0.125);
      else if (temp < 30) temp -= Math.abs(scale * 0.25);
      else temp -= Math.abs(scale * 0.5);
      rTf.setText(String.format("%.2f", temp));
      vlab.setText("Manual AirCondition scale: " + String.format("%.2f", scale));
      paint(String.format("Outside: %.2f C. Room's Temp %.2f C. AirCon: %.2f",
                          outside, temp, scale));
    });
    Button FUZZY = new Button("FUZZY");
    Button MAN = new Button("MANUAL");
    MAN.setPrefWidth(80);
    MAN.setOnAction(e -> {
      FUZZY.setDisable(true);
      MAN.setDisable(true);
      fz = false;
    });    
    FUZZY.setPrefWidth(80);
    FUZZY.setOnAction(e -> {
      try {
        FUZZY.setDisable(true);
        MAN.setDisable(true);
        eng.execute("script/thishouse.txt");
        // present the Fuzzy Results scale, temp
        vlab.setText("Fuzzy AirCondition scale: " + String.format("%.2f", scale));
        rTf.setText(String.format("%.2f", temp));
        slider.setValue(scale);
      } catch (Exception ex) {
        ex.printStackTrace();
        stop();
      }
      MAN.setDisable(false);
      FUZZY.setDisable(false);
    });
    
    Button NEW = new Button("RESET");
    NEW.setPrefWidth(80);
    NEW.setOnAction(e -> {
      FUZZY.setDisable(false);
      MAN.setDisable(false); 
      fz = false;
      setTemp();
    });
    
    HBox hbb = new HBox();    
    hbb.setPadding(new Insets(5));
    hbb.setSpacing(5);
    hbb.setAlignment(Pos.CENTER);
    hbb.getChildren().addAll(new Label("Outside"), oTf, FUZZY, MAN, NEW, new Label("Room"), rTf);
    //
    Label lab = new Label("<--COOL--  Air Conditioning  --WARM-->");
    
    VBox vbox = new VBox();
    vbox.setBackground(new Background(new BackgroundFill(Color.web("#f0f8ff"),
                                                          CornerRadii.EMPTY,
                                                          Insets.EMPTY)));
    vbox.getChildren().addAll(hbb, canvas, lab, slider, vlab);
    vbox.setAlignment(Pos.CENTER);
    //
    Scene scene = new Scene(vbox, 500, 360);
    stage.setResizable(false);
    stage.setScene(scene);
    stage.setX(30); 
    stage.setY(30);  
    stage.show();
    // set Outside temperature
    setTemp();
  }
  public void stop() {
    Platform.exit();
    System.exit(0);
  }
  //
  private void setTemp() {
    fz = true;
    scale = 0;
    slider.setValue(0);
    outside = (double)ran.nextInt(40);
    if (ran.nextInt(2) > 0) outside = -outside;
     // 0.85 % Isolation
    if (outside < 18) temp = outside * 0.85;
    else temp = outside;
    //
    eng.clearEngine(); // clear FD
    rTf.setText(String.format("%.2f", temp));
    vlab.setText("AirCondition sacle: 0:00");
    oTf.setText(String.format("%.2f", outside));
    paint(String.format("Outside: %.2f Celcius. Room temparature: %.2f Celcius.",
                         outside, temp
                        )
         );
    slider.requestFocus();   
  }
  //
  private void paint(String msg) {
    if (gc == null) return;
    StringBuilder note = new StringBuilder("It's ");
    if (temp < 5) {
      gc.setFill(Color.MEDIUMBLUE);
      gc.fillRect(0, 0, 380, 200);
      note.append("COLD. ");
      gc.setStroke(Color.WHITE);
    } else if (temp <  19) {
      gc.setFill(Color.PALETURQUOISE);
      gc.fillRect(0, 0, 380, 200);
      gc.setStroke(Color.MEDIUMBLUE);
      note.append("COOL. ");
    } else if (temp <  22) {
      gc.setFill(Color.FLORALWHITE);
      gc.fillRect(0, 0, 380, 200);
      gc.setStroke(Color.DARKSEAGREEN);
      note.append("FAIR. ");
    } else if (temp <= 24) {
      gc.setFill(Color.PINK);
      gc.fillRect(0, 0, 380, 200);
      gc.setStroke(Color.DARKRED);
      note.append("WARM. ");
    } else {
      gc.setFill(Color.DARKRED);
      gc.fillRect(0, 0, 380, 200);
      gc.setStroke(Color.YELLOW);
      note.append("HOT. ");
    }
    gc.strokeText(note.toString()+msg, 5, 90);
  }
  //--------------------- invoke by FuzzyScript
  private void print( ) {
    paint(String.format("Outside %.2f Celcius. Room %.2f Celcius.", outside, temp));
  }
  private void print(String msg) {
    rTf.setText(String.format("%.2f", temp));
    paint(msg+String.format(" (Room:%.2f)", temp));
  }
  private void print(String msg, double d) {
    temp = d; // set the new temperature
    rTf.setText(String.format("%.2f", temp));
    paint(msg+String.format(" %.2f", d));
  }
  // adjust the AirConditioning scale
  private double adjust(double d) {
    // Cooling: -40 .. +20: 10/60 = 0.1667
    // Heating: +20 .. +40: 10/20 = 0.5
    temp = d;
    if (temp < 20) scale += 0.16667;
    else if (temp > 22) scale -= 0.5;
    return scale;
  }
  //
  private boolean fz;
  private Label vlab;
  private Slider slider;
  private FuzzyEngine eng;
  private TextField oTf, rTf;
  private GraphicsContext gc;
  private java.util.Random ran;
  private double temp, scale, outside;
}
