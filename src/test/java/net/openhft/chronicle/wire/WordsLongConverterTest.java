package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.converter.Words;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class WordsLongConverterTest extends WireTestCommon {

    @Test
    public void inYaml() {
        Wire wire = Wire.newYamlWireOnHeap();
        final AsWords words = wire.methodWriter(AsWords.class);
        words.words(Words.INSTANCE.parse("eat.red.apple"));
        words.words(Words.INSTANCE.parse("blue.clay,sets"));
        words.words(Words.INSTANCE.parse("many.looks.now"));
        words.words(Words.INSTANCE.parse("square.army.plan.player.wash.disk"));
        assertEquals("" +
                "words: eat.red.apple\n" +
                "...\n" +
                "words: blue.clay.sets\n" +
                "...\n" +
                "words: many.looks.now\n" +
                "...\n" +
                "words: square.army.plan.player.wash.disk\n" +
                "...\n", wire.toString());
    }

    @Test
    public void asString() {
        LongConverter bic = new WordsLongConverter();
        for (long i = 1; i <= 1e12; i *= 10)
            System.out.println(i + ": " + bic.asString(i));
    }

    @Test
    public void parse() {
        LongConverter bic = new WordsLongConverter();
        for (String s : ("square.army.plan.player.wash.disk\n" +
                "instant.our.ocean.gold.thank.bang\n" +
                "attach.company.castle.instant.young.brick\n" +
                "we.smooth.smart.grants.when.beard\n" +
                "bunch.prior.sky.bond.assist.apply\n" +
                "island.cheek.across.ban.laws.bomb\n" +
                "aim.neck.deal.award.impress.ink\n" +
                "cruel.damp.they.toy.quite.drill\n" +
                "chart.two.glad.clock.any.lean\n" +
                "guard.could.gap.glue.boil.apply\n" +
                "glue.account.sour.bubble.thought.spa\n" +
                "songs.unlock.dare.enough.shed.child\n" +
                "smile.excerpt.alpha.centre.grill.alpha\n" +
                "it.launch.duck.stable.brother.calm").split("\n")) {
            assertEquals(s, bic.asString(bic.parse(s)));
        }
        for (long l : new long[]{Long.MIN_VALUE, Integer.MIN_VALUE,
                System.currentTimeMillis(), System.nanoTime(), System.currentTimeMillis() * 1000,
                -1, 0, 1,
                Integer.MAX_VALUE, Long.MAX_VALUE}) {
            String text = bic.asString(l);
            // System.out.println(text);
            assertEquals(l, bic.parse(text));
        }
        Random random = new Random(1);
        for (int i = 0; i < 1000; i++) {
            long l = random.nextLong();
            String text = bic.asString(l);
            if (text.equals("eagle.eagle.bedroom.again.dealer.bids"))
                System.out.println(text);
            long parse = bic.parse(text);
            assertEquals(l, parse);

        }
    }

    interface AsWords {
        void words(@Words long words);
    }
}