package util;

import java.util.Random;

/**
 * Created by mattia on 17/03/17.
 */
public class RandomGen {

    public static int getRandomIntInRange(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }
}
