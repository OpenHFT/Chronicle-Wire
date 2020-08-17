package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.*;

public class GenerateJsonSchemaMainTest {

    @Test
    public void generateSchemaFor() throws ClassNotFoundException {
        GenerateJsonSchemaMain.main(ITop.class.getName());
    }
}