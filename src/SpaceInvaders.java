import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import javafx.scene.image.Image;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * @author Linh Hoang
 * @version 1.0
 * @date 2024-12-22
 */
public class SpaceInvaders extends Application
{
   private static final Random RAND = new Random();
   private static final int WIDTH         = 800;
   private static final int HEIGHT        = 600;
   private static final int PLAYERS_SIZE  = 60;

   static final int EXPLOSION_W     = 128;
   static final int EXPLOSION_ROWS  = 3;
   static final int EXPLOSION_COLS  = 3;
   static final int EXPLOSION_H     = 128;
   static final int EXPLOSION_STEPS = 15;

   static final Image PLAYER_IMG    = new Image("/resources/player.png");
   static final Image EXPLOSION_IMG = new Image("/resources/explosion.png");

   static final Image[] BOMBS_IMG = {
           new Image("/resources/1.png"),
           new Image("/resources/2.png"),
           new Image("/resources/3.png"),
           new Image("/resources/4.png"),
           new Image("/resources/5.png"),
           new Image("/resources/6.png"),
           new Image("/resources/7.png"),
           new Image("/resources/8.png"),
           new Image("/resources/9.png"),
           new Image("/resources/10.png"),
   };

   static final int MAX_BOMB = 10;
   static final int MAX_SHOTS = MAX_BOMB * 2;

   private boolean gameOver = false;
   private GraphicsContext gc;
   private double mouseX;
   private int score;

   private Rocket player;
   private List<Shot> shots;
   private List<Universe> univ;
   private List<Bomb> bombs;


   @Override
   public void start(final Stage stage) {
      final Canvas canvas;
      final Timeline timeline;

      canvas = new Canvas(WIDTH, HEIGHT);
      gc = canvas.getGraphicsContext2D();

      timeline = new Timeline(new KeyFrame(Duration.millis(100), e -> run(gc)));
      timeline.setCycleCount(Timeline.INDEFINITE);
      timeline.play();
      canvas.setCursor(Cursor.MOVE);
      canvas.setOnMouseMoved(e -> mouseX = e.getX());
      canvas.setOnMouseClicked(e -> {
         if(shots.size() < MAX_SHOTS)
         {
            shots.add(player.shoot());
         }

         if(gameOver)
         {
            gameOver = false;
            setUp();
         }
      });

      setUp();
      stage.setScene(new Scene(new StackPane(canvas)));
      stage.setTitle("Space Invaders");
      stage.show();
   }

   // Set up the game
   private void setUp()
   {
      univ = new ArrayList<>();
      shots = new ArrayList<>();
      bombs = new ArrayList<>();
      player = new Rocket(WIDTH / 2, HEIGHT - PLAYERS_SIZE, PLAYERS_SIZE, PLAYER_IMG);
      score = 0;
      IntStream.range(0, MAX_BOMB).mapToObj(i -> this.newBomb()).forEach((bombs::add));
   }

   private void run(final GraphicsContext gc)
   {
      gc.setFill(Color.grayRgb(20));
      gc.fillRect(0, 0, WIDTH, HEIGHT);
      gc.setTextAlign(TextAlignment.CENTER);
      gc.setFont(Font.font(20));
      gc.setFill(Color.WHITE);
      gc.fillText("Score: " + score, 60, 20);

      if(gameOver)
      {
         gc.setFont(Font.font(35));
         gc.setFill(Color.YELLOW);
         gc.fillText("Game Over \n Your Score is " + score + "\n Click to play again", (double) WIDTH / 2, HEIGHT / 2.5);
      }

      univ.forEach(Universe::draw);

      player.update();
      player.draw();
      player.posX = (int) mouseX;

      bombs.stream()
              .peek(Rocket::update)
              .peek(Rocket::draw)
              .forEach(e -> {
                 if(player.collide(e) && !player.exploding)
                  {
                     player.explode();
                  }
      });

      for(int i = shots.size() - 1; i >= 0; i--)
      {
         final Shot shot = shots.get(i);

         if(shot.posY < 0 || shot.toRemove)
         {
            shots.remove(i);
            continue;
         }

         shot.update();
         shot.draw();

         for(final Bomb bomb : bombs)
         {
            if(shot.collide(bomb) && !bomb.exploding)
            {
               score++;
               bomb.explode();
               shot.toRemove = true;
            }
         }
      }

      for(int i = bombs.size() - 1; i >= 0; i--)
      {
         if(bombs.get(i).destroyed)
         {
            bombs.set(i, newBomb());
         }
      }

      gameOver = player.destroyed;

      if(RAND.nextInt(10) > 2)
      {
         univ.add(new Universe());
      }

      univ.removeIf(u -> u != null && u.posY > HEIGHT);
   }

   // Player
   public class Rocket
   {
      int posX, posY, size;
      boolean exploding, destroyed;
      Image img;
      int explosionStep = 0;

      public Rocket(final int posX,
                    final int posY,
                    final int size,
                    final Image image)
      {
         this.posX = posX;
         this.posY = posY;
         this.size = size;
         this.img = image;
      }

      public Shot shoot()
      {
         return new Shot(posX + size / 2 - Shot.SIZE / 2, posY - Shot.SIZE);
      }

      public void update()
      {
         if(exploding)
         {
            explosionStep++;
         }

         destroyed = explosionStep > EXPLOSION_STEPS;
      }

      public void draw()
      {
         if(exploding)
         {
            gc.drawImage(EXPLOSION_IMG, explosionStep % EXPLOSION_COLS * EXPLOSION_W,
                    ((double) explosionStep / EXPLOSION_ROWS) * EXPLOSION_H + 1,
                        EXPLOSION_W, EXPLOSION_H,
                        posX, posY, size, size);
         }
         else
         {
            gc.drawImage(img, posX, posY, size, size);
         }
      }

      public boolean collide(final Rocket other)
      {
         final int d = distance(this.posX + size / 2, this.posY + size / 2,
                              other.posX + other.size / 2, other.posY + other.size / 2);

         return d < other.size / 2 + this.size / 2;
      }

      public void explode()
      {
         exploding = true;
         explosionStep = -1;
      }
   }

   // Computer Player
   public class Bomb extends Rocket
   {
      final int SPEED = (score / 5) + 2;

      public Bomb(final int posX, final int posY, final int size, final Image image)
      {
         super(posX, posY, size, image);
      }

      public void update()
      {
         super.update();

         if(!exploding && !destroyed)
         {
            posY += SPEED;
         }

         if(posY > HEIGHT)
         {
            destroyed = true;
         }
      }
   }

   // bullets
   public class Shot
   {
      private static final int SIZE = 6;

      public boolean toRemove;

      int posX, posY;
      int speed = 10;

      public Shot(final int posX, final int posY)
      {
         this.posX = posX;
         this.posY = posY;
      }

      public void update()
      {
         posY -= speed;
      }

      public void draw()
      {
         gc.setFill(Color.RED);

         if(score >= 50 && score <= 70 || score >= 120)
         {
            gc.setFill(Color.YELLOWGREEN);
            speed = 50;
            gc.fillRect(posX - 5, posY - 10, SIZE + 10, SIZE + 30);
         }
         else
         {
            gc.fillOval(posX, posY, SIZE, SIZE);
         }
      }

      public boolean collide(final Rocket rocket)
      {
         final int distance = distance(this.posX + SIZE / 2, this.posY + SIZE / 2,
                                    rocket.posX + rocket.size / 2, rocket.posY + rocket.size / 2);

         return distance < rocket.size / 2 + SIZE / 2;
      }
   }

   // Environment
   public class Universe
   {
      private final int h;
      private final int w;
      private final int r;
      private final int g;
      private final int b;

      private double opacity;
      private int posX;
      private int posY;

      public Universe()
      {
         posX = RAND.nextInt(WIDTH);
         posY = 0;
         w = RAND.nextInt(5) + 1;
         h = RAND.nextInt(5) + 1;
         r = RAND.nextInt(100) + 150;
         g = RAND.nextInt(100) + 150;
         b = RAND.nextInt(100) + 150;
         opacity = RAND.nextFloat();

         if(opacity < 0)
         {
            opacity *= -1;
         }

         if(opacity > 0.5)
         {
            opacity = 0.5;
         }
      }

      public void draw()
      {
         if(opacity > 0.8)
         {
            opacity -= 0.01;
         }

         if(opacity < 0.1)
         {
            opacity += 0.01;
         }

         gc.setFill(Color.rgb(r, g, b, opacity));
         gc.fillOval(posX, posY, w, h);
         posY += 20;
      }
   }

   Bomb newBomb()
   {
      return new Bomb(50 + RAND.nextInt(WIDTH - 100), 0, PLAYERS_SIZE,
              BOMBS_IMG[RAND.nextInt(BOMBS_IMG.length)]);
   }

   int distance(final int x1, final int y1, final int x2, final int y2)
   {
      return (int) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
   }

   public static void main(final String[] args)
   {
      launch(args);
   }
}

