package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerateMethodReaderTest {

    @Test
    void validateGeneratedMethodReaderName() {
        GenerateMethodReader generateMethodReader = new GenerateMethodReader(WireType.BINARY, null, null, new ServiceA(), new ServiceB(), new ServiceC(), new ServiceD());
        String className = generateMethodReader.generatedClassName();
        assertEquals("GenerateMethodReaderTestGenerateMethodReaderTest$ServiceAGenerateMethodReaderTestGenerateMethodReaderTest$ServiceBGenerateMethodReaderTestGenerateMethodReaderTest$ServiceCGenerateMethodReaderTestGenerateMethodReaderTest$ServiceDBINARYMethodReader", className);
    }

    private static class ServiceA {

    }

    private static class ServiceB {

    }

    private static class ServiceC {

    }

    private static class ServiceD {

    }

}