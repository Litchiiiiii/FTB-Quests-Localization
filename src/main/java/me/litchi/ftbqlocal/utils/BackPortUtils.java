package me.litchi.ftbqlocal.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import me.litchi.ftbqlocal.handler.FtbQHandler;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import static me.litchi.ftbqlocal.commands.FTBQLangConvert.originalLangCode;

public class BackPortUtils implements FtbQHandler {
    private static final String KUBEJS_LANG_DIR = Constants.PackMCMeta.GAMEDIR+"\\FTBLang\\backup\\"+Constants.PackMCMeta.KUBEJSFOLDER+"\\";
    private static final String RESOURCE_LANG_DIR = Constants.PackMCMeta.GAMEDIR+"\\resourcepacks\\"+Constants.PackMCMeta.PACKNAME;
    private static final Logger log = LoggerFactory.getLogger(BackPortUtils.class);
    private static JsonObject defaultJSON = null;
    private static JsonObject enJson = null;
    private static final List<String> descList = new ArrayList<>();
    private static final BackPortUtils backportU = new BackPortUtils();
    private static final Map<Long,List<String>> newdescMap = new HashMap<>();
    private static final Map<Long,List<String>> chapterSubMap = new HashMap<>();
    private static final Map<Long,List<ChapterImage>> chapterImageList = new HashMap<>();
    private static final Map<String,List<String>> hoverTextMap = new HashMap<>();
    public static void backport(String langStr){
        try {
            enJson =JsonParser.parseString(FileUtils.readFileToString(new File(KUBEJS_LANG_DIR+"en_us.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            log.info("en_us.json is null");
        }
        defaultJSON = null;
        try {
            QuestFile questFile = FTBQuests.PROXY.getQuestFile(false);
            if (langStr.equals("en_us")){
                newdescMap.clear();
                chapterSubMap.clear();
                chapterImageList.clear();
                questFile.getAllChapters().forEach(chapter -> {
                    chapterSubMap.put(chapter.id,new ArrayList<>(chapter.subtitle));
                    chapterImageList.put(chapter.id,new ArrayList<>(chapter.images));
                    long num = 0L;
                    for (ChapterImage image : chapter.images) {
                        List<String> textList = image.hover;
                        hoverTextMap.put(String.valueOf(chapter.id)+ num, textList);
                        num++;
                    }
                    chapter.getQuests().forEach(quest -> {
                        newdescMap.put(quest.id,new ArrayList<>(quest.description));
                    });
                });
            }
            File kubefile = new File(KUBEJS_LANG_DIR + langStr +".json");
            String defaultLang;
            if (!kubefile.exists()){
                try (ZipFile zipFile = new ZipFile(RESOURCE_LANG_DIR)){
                    zipFile.stream().forEach(zipEntry -> {
                        if (!zipEntry.isDirectory()){
                            try {
                                if (zipEntry.getName().equals(langStr+".json")){
                                    byte[] bytes = zipFile.getInputStream(zipEntry).readAllBytes();
                                    defaultJSON =JsonParser.parseString(new String(bytes)).getAsJsonObject();
                                }
                            } catch (IOException e) {
                                log.info("JsonFile error");
                            }
                        }
                    });
                }catch (Exception e){
                    log.info("ZIPFile error");
                }
            } else {
                defaultLang = FileUtils.readFileToString(kubefile, StandardCharsets.UTF_8);
                defaultJSON =JsonParser.parseString(defaultLang).getAsJsonObject();
            }
            if (defaultJSON == null){
                if (!langStr.equals("en_us")){
                    //defaultLang = FileUtils.readFileToString(new File(KUBEJS_LANG_DIR+"en_us.json"), StandardCharsets.UTF_8);
                    defaultJSON = enJson;
                }else {
                    throw new IOException();
                }
            }
            backportU.handleRewardTables(questFile.rewardTables);
            questFile.chapterGroups.forEach(backportU::handleChapterGroup);
            questFile.getAllChapters().forEach(chapter -> {
                backportU.handleChapter(chapter);
                backportU.handleQuests(chapter.getQuests());
            });
            File output = new File(Constants.PackMCMeta.GAMEDIR, Constants.PackMCMeta.QUESTFOLDER);
            questFile.writeDataFull(output.toPath());
            ServerQuestFile.INSTANCE.save();
            ServerQuestFile.INSTANCE.saveNow();
            if (langStr.equals(originalLangCode)){
                File questsFolder = new File(Constants.PackMCMeta.GAMEDIR, Constants.PackMCMeta.QUESTFOLDER);
                File parent = new File(Constants.PackMCMeta.GAMEDIR, Constants.PackMCMeta.OUTPUTFOLDER);
                if(questsFolder.exists()){
                    File backup = new File(parent, Constants.PackMCMeta.BACKUPFOLDER);
                    FileUtils.copyDirectory(questsFolder, backup);
                }
            }
        } catch (IOException e) {
            log.info("This is first port!");
        }
    }

    @Override
    public void handleRewardTables(List<RewardTable> rewardTables) {
        rewardTables.forEach(rewardTable -> {
            try {
                rewardTable.title = defaultJSON.get(rewardTable.title.replaceAll("[{}]","")).getAsString();
            }catch (Exception e){
                try {
                    rewardTable.title = enJson.get(rewardTable.title.replaceAll("[{}]","")).getAsString();
                }catch (Exception e1){
                    log.info("rewardTable title is not in kubejs!");
                }
            }
        });
    }

    @Override
    public void handleChapterGroup(ChapterGroup chapterGroup) {
        if (chapterGroup.title.contains("{")){
            try {
                chapterGroup.title = defaultJSON.get(chapterGroup.title.replaceAll("[{}]","")).getAsString();
            }catch (Exception e){
                try {
                    chapterGroup.title = enJson.get(chapterGroup.title.replaceAll("[{}]","")).getAsString();
                }catch (Exception e1){
                    log.info("ChapterGroup is null");
                }
            }
        }

    }

    @Override
    public void handleChapter(Chapter chapter) {
        try {
            if (chapter.title.contains("{")){
                try {
                    chapter.title = (defaultJSON.get(chapter.title.replaceAll("[{}]","")).getAsString());
                }catch (Exception e){
                    try {
                        chapter.title = (enJson.get(chapter.title.replaceAll("[{}]","")).getAsString());
                    }catch (Exception e1){
                        log.info("chapter title is not in kubejs!");
                    }
                }
            }
            Field rawSubtitle = chapter.getClass().getDeclaredField("subtitle");
            rawSubtitle.setAccessible(true);

            //List<String> subtitle = new ArrayList<>(chapter.getRawSubtitle());
            List<String> subtitle = new ArrayList<>(chapterSubMap.get(chapter.id));
            List<String> subtitleList = new ArrayList<>();
            for (String s : subtitle) {
                if (s.contains("{")){
                    String key = s.replaceAll("[{}]", "");
                    try {
                        subtitleList.add(defaultJSON.get(key).getAsString());
                    }catch (Exception e){
                        try {
                            subtitleList.add(enJson.get(key).getAsString());
                        }catch (Exception e1){
                            log.info("chaptSubtitle is not in kubejs!");
                        }
                    }
                }else {
                    subtitleList.add(s);
                }
            }
            if (!subtitleList.isEmpty()){
                rawSubtitle.set(chapter,subtitleList);
            }
            List<ChapterImage> images = new ArrayList<>(chapterImageList.get(chapter.id));
            long num = 0L;
            for (ChapterImage image : images) {
                List<String> chapterImageHoverTextList = new ArrayList<>();
                List<String> hoverTextList = hoverTextMap.get(String.valueOf(chapter.id )+ num);
                num++;
                if (!hoverTextList.isEmpty()){
                    hoverTextList.forEach(hoverTextString ->{
                        if (hoverTextString.contains(".image.hovertext")){
                            //String key = hoverTextString.replaceAll("[{}]", "");
                            try {
                                chapterImageHoverTextList.add(defaultJSON.get(hoverTextString).getAsString());
                            }catch (Exception e){
                                try {
                                    chapterImageHoverTextList.add(enJson.get(hoverTextString).getAsString());
                                }catch (Exception e1){
                                    log.info("chapter ImageHoverText is not in kubejs!");
                                }
                            }
                        }else {
                            chapterImageHoverTextList.add(hoverTextString);
                        }
                    });
                    if (!chapterImageHoverTextList.isEmpty()){
                        image.hover = chapterImageHoverTextList;
                    }
                }
            }
            Field images1 = Chapter.class.getDeclaredField("images");
            images1.setAccessible(true);
            images1.set(chapter,images);
        }catch (Exception e){
            log.info(e.getMessage());
        }
    }

    @Override
    public void handleQuests(List<Quest> allQuests) {
        allQuests.forEach(quest -> {
            if (quest.title.contains("{")){
                try {
                    quest.title = (defaultJSON.get(quest.title.replaceAll("[{}]","")).getAsString());
                }catch (Exception e){
                    try {
                        quest.title = (enJson.get(quest.title.replaceAll("[{}]","")).getAsString());
                    }catch (Exception e1){
                        log.info("questTitle is not in kubejs!");
                    }
                }
            }
            if (quest.subtitle.contains("{")){
                try {
                    quest.subtitle = (defaultJSON.get(quest.subtitle.replaceAll("[{}]","")).getAsString());
                }catch (Exception e){
                    try{
                        quest.subtitle = (enJson.get(quest.subtitle.replaceAll("[{}]","")).getAsString());
                    }catch (Exception e1){
                        log.info("questSubtitle is not in kubejs!");
                    }
                }
            }
            try {
                quest.rewards
                        .stream()
                        .filter(reward -> reward.title.contains("{"))
                        .forEach(reward -> reward.title = defaultJSON.get(reward.title.replaceAll("[{}]","")).getAsString());
            }catch (Exception e){
                try {
                    quest.rewards
                            .stream()
                            .filter(reward -> reward.title.contains("{"))
                            .forEach(reward -> reward.title = (enJson.get(reward.title.replaceAll("[{}]","")).getAsString()));
                }catch (Exception e1){
                    log.info("questReward title is not in kubejs!");
                }
            }
            try {
                quest.tasks
                        .stream()
                        .filter(task -> task.title.contains("{"))
                        .forEach(task -> task.title = (defaultJSON.get(task.title.replaceAll("[{}]","")).getAsString()));
            }catch (Exception e){
                try {
                    quest.tasks
                            .stream()
                            .filter(task -> task.title.contains("{"))
                            .forEach(task -> task.title = (enJson.get(task.title.replaceAll("[{}]","")).getAsString()));
                }catch (Exception e1){
                    log.info("questReward title is not in kubejs!");
                }
            }
            //List<String> rawDescription = quest.getRawDescription();
            handleQuestDescriptions(quest.id);
            quest.description.clear();
            quest.description.addAll(descList);
            descList.clear();
        });
    }
    private void handleQuestDescriptions(Long id) {
        List<String> newDescList = new ArrayList<>(newdescMap.get(id));
        for (String desc : newDescList) {
            if (desc.isBlank()) {
                descList.add("");
            } else if (desc.contains("ftbquests")){
                String key = desc.replaceAll("[{}]","");
                try {
                    descList.add(defaultJSON.get(key).getAsString());
                }catch (Exception e){
                    try {
                        descList.add(enJson.get(key).getAsString());
                    }catch (Exception e1) {
                        log.info(e1.getMessage());
                    }
                }
            } else {
                descList.add(desc);
            }
        }
    }
}