package devoxx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Why there are some dead stores at line 23 and 26 ?
 */
public class QuizzDeadStore {

  private static final Logger LOG = LoggerFactory
      .getLogger(QuizzDeadStore.class);

  public void playWith(int number, int playerAge) {
    int amountOfMoney;
    switch (number) {
    case 0:
      amountOfMoney = 1_000;
      break;
    case 42:
      switch (playerAge) {
      case 42:
        amountOfMoney = 10_000; 
        break;
      default:
        amountOfMoney = 5_000; 
        break;
      }
    default:
      throw new RuntimeException("Game Over!");
    }
    give(amountOfMoney);
  }

  private void give(int amountOfMoney) {
    LOG.info("Yeah: " + amountOfMoney);
  }
}
