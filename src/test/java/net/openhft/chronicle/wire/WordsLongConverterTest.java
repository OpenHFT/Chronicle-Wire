package net.openhft.chronicle.wire;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class WordsLongConverterTest {
    @Test
    public void parse() {
        LongConverter bic = new WordsLongConverter();
        for (String s : ("square.evidence.object.plate.wash.shot\n" +
                "instant.content.occur.gold.reply.code\n" +
                "pay.service.salad.sunday.escape.bring\n" +
                "attach.video.cash.instant.young.brick\n" +
                "creation.smoke.project.grand.when.beard\n" +
                "tomato.survive.career.computer.suitable.black\n" +
                "bomb.identify.castle.credit.royal.each\n" +
                "bunch.outline.battery.fever.exhibit.her\n" +
                "pose.critical.suffix.silk.rather.hers\n" +
                "farm.romantic.officer.tree.member.bound\n" +
                "bandage.exchange.acid.ban.violence.boil\n" +
                "aim.neck.deal.award.impress.award\n" +
                "govern.dad.research.biology.quit.pack\n" +
                "north.powerful.delivery.cash.fig.curl\n" +
                "chart.cover.girl.clock.stupid.sour\n" +
                "guide.destroy.seventeen.somebody.nephew.east\n" +
                "hurry.joy.somebody.edition.four.suit\n" +
                "guard.could.gap.judgment.fellow.vote\n" +
                "judgment.access.sour.bubble.response.folk\n" +
                "latter.involve.cure.powerful.gap.term\n" +
                "publish.unload.dare.hobby.precise.melt\n" +
                "promote.except.crucial.cease.inside.make\n" +
                "damp.holiday.beak.score.percent.nail\n" +
                "dismiss.laugh.harmless.squeeze.bother.unit").split("\n")) {
            assertEquals(s, bic.asString(bic.parse(s)));
        }
        for (long l : new long[]{Long.MIN_VALUE, Integer.MIN_VALUE,
                System.currentTimeMillis(), System.nanoTime(), System.currentTimeMillis() * 1000,
                -1, 0, 1,
                Integer.MAX_VALUE, Long.MAX_VALUE}) {
            String text = bic.asString(l);
//            System.out.println(text);
            assertEquals(l, bic.parse(text));
        }
        Random random = new Random(1);
        for (int i = 0; i < 1000; i++) {
            long l = random.nextLong();
            String text = bic.asString(l);
//            System.out.println(text);
            assertEquals(l, bic.parse(text));

        }
    }
}