// Java
import java.util.ArrayList;
import java.util.concurrent.*;
// JavaFX
import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.*;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.beans.binding.Bindings;
// FuzzyLogic
import fuzzylogic.FuzzyEngine;
// FuzzyLogic Controlled Unmanned Aerial Chopper
// Joe T. Schwarz (C)
public class FLDrone extends Application {
  /* no need
  public static void main(String... argv) {
    Application.launch();
  }
  */
  public void start(Stage stage) {
    // check for external script or run the predefined script/FLChopper.txt
    Application.Parameters params = getParameters();
    java.util.List<String> pList = params.getRaw();
    //
    if (pList.size() > 0) {
      String tmp = pList.get(0);
      if (tmp.endsWith(".css")) css = tmp;
      else script = tmp;
      if (pList.size() > 1) {
        tmp = pList.get(1);
        if (tmp.endsWith(".css")) css = tmp;
        else script = tmp;
      }
    }
    engine = new FuzzyEngine(this, "FLDrone");
    if (script == null) script = "script/FLDrone.txt";
    stage.setTitle("FuzzyLogic Controlled Drone -Joe T. Schwarz (C)-");
    scopter = new Image(getClass().getResourceAsStream("/scopter.jpg"));
    //scopter.getWidth() = 68; scopter.getHeight() = 45;
    Canvas screen = new Canvas(580, 500);
    gc = screen.getGraphicsContext2D();
    // set up MouseListener for Cargo and Obstacle
    screen.setOnMouseReleased(e -> {
      double X = e.getX(), Y = e.getY(); // Standing: scopterX: 516, scopterY: 455
      if (bussy) { // set Obstruction. ScopterHeight: 45, ScopterWidth: 68
        if (Y > 400) Y = 400;          // scopterY - scopterHeight - 10
        else if (Y < 50) Y = 50;       // scopterHeight + 5
        if (X > 480) X = 480;          // scopterX-(scopterWidth/2)
        else if (X < 34) X = 34;       // half scopterWidth 
        if (Y < (cY+22.5)) Y = cY+23; // haft scopterHeight
        oX = X;
        oY = Y;
        return;
      }
      reset();
      //
      if (X > (cargoX-10) || X < 10 || Y < DY || Y > scopterY) {
        home = -1;
        draw( );
        return;
      }
      home = 0;
      // min. cargo Height: 355
      if (Y > 355) Y = 355;
      cX = X; 
      cY = Y;
      pX = X; 
      pY = Y;
      if (mode) {
        loadXY(X, Y);
      } else {
        delivery = 0;
        dX = X; 
        dY = Y;
      }
      draw( );
    });
    // init the scene
    reset();
    draw( );
    START = new Button("START");
    START.setOnAction(a -> {
      if (mode && betaY == 0) return;
      ON = !ON;
      bussy = ON;
      if (ON) {
        fuzzy();
        START.setText("STOP");
        HOME.setDisable(true);
      } else {
        START.setText("START");
        HOME.setDisable(false);
        home = -1; // break
      }
    });
    HOME = new Button("HOME");
    HOME.setOnAction(a -> {
      reset();
      draw( );
    });
    //
    RadioButton MODE = new RadioButton("PICK-UP");
    MODE.setSelected(mode);
    MODE.setOnMouseReleased(e -> {
      if (bussy) return;
      mode = MODE.isSelected();
      MODE.setText(mode?"PICK-UP":"DELIVERY");
      reset( ); 
      draw( );
    });
    //
    HBox hBox = new HBox(20);
    hBox.setAlignment(Pos.CENTER);
    hBox.getChildren().addAll(START, MODE, HOME);
    //
    VBox root = new VBox(10);
    root.setAlignment(Pos.CENTER);
    root.setPadding(new Insets(5,5,5,5));
    root.getChildren().addAll(screen, hBox);
    Scene scene = new Scene(root, 590, 590);
    // either inline styling or via file css.css
    if (css == null || !(new java.io.File(css)).exists()) {
      setStyle(MODE);
      setStyle(HOME);
      setStyle(START);
      scene.getRoot().setStyle(vStyle);
    } else scene.getStylesheets().add(css);
    stage.setScene(scene);
    stage.show();
  }
  // clean up before terminated
  public void stop() {
    Platform.exit();
    System.exit(0);
  }
  // FuzzyEngine Thread
  private void fuzzy() {
    if (betaY > 0 || !mode) (new Thread() {
      public void run() {
        speed = 0;
        bussy = true;
        if (!mode) {
          cX = cargoX; cY = cargoY;
          START.setDisable(true);
          loadXY(cX, cY);
        }
        final double iX = cX, iY = cY;
        try {
          engine.execute(script);
        } catch (Exception ex) {
          ex.printStackTrace();
          System.exit(0);
        }
        // prevent the "Not on FX application thread" exception
        Platform.runLater(() -> {
          if (home > 1) {
            reset();
            ON = false;
            START.setDisable(false);
            START.setText("START");
            HOME.setDisable(false);
          } else { // it's STOP
            home = 0;
            bussy = false;
            sX = scopterX;
            sY = scopterY;
            if (!mode) {
              delivery = 0;
              cX = cargoX; cY = cargoY;
              loadXY(cX, cY);
            } else {
              pX = iX; pY = iY;
              loadXY(iX, iY);
            }
          }
          blocked = false;
          obstacle = 0;
          hY = 0;
        });
      }
    }).start();
  }
  //
  private void computeDelta( ) throws Exception {
   if (obstacle != 0) {
      if (home > 0) {
        hY = sY; // set this Height
        loadXY(sX - (obstacle > 20? 20:obstacle), sY+DY+10);
      } else loadXY(sX + (obstacle < 25? 25:obstacle), sY);
      blocked = true;
    }
    // PICK-UP: avoid collision with cargo
    if (!blocked && home == 0 && mode && deltaX <= 0.1 && deltaX >= -0.1 && (sY-25) < pY) {
      loadXY(sX+60, pY-10);
      blocked = true;
    }
    double x = sX - betaX;
    double y = sY - betaY;
    // Y-Axis Velocity
    if (x == 0) y = speed;
    else y = speed*Math.sin(Math.atan(y/x));
    deltaY += y;
    sY += y;
    //X-Axis Velocity
    if (y == 0) x = speed;
    else x = speed*Math.cos(Math.atan(y/x));
    deltaX += x;
    sX += x;
    draw( );
    // check the phases
    if (deltaY > MIN && deltaY < MAX && deltaX > MIN && deltaX < MAX) {
      if (blocked) { // on Obstacle ?
        loadXY(pX, pY);
        blocked = false;
      } else if (mode) { // pick-up cargo
        if (home > 0) {
          cX = sX+DX;
          cY = sY+DY;
          START.setDisable(true); // maneuver to parking lot
          java.util.concurrent.TimeUnit.MILLISECONDS.sleep(300);
          loadXY(scopterX+DX, scopterY+DY);
          ++home;
        } else {
          START.setDisable(true); // temporarily disable
          java.util.concurrent.TimeUnit.MILLISECONDS.sleep(300);
          loadXY(cargoX, cargoY); // Home Coordinates
          START.setDisable(false); // enable the STOP
          pX = cargoX;
          pY = cargoY;
          ++home;
        }
      } else { // deliver cargo
        if (home > 0) {
          ++home;
          cX = sX+DX;
          cY = sY+DY;
          START.setDisable(true);
          loadXY(scopterX+DX, scopterY+DY);
        } else {
          // temporarily disable
          START.setDisable(true);
          TimeUnit.MILLISECONDS.sleep(300);
          if (delivery > 0) { // return home
            loadXY(cargoX, cargoY);
            pX = cargoX;
            pY = cargoY;
            ++home;
          } else { // to client
            cX = dX; cY = dY;
            loadXY(cX, cY);
           }
          START.setDisable(false); // enable the STOP
          ++delivery;
        }
      }
    }
    TimeUnit.MILLISECONDS.sleep(25);
  }
  // invoked by FuzzyEngine
  private void print(String S, double X) {
    System.out.printf("%S-->>X: %3.2f\n",S,X);
  }
  //
  private double checkObstacle() {
    if (obstacle != 0) return 0;
    // on the way back
    if (home > 0 && hY > 0) {
      if (--cnt > 0) return 0;
      hY = 0; cnt = 0; // reset ALL
    }
    // 45: scopter height, 68 scopter width
    if (home >  0 && (sY+45) > oY && sY < (oY+22.5) && (sX-20) < oX ||
        home == 0 && sX < (oX+10) && (sX+10) > oX && sY < (oY+10)) {
      double d, y = Math.abs(sY - oY);
      int b = home > 0? 10:15;
      cnt = 100;
      d = sY-b-oY;
      if (d < b) d = b;
      if ((sX-b) < oX) d += b;
      return -(y+((sX+DX) > oX? d : d+10));
    }
    return 0;
  }
  //
  private void reset() {
    home = 0;
    speed = 0;
    cX = cargoX;
    cY = cargoY;
    oX = oY = 0;
    dX = dY = 0;
    obstacle = 0;
    delivery = 0;
    sX = scopterX;
    sY = scopterY;
    bussy = false;
  }
  // load relative Scopter to Cargo
  private void loadXY(double X, double Y) {
    speed = 0;
    betaY = Y-DY;
    betaX = X-DX;
    deltaY = sY - betaY;
    deltaX = sX - betaX;
    engine.clearEngine();
  }
  // paint the Canvas
  private void draw( ) {
    gc.setFill(Color.BLACK);
    gc.fillRect(0,0,580,580);
    gc.setFill(Color.RED);
    gc.drawImage(scopter, sX, sY);
    if (mode) { // pick-up mode
      if (home > 1) { // chopper home
        gc.fillOval(cX, cY, 15, 15);
      } else if (home > 0) { // cargo home
        gc.fillOval(sX+DX, sY+DY, 15, 15);
      } else { // on fetching cargo
        if (home < 0) { // outbound
          gc.setStroke(Color.RED);
          gc.fillOval(cargoX, cargoY, 15, 15);
          gc.strokeText("Invalid Cargo Location.", 200, 250);
        } else gc.fillOval(cX, cY, 15, 15);
      }
    } else { // delivery mode
      if (home > 0) { // chopper home
        gc.fillOval(dX, dY, 15, 15);
      } else { // on fetching cargo
        if (home < 0) { // outbound
          gc.setStroke(Color.RED);
          gc.fillOval(cargoX, cargoY, 15, 15);
          gc.strokeText("Invalid Cargo Location.", 200, 250);
        } else {
          if (delivery > 0) {
            gc.fillOval(sX+DX, sY+DY, 15, 15);
          } else gc.fillOval(cargoX, cargoY, 15, 15);
        }
      }
      if (dX > 0) {
        gc.setFill(Color.WHITE);
        gc.fillOval(dX+5, dY+5, 5, 5);
      }
    }
    gc.setLineWidth(1);
    gc.setStroke(Color.YELLOW);
    gc.strokeText("FuzzyLogic Controlled Drone - Joe T Schwarz (C) -", 124, 15);
    gc.strokeText(String.format("Speed %3.2f, Chopper %3.2f / %3.2f (cargo %3.2f / %3.2f)",
                                speed, sX, sY, cX, cY), 108, 25);
    gc.setStroke(Color.CYAN);
    gc.strokeText(String.format("Distance to TARGET and GOAL  %3.2f / %3.2f",
                                deltaX, deltaY),5, 495);
    if (oX != 0) {
      gc.setFill(Color.web("#a9ff00"));
      gc.fillOval(oX, oY, 20, 20);
    }
  }
  // for the inline settings
  private void setStyle(ButtonBase but) {
     but.setStyle(bStyle);
     but.styleProperty().bind(Bindings.when(but
                                      .hoverProperty())
                                      .then(hover)
                                      .otherwise(bStyle));
  }
  //
  private Image scopter;
  private double dX, dY;
  private String script, css;
  private Button START, HOME;
  private FuzzyEngine engine;
  private GraphicsContext gc;
  private boolean blocked = false;
  private int DX = 23, DY = 35, delivery = 0, cnt;
  private volatile boolean ON = false, bussy = false, mode = true;
  private double betaY, betaX, home, oX, oY, obstacle, pX, pY, hY;
  private double cX, cY, sX,sY,deltaX,deltaY,speed,MIN = -0.8d,MAX = 0.8d;
  private double cargoX = 485, cargoY = 482, scopterX = 516, scopterY = 455;
  private String hover  = "-fx-background-color: #afeeee;"; // paleturquoise
  private String vStyle = "-fx-background-color: burlywood;-fx-font-size: 11pt;-fx-base: silver;";
  private String bStyle = "-fx-background-color:linear-gradient(#f0ff35, #a9ff00),"+
                          "  radial-gradient(center 50% -40%, radius 200%, #b8ee36 45%, #80c800 50%);"+
                          "-fx-background-radius: 6, 5;-fx-background-insets: 0, 1;"+
                          "-fx-effect: dropshadow(three-pass-box ,rgba(0,0,0,0.4) , 5, 0.0 , 0 , 1);"+
                          "-fx-text-fill: #395306;";
}
