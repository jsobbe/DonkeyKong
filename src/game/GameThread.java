package game;

import java.util.Random;

import general.ImageLoader;
import gui.LevelController;
import gui.MainApplication;
import javafx.animation.ScaleTransition;
import javafx.scene.paint.ImagePattern;
import javafx.util.Duration;
import objects.Player;

public class GameThread extends Thread{
	
	private Player player;
	private MainApplication main;
	private GameState gameState;
	private final Object pauseLock = new Object();
	private boolean paused = false;
	private int count = 0;
	private long startTime = 0;
	
    public void setPaused(boolean paused) {
		this.paused = paused;
	}

	public GameThread(){
		main = MainApplication.getMain();
		gameState = main.getGamestate();   
		player = gameState.getPlayer();
    }
    
    public void updatePlayer() {
    	player = gameState.getPlayer();
    }
	
    public void startTimer() {
    	startTime = System.currentTimeMillis();
    }
    
    public int stopTimer() {
    	return (int)(System.currentTimeMillis() - startTime)/1000;
    }
    
    private int calcTimeBonus(int time) {
    	if(30-time > 0) {
    		return (30-time)*50;
    	} else {
    		return 0;
    	}
    }
    
	@Override
	public synchronized void run() {
		Random rand = new Random();
		startTimer();
		while (gameState.isGameActive()) {
			if(count % (130- gameState.getLevel()*10) == 0){
				gameState.addBarrel();
				count = rand.nextInt(20)+1;
			}
			if(!player.isClimbing() && gameState.isControlsEnabled() && (player.isPressedKeyLeft() || player.isPressedKeyRight() || (player.getvSpeed() != 0.0 && player.getvPos() < 800))) {
				player.move();
				if(count % 10 == 0) {
					 player.switchPlayerImg(true);
				 }
			} else {
				 player.switchPlayerImg(false);
			}
			if(gameState.isControlsEnabled() && gameState.canClimb()){
				player.setCanClimb(true);
				if(player.isPressedKeyUp() || player.isPressedKeyDown()){
				  player.climb();
				}
			} else {
				player.setCanClimb(false);
				player.setClimbing(false);
			}
			
			if(gameState.checkForPlayerBarrelCollision()) {
				gameState.updatePlayerHealth();
			}
			
			if(gameState.hasReachedGoal()) {
				ScaleTransition disappear = new ScaleTransition(Duration.seconds(1.5), player.getShape());
				disappear.setToX(0.01);
				disappear.setToY(0.01);
				disappear.setOnFinished(e -> {
					gameState.addToScore(calcTimeBonus(stopTimer()));
					gameState.endGame(false);
				});	
				disappear.play();
				paused = true;
			}
		
			if(gameState.getHealthProperty().intValue() == 0) {
				main.getContrLevel().gameOver(gameState.getScore());
				gameState.endGame(true);
				paused = true;
			}
			
			synchronized (pauseLock) {
				if (paused) {
					try {
						pauseLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
			count++;
			
			try {
				sleep(33);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
			
		}
		System.out.println("Thread was stopped");
		
	}
	
	public void pauseThread() {
		paused = true;
	}
	
	public void resumeThread() {
		synchronized (pauseLock) {
			paused = false;
			count = -1;
			startTimer();
			pauseLock.notifyAll();
		}
	}

}
