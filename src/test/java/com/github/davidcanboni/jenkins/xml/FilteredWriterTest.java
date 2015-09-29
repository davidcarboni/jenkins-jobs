package com.github.davidcanboni.jenkins.xml;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Created by david on 29/09/2015.
 */
public class FilteredWriterTest {

    String source = "The quick brown fox jumped over the lazy dog";
    StringWriter target;

    @Before
    public void before() {
        target = new StringWriter();
    }

    @Test
    public void shouldFilterStart() throws IOException {

        // Given
        String filtered = "The";

        // When
        try (FilteredWriter filteredWriter = new FilteredWriter(target, filtered)) {
            filteredWriter.write(source.toCharArray(), 0, source.length());
        }

        // Then
        Assert.assertEquals("*** quick brown fox jumped over the lazy dog", target.toString());
    }

    @Test
    public void shouldFilterEnd() throws IOException {

        // Given
        String filtered = "dog";

        // When
        try (FilteredWriter filteredWriter = new FilteredWriter(target, filtered)) {
            filteredWriter.write(source.toCharArray(), 0, source.length());
        }

        // Then
        Assert.assertEquals("The quick brown fox jumped over the lazy ***", target.toString());
    }

    @Test
    public void shouldFilterMiddle() throws IOException {

        // Given
        String filtered = "jumped";

        // When
        try (FilteredWriter filteredWriter = new FilteredWriter(target, filtered)) {
            filteredWriter.write(source.toCharArray(), 0, source.length());
        }

        // Then
        Assert.assertEquals("The quick brown fox ****** over the lazy dog", target.toString());
    }

    @Test
    public void shouldFilterSplit() throws IOException {

        // Given
        String filtered = "jumped";

        // When
        try (FilteredWriter filteredWriter = new FilteredWriter(target, filtered)) {
            // Write the content in two chunks, splitting the filtered value
            filteredWriter.write(source.toCharArray(), 0, 23);
            filteredWriter.write(source.toCharArray(), 23, source.length() - 23);
        }

        // Then
        Assert.assertEquals("The quick brown fox ****** over the lazy dog", target.toString());
    }

    @Test
    public void shouldFilterCharacterByCharacter() throws IOException {

        // Given
        String filtered = "brown";

        // When
        try (FilteredWriter filteredWriter = new FilteredWriter(target, filtered)) {
            for (char c : source.toCharArray()) {
                filteredWriter.write(c);
            }
        }

        // Then
        Assert.assertEquals("The quick ***** fox jumped over the lazy dog", target.toString());
    }


}