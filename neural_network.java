import processing.core.*;

public class neural_network extends PApplet {

Population test;
PVector goal  = new PVector(400, 10);


@Override
public void setup() {
  // Size of the window
  frameRate(100); // Increase this to make the dots go faster
  test = new Population(1000); //Create a new population with 1000 members
}


@Override
public void draw() { 
  background(255);

  // Draw goal
  fill(255, 0, 0);
  ellipse(goal.x, goal.y, 10, 10);

  // Draw an obstacle
  fill(0, 0, 255);
  rect(0, 300, 600, 10);

  if (test.allDotsDead()) {
    // Genetic algorithm
    test.calculateFitness();
    test.naturalSelection();
    test.mutateClones();
  } else {
    // If any of the dots are still alive, then update and then show them
    test.update();
    test.show();
  }
}


@Override
public void settings() { size(800, 800); }


class Brain {
  PVector[] directions; // Series of vectors which get the dot to the goal (hopefully)
  int step = 0;

  Brain(int size) {
    directions = new PVector[size];
    randomize();
  }


  // Sets all the vectors in directions to a random vector with length 1
  public void randomize() {
    for (int i = 0; i< directions.length; i++) {
      float randomAngle = random(2*PI);
      directions[i] = PVector.fromAngle(randomAngle);
    }
  }


  // Returns a perfect copy of this brain object
  @Override
  public Brain clone() {
    Brain clone = new Brain(directions.length);
    for (int i = 0; i < directions.length; i++) {
      clone.directions[i] = directions[i].copy();
    }

    return clone;
  }


  // Mutates the brain by setting some directions to random vectors
  public void mutate() {
    float mutationRate = 0.1f; // Chance that any vector in directions gets changed
    for (int i =0; i< directions.length; i++) {
      float rand = random(1);
      if (rand < mutationRate) {
        //set this direction as a random direction 
        float randomAngle = random(2*PI);
        directions[i] = PVector.fromAngle(randomAngle);
      }
    }
  }
}


class Dot {
  PVector pos;
  PVector vel;
  PVector acc;
  Brain brain;

  boolean dead = false;
  boolean reachedGoal = false;
  boolean isBest = false; // True if this dot is the best dot from the previous generation

  float fitness = 0;

  Dot() {
    brain = new Brain(1000); //New brain with 1000 instructions

    // Start the dots at the bottom of the window with a no velocity or acceleration
    pos = new PVector((float) width /2, height- 10);
    vel = new PVector(0, 0);
    acc = new PVector(0, 0);
  }


  // Draws the dot on the screen
  public void show() {
    // If this dot is the best dot from the previous generation, then draw it as a big green dot
    if (isBest) {
      fill(0, 255, 0);
      ellipse(pos.x, pos.y, 8, 8);
    } else {
      fill(0);
      ellipse(pos.x, pos.y, 4, 4);
    }
  }


  // Moves the dot according to the brain directions
  public void move() {
    if (brain.directions.length > brain.step) {
      acc = brain.directions[brain.step];
      brain.step++;
    } else {
      dead = true;
    }

    //apply the acceleration and move the dot
    vel.add(acc);
    vel.limit(5);//not too fast
    pos.add(vel);
  }


  //calls the move function and check for collisions and stuff
  public void update() {
    if (!dead && !reachedGoal) {
      move();
      if (pos.x< 2|| pos.y<2 || pos.x>width-2 || pos.y>height -2) {
        dead = true;
      } else if (dist(pos.x, pos.y, goal.x, goal.y) < 5) {

        reachedGoal = true;
      } else if (pos.x < 600 && pos.y < 310 && pos.y > 300) {
        dead = true;
      }
    }
  }


  // Calculates the fitness
  public void calculateFitness() {
    if (reachedGoal) {
      fitness = 1.0f/16.0f + 10000.0f/(float)(brain.step * brain.step);
    } else {
      float distanceToGoal = dist(pos.x, pos.y, goal.x, goal.y);
      fitness = 1.0f/(distanceToGoal * distanceToGoal);
    }
  }


  // Clone it
  public Dot offspring() {
    Dot baby = new Dot();
    baby.brain = brain.clone();
    return baby;
  }
}


class Population {
  Dot[] dots;

  float fitnessSum;
  int gen = 1;

  int bestDot = 0;//the index of the best dot in the dots[]

  int minStep = 1000;


  Population(int size) {
    dots = new Dot[size];
    for (int i = 0; i< size; i++) {
      dots[i] = new Dot();
    }
  }


  // Show all the dots
  public void show() {
    for (int i = 1; i< dots.length; i++) {
      dots[i].show();
    }
    dots[0].show();
  }


  // Update all dots
  public void update() {
    for (Dot dot : dots) {
      // If the dot has already taken more steps than the best dot has taken to reach the goal
      if (dot.brain.step > minStep) {
        dot.dead = true;//then it dead
      } else {
        dot.update();
      }
    }
  }


  // Calculate all the fitness
  public void calculateFitness() {
    for (Dot dot : dots) {
      dot.calculateFitness();
    }
  }


  // Returns whether all the dots are either dead or have reached the goal
  public boolean allDotsDead() {
    for (Dot dot : dots) {
      if (!dot.dead && !dot.reachedGoal) {
        return false;
      }
    }

    return true;
  }


  // Gets the next generation of dots
  public void naturalSelection() {
    Dot[] newDots = new Dot[dots.length];//next gen
    setBestDot();
    calculateFitnessSum();

    // The champion lives on
    newDots[0] = dots[bestDot].offspring();
    newDots[0].isBest = true;
    for (int i = 1; i< newDots.length; i++) {
      // Select parent based on fitness
      Dot parent = selectParent();

      // Get baby from them
      newDots[i] = parent.offspring();
    }

    dots = newDots.clone();
    gen ++;
  }


  // Calculate the sum of fitness
  public void calculateFitnessSum() {
    fitnessSum = 0;
    for (Dot dot : dots) {
      fitnessSum += dot.fitness;
    }
  }


  // Chooses dot from the population to return randomly(considering fitness)
  // This function works by randomly choosing a value between 0 and the sum of all the fitness
  // then going through all the dots and add their fitness to a running sum,
  // and if that sum is greater than the random value generated, that dot is chosen
  //  since dots with a higher fitness function add more to the
  //  running sum then they have a higher chance of being chosen
  public Dot selectParent() {
    float rand = random(fitnessSum);
    float runningSum = 0;

    for (Dot dot : dots) {
      runningSum += dot.fitness;
      if (runningSum > rand) {
        return dot;
      }
    }
    return null;
  }


  // Mutates all the brains of the babies
  public void mutateClones() {
    for (int i = 1; i< dots.length; i++) {
      dots[i].brain.mutate();
    }
  }

  
  // Finds the dot with the highest fitness and sets it as the best dot
  public void setBestDot() {
    float max = 0;
    int maxIndex = 0;
    for (int i = 0; i< dots.length; i++) {
      if (dots[i].fitness > max) {
        max = dots[i].fitness;
        maxIndex = i;
      }
    }

    bestDot = maxIndex;

    // If this dot reached the goal, then reset the minimum number of steps it takes to get to the goal
    if (dots[bestDot].reachedGoal) {
      minStep = dots[bestDot].brain.step;
      println("", minStep);
    }
  }
}


public static void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "--present", "--window-color=#A2A2A2", "--stop-color=#FA0303", "neural_network" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
