package me.litchi.ftbqlocal.utils;

import me.litchi.ftbqlocal.FtbQuestLocalizerMod;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class HandlerCounter {
    public static final TreeMap<String, String> transKeys = new TreeMap<>();
    private static String prefix;
    private static int chapters = 0;
    private static int quests = 0;
    private static int counter = 0;
    private static int description = 0;
    private static int image = 0;
    private static int imageNum = 0;

    public static int getImageNum() {
        return imageNum;
    }
    public static void addImageNum() {
        imageNum++;
    }

    public static void setImageNum(int imageNum) {
        HandlerCounter.imageNum = imageNum;
    }

    public static List<String> descList = new ArrayList<>();

    public static Logger log = FtbQuestLocalizerMod.log;

    private HandlerCounter() {
    }

    public static String getPrefix() {
        return prefix;
    }

    public static void setPrefix(String prefix) {
        HandlerCounter.prefix = prefix;
    }

    public static int getChapters() {
        return chapters;
    }

    public static void setChapters(int chapters) {
        HandlerCounter.chapters = chapters;
    }

    public static void addChapters(){
        HandlerCounter.chapters++;
    }
    public static int getQuests() {
        return quests;
    }

    public static void setQuests(int quests) {
        HandlerCounter.quests = quests;
    }
    public static void addQuests(){
        HandlerCounter.quests++;
    }

    public static int getCounter() {
        return counter;
    }

    public static void setCounter(int counter) {
        HandlerCounter.counter = counter;
    }
    public static void addCounter() {
        HandlerCounter.counter++;
    }

    public static int getDescription() {
        return description;
    }

    public static void setDescription(int description) {
        HandlerCounter.description = description;
    }
    public static void addDescription() {
        HandlerCounter.description++;
    }
    public static int getImage() {
        return image++;
    }

    public static void setImage(int image) {
        HandlerCounter.image = image;
    }
    public static void addImage() {
        HandlerCounter.image++;
    }
}
