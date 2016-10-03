package org.opengis.cite.iso19142.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Provides utility methods to randomly generate test inputs.
 */
public class Randomizer {

    private static final int MIN_CODE_POINT = 65;
    private static final int MAX_CODE_POINT = 122;
    private static final int WORD_LENGTH = 10;

    /**
     * Generates a sequence of of (space-separated) words, each of which
     * contains a random sequence of letter characters in the range [A-Za-z].
     * 
     * @param numWords
     *            The number of words in the sequence (1..10).
     * @return A String consisting of one or more words.
     */
    public static String generateWords(int numWords) {
        if (numWords < 1 || numWords > 10) {
            numWords = 1;
        }
        StringBuilder words = new StringBuilder();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < numWords; i++) {
            int charPos = 0;
            do {
                int codePoint = rnd.nextInt(MIN_CODE_POINT, MAX_CODE_POINT + 1);
                if (codePoint < 91 || codePoint > 96) {
                    words.append(Character.toChars(codePoint));
                    charPos++;
                }
            } while (charPos < WORD_LENGTH);
            words.append(' ');
        }
        return words.toString().trim();
    }
}
