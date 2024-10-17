package org.opengis.cite.iso19142.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VerifyRandomizer {

	@Test
	public void generate2Words() {
		String words = Randomizer.generateWords(2);
		assertEquals("Unexpected number of words", 2, words.split("\\s").length);
	}

	@Test
	public void generate10Words() {
		String words = Randomizer.generateWords(10);
		System.out.println(words);
		assertEquals("Unexpected number of words", 10, words.split("\\s").length);
	}

	@Test
	public void generate11Words() {
		String words = Randomizer.generateWords(11);
		assertEquals("Unexpected number of words", 1, words.split("\\s").length);
	}

}
