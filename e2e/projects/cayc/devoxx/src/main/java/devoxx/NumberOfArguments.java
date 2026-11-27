package devoxx;

public class NumberOfArguments {
  
  public int getAnimalSpeed(
      int numberOfPaws, int hairDensity, boolean isWet,
      boolean hasEaten, boolean isSleepy, boolean isHappy,
      boolean isAfraid, boolean isARabbit
      ) {
    int speed = numberOfPaws * hairDensity;
    if (isWet) {
      speed -= 10;
    }
    if (hasEaten) {
      speed -= 10;
    }
    if (isSleepy) {
      speed -= 10;
    }
    if (isHappy) {
      speed -= 10;
    }
    if (isAfraid) {
      speed -= 10;
    }
    if (isARabbit) {
      speed += 1000;
    }
    return speed;
  }
  
  public int getAnimalHapiness(boolean isWet,
      boolean hasEaten, boolean isSleepy,
      boolean isAfraid, boolean isARabbit) {
    int happiness = 42;
    if (isWet) {
      happiness -= 10;
    }
    if (hasEaten) {
      happiness -= 10;
    }
    if (isSleepy) {
      happiness -= 10;
    }
    if (isAfraid) {
      happiness -= 10;
    }
    if (isARabbit) {
      happiness += 1000;
    }
    return happiness;
  }
}
