package vn.bnh.generator;

import java.util.Random;
import java.util.regex.Pattern;

public class ValueGenerator {
    private static final Random RANDOM = new Random();

    public static String getRandomData(boolean isFixed, String dataType, String sampleValue) {
        StringBuilder sampleValueSB = new StringBuilder(sampleValue);
        if (!isFixed) {
            int length = sampleValue.length();
            int randomIndex;

            do {
                randomIndex = RANDOM.nextInt(length);
            } while (!isValidCharacter(sampleValueSB.charAt(randomIndex)));
            char randomChar = switch (dataType) {
                case "string" -> (char) ('a' + RANDOM.nextInt(26));
                case "number" -> (char) ('0' + RANDOM.nextInt(10));
                default -> throw new IllegalStateException("Unexpected data type: " + dataType);
            };
            sampleValueSB.setCharAt(randomIndex, randomChar);
        }
        return dataType.equals("string") ? "'" + sampleValueSB + "'" : sampleValueSB.toString();
    }

    private static boolean isValidCharacter(char ch) {
        return Pattern.matches("[a-zA-Z0-9]", String.valueOf(ch));
    }
}
