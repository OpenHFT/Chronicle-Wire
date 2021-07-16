package net.openhft.chronicle.wire;

import org.junit.Test;

public class GenerateJsonSchemaMainTest extends WireTestCommon {

    @Test
    public void generateSchemaFor() throws ClassNotFoundException {
        GenerateJsonSchemaMain.main(ITop.class.getName());
    }
}