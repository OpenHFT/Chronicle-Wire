package net.openhft.chronicle.wire;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class WordsLongConverterTest {
    @Test
    public void london() {
        double precision = 0.7e-3;
        System.out.println(WordsLongConverter.latLong(51.5074, -0.1278, precision));
        System.out.println(WordsLongConverter.latLong(48.8566, 2.3522, precision));
        System.out.println(WordsLongConverter.latLong(40.7128, -74.0060, precision));
        System.out.println(WordsLongConverter.latLong(-37.8136, 144.9631, precision));
    }

    @Test
    public void parse() {
        LongConverter bic = new WordsLongConverter();
        for (String s : ("square.army.observe.plate.wash.achieve\n" +
                "instant.content.occur.gold.thank.coins\n" +
                "pay.who.unable.appropriate.essay.bring\n" +
                "attach.campaign.castle.instant.young.brick\n" +
                "creation.smoke.smart.grants.when.beard\n" +
                "bunch.outline.coral.bomb.assist.apply\n" +
                "bandage.exchange.acid.ban.capable.bold\n" +
                "aim.neck.deal.award.impress.award\n" +
                "cruel.damp.they.countries.quite.drill\n" +
                "chart.cover.girl.clock.anytime.allow\n" +
                "guard.could.gap.glue.boil.choice\n" +
                "glue.account.sour.bubble.thought.ours\n" +
                "songs.unlock.dare.enough.shed.child\n" +
                "smile.excerpt.crucial.cease.inside.chance\n" +
                "it.launch.duck.stable.bother.calm").split("\n")) {
            assertEquals(s, bic.asString(bic.parse(s)));
        }
        for (long l : new long[]{Long.MIN_VALUE, Integer.MIN_VALUE,
                System.currentTimeMillis(), System.nanoTime(), System.currentTimeMillis() * 1000,
                -1, 0, 1,
                Integer.MAX_VALUE, Long.MAX_VALUE}) {
            String text = bic.asString(l);
            System.out.println(text);
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
}