package com.aghakhani.flappybirdgame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import android.media.MediaPlayer;

public class GameView extends SurfaceView implements Runnable {
    private MediaPlayer mediaPlayer;
    private Thread gameThread;
    private boolean isPlaying;
    private SurfaceHolder holder;
    private Paint paint;

    private int screenX, screenY;
    private int birdX, birdY;
    private int birdSize = 50;
    private int birdVelocity = 0;
    private int gravity = 2;

    private ArrayList<Rect> obstacles;
    private int obstacleWidth = 200; // Width of the obstacle
    private int gapHeight = 400; // Space between top and bottom obstacles
    private int obstacleSpeed = 10;
    private int obstacleSpawnTime = 200; // Obstacle generation interval (frames)

    private Random random;
    private int score = 0;
    private boolean gameOver = false;
    private int frameCount = 0;

    public GameView(Context context) {
        super(context);
        mediaPlayer = MediaPlayer.create(this.getContext(), R.raw.lose_sound);
        holder = getHolder();
        paint = new Paint();
        random = new Random();

        screenX = getResources().getDisplayMetrics().widthPixels;
        screenY = getResources().getDisplayMetrics().heightPixels;

        birdX = screenX / 4;
        birdY = screenY / 2;

        obstacles = new ArrayList<>();
    }

    @Override
    public void run() {
        while (isPlaying) {
            update();
            draw();
            sleep();
        }
    }

    private void update() {
        if (gameOver) return;

        // Update bird movement
        birdVelocity += gravity;
        birdY += birdVelocity;

        // Prevent bird from going off-screen
        if (birdY < 0) birdY = 0;
        if (birdY + birdSize > screenY) birdY = screenY - birdSize;

        // Update obstacles and remove passed ones
        Iterator<Rect> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Rect obstacle = iterator.next();
            obstacle.left -= obstacleSpeed;
            obstacle.right -= obstacleSpeed;

            if (obstacle.right < 0) {
                iterator.remove();
                score++;
            }
        }

        // Generate obstacles at specific intervals
        if (frameCount % obstacleSpawnTime == 0) {
            int minHeight = screenY / 6;
            int maxHeight = screenY - gapHeight - minHeight;

            int topHeight = random.nextInt(maxHeight - minHeight) + minHeight;
            int bottomY = topHeight + gapHeight;

            // Ensure obstacle heights are within valid range
            topHeight = Math.max(topHeight, minHeight);
            bottomY = Math.min(bottomY, screenY - minHeight);

            // Create top and bottom obstacles
            obstacles.add(new Rect(screenX, 0, screenX + obstacleWidth, topHeight));
            obstacles.add(new Rect(screenX, bottomY, screenX + obstacleWidth, screenY));
        }

        frameCount++;

        // Check for collision
        Rect birdRect = new Rect(birdX, birdY, birdX + birdSize, birdY + birdSize);
        for (Rect obstacle : obstacles) {
            if (Rect.intersects(birdRect, obstacle)) {
                stopGame();
            }
        }
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();
            canvas.drawColor(Color.WHITE);

            // Draw bird
            paint.setColor(Color.RED);
            canvas.drawRect(birdX, birdY, birdX + birdSize, birdY + birdSize, paint);

            // Draw obstacles
            paint.setColor(Color.GREEN);
            for (Rect obstacle : obstacles) {
                canvas.drawRect(obstacle, paint);
            }

            // Draw score
            paint.setColor(Color.BLACK);
            paint.setTextSize(50);
            canvas.drawText("Score: " + score, 50, 100, paint);

            // Draw game over text if the game ends
            if (gameOver) {
                paint.setTextSize(100);
                canvas.drawText("Game Over", screenX / 4, screenY / 2, paint);
            }

            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(16);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        isPlaying = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void pause() {
        try {
            isPlaying = false;
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopGame() {
        isPlaying = false;
        gameOver = true;
        mediaPlayer.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) {
            restartGame();
        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            birdVelocity = -20;
        }
        return true;
    }

    private void restartGame() {
        score = 0;
        birdY = screenY / 2;
        birdVelocity = 0;
        obstacles.clear();
        gameOver = false;
        resume();
    }
}
