package com.glaurung.batMap.gui.corpses;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class CorpseModelTest {

    @Test
    public void defaultDelim() {
        CorpseModel model = new CorpseModel();
        assertEquals(";", model.getDelim());
    }

    @Test
    public void defaultMountHandle() {
        CorpseModel model = new CorpseModel();
        assertEquals("snowman", model.getMountHandle());
    }

    @Test
    public void defaultOrgans() {
        CorpseModel model = new CorpseModel();
        assertEquals("antenna", model.getOrgan1());
        assertEquals("antenna", model.getOrgan2());
    }

    @Test
    public void defaultEtherType() {
        CorpseModel model = new CorpseModel();
        assertEquals("no_focus", model.getEtherType());
    }

    @Test
    public void defaultFlagsAreFalse() {
        CorpseModel model = new CorpseModel();
        assertFalse(model.lichdrain);
        assertFalse(model.kharimsoul);
        assertFalse(model.kharimSoulCorpse);
        assertFalse(model.tsaraksoul);
        assertFalse(model.ripSoulToKatana);
        assertFalse(model.arkemile);
        assertFalse(model.gac);
        assertFalse(model.ga);
        assertFalse(model.donate);
        assertFalse(model.lootCorpse);
        assertFalse(model.lootGround);
        assertFalse(model.eatCorpse);
        assertFalse(model.barbarianBurn);
        assertFalse(model.feedCorpseTo);
        assertFalse(model.beheading);
        assertFalse(model.desecrateGround);
        assertFalse(model.burialCere);
        assertFalse(model.wakeCorpse);
        assertFalse(model.dig);
        assertFalse(model.aelenaOrgan);
        assertFalse(model.aelenaFam);
        assertFalse(model.dissect);
        assertFalse(model.tin);
        assertFalse(model.extractEther);
        assertFalse(model.wakeFollow);
        assertFalse(model.wakeAgro);
        assertFalse(model.wakeTalk);
        assertFalse(model.wakeStatic);
        assertFalse(model.lichWake);
        assertFalse(model.vampireWake);
        assertFalse(model.skeletonWake);
        assertFalse(model.zombieWake);
    }

    @Test
    public void defaultLootListEmpty() {
        CorpseModel model = new CorpseModel();
        assertNotNull(model.getLootList());
        assertTrue(model.getLootList().isEmpty());
    }

    @Test
    public void clear_resetsFlags() {
        CorpseModel model = new CorpseModel();
        model.lichdrain = true;
        model.gac = true;
        model.eatCorpse = true;
        model.wakeFollow = true;

        model.clear();

        assertFalse(model.lichdrain);
        assertFalse(model.gac);
        assertFalse(model.eatCorpse);
        assertFalse(model.wakeFollow);
    }

    @Test
    public void clear_resetsDelimToEmpty() {
        CorpseModel model = new CorpseModel();
        assertEquals(";", model.getDelim());
        model.clear();
        assertEquals("", model.getDelim());
    }

    @Test
    public void clear_resetsMountHandleToEmpty() {
        CorpseModel model = new CorpseModel();
        model.setMountHandle("horse");
        model.clear();
        assertEquals("", model.getMountHandle());
    }

    @Test
    public void clear_resetsOrgansToAntenna() {
        CorpseModel model = new CorpseModel();
        model.setOrgan1("brain");
        model.setOrgan2("heart");
        model.clear();
        assertEquals("antenna", model.getOrgan1());
        assertEquals("antenna", model.getOrgan2());
    }

    @Test
    public void clear_resetsEtherType() {
        CorpseModel model = new CorpseModel();
        model.setEtherType("blue");
        model.clear();
        assertEquals("no_focus", model.getEtherType());
    }

    @Test
    public void clear_resetsLootList() {
        CorpseModel model = new CorpseModel();
        model.setLootList(new LinkedList<>(Arrays.asList("gold", "gem")));
        model.clear();
        assertTrue(model.getLootList().isEmpty());
    }

    @Test
    public void setDelim_roundTrip() {
        CorpseModel model = new CorpseModel();
        model.setDelim("|");
        assertEquals("|", model.getDelim());
    }

    @Test
    public void setMountHandle_roundTrip() {
        CorpseModel model = new CorpseModel();
        model.setMountHandle("warhorse");
        assertEquals("warhorse", model.getMountHandle());
    }

    @Test
    public void setLootList_roundTrip() {
        CorpseModel model = new CorpseModel();
        List<String> list = new LinkedList<>(Arrays.asList("gold", "gem", "scroll"));
        model.setLootList(list);
        assertEquals(3, model.getLootList().size());
        assertEquals("gold", model.getLootList().get(0));
    }

    @Test
    public void setOrgans_roundTrip() {
        CorpseModel model = new CorpseModel();
        model.setOrgan1("brain");
        model.setOrgan2("heart");
        assertEquals("brain", model.getOrgan1());
        assertEquals("heart", model.getOrgan2());
    }

    @Test
    public void setEtherType_roundTrip() {
        CorpseModel model = new CorpseModel();
        model.setEtherType("red");
        assertEquals("red", model.getEtherType());
    }
}
