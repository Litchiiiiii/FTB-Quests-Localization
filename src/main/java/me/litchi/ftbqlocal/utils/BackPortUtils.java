package me.litchi.ftbqlocal.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.*;
import java.util.regex.Matcher;
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
                questFile.getAllChapters().forEach(chapter -> {
                    chapterSubMap.put(chapter.id,new ArrayList<>(chapter.subtitle));
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
                    log.info("defaultJson is null");
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
                rewardTable.title=(defaultJSON.get(rewardTable.title.replaceAll("[{}]","")).getAsString());
            }catch (Exception e){
                try {
                    rewardTable.title=(enJson.get(rewardTable.title.replaceAll("[{}]","")).getAsString());
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
                chapterGroup.title=(defaultJSON.get(chapterGroup.title.replaceAll("[{}]","")).getAsString());
            }catch (Exception e){
                try {
                    chapterGroup.title=(enJson.get(chapterGroup.title.replaceAll("[{}]","")).getAsString());
                }catch (Exception e1){
                    log.info("ChapterGroup title is not in kubejs!");
                }
            }
        }
    }

    @Override
    public void handleChapter(Chapter chapter) {
        try {
            if (chapter.title.contains("{")){
                try {
                    chapter.title=(defaultJSON.get(chapter.title.replaceAll("[{}]","")).getAsString());
                }catch (Exception e){
                    try {
                        chapter.title=(enJson.get(chapter.title.replaceAll("[{}]","")).getAsString());
                    }catch (Exception e1){
                        log.info("chapter title is not in kubejs!");
                    }
                }
            }
            Field rawSubtitle = chapter.getClass().getDeclaredField("rawSubtitle");
            rawSubtitle.setAccessible(true);
            //List<String> subtitle = new ArrayList<>(chapter.subtitle);
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
        }catch (Exception e){
            log.info(e.getMessage());
        }
    }

    @Override
    public void handleQuests(List<Quest> allQuests) {
        allQuests.forEach(quest -> {
            if (quest.title.contains("{")){
                try {
                    quest.title=(defaultJSON.get(quest.title.replaceAll("[{}]","")).getAsString());
                }catch (Exception e){
                    try {
                        quest.title=(enJson.get(quest.title.replaceAll("[{}]","")).getAsString());
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
                        .forEach(reward -> reward.title=(defaultJSON.get(reward.title.replaceAll("[{}]","")).getAsString()));
            }catch (Exception e){
                try {
                    quest.rewards
                            .stream()
                            .filter(reward -> reward.title.contains("{"))
                            .forEach(reward -> reward.title=(enJson.get(reward.title.replaceAll("[{}]","")).getAsString()));
                }catch (Exception e1){
                    log.info("questReward title is not in kubejs!");
                }
            }
            try {
                quest.tasks
                        .stream()
                        .filter(task -> task.title.contains("{"))
                        .forEach(task -> task.title=(defaultJSON.get(task.title.replaceAll("[{}]","")).getAsString()));
            }catch (Exception e){
                try {
                    quest.tasks
                            .stream()
                            .filter(task -> task.title.contains("{"))
                            .forEach(task -> task.title=(enJson.get(task.title.replaceAll("[{}]","")).getAsString()));
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
    private void handleQuestDescriptions(long id) {
        String rich_desc_regex = "\\s*[\\[{].*\"+.*[]}]\\s*";
        Pattern rich_desc_pattern = Pattern.compile(rich_desc_regex);
        List<String> newDescList = new ArrayList<>(newdescMap.get(id));
        for (String s : newDescList) {
            if (s.isBlank()) {
                descList.add("");
            } else if(s.contains("{@pagebreak}")){
                descList.add(s);
            }
            if(rich_desc_pattern.matcher(s).find()){
                Pattern pattern = Pattern.compile("ftbquests\\.chapter\\.[a-zA-Z0-9_]+\\.quest\\d+\\.[a-zA-Z_]+description\\d");
                Matcher matcher = pattern.matcher(s);
                while (matcher.find()){
                    try {
                        s = s.replace(matcher.group(0),defaultJSON.get(matcher.group(0)).getAsString()).replace("translate","text");
                    }catch (Exception e){
                        try {
                            s = s.replace(matcher.group(0),enJson.get(matcher.group(0)).getAsString()).replace("translate","text");
                        }catch (Exception e1){
                            log.info(e1.getMessage());
                        }
                    }
                }
                descList.add(s);
            } else if (s.contains("ftbquests")){
                if (s.contains("{")){
                    String key = s.replaceAll("[{}]","");
                    try {
                        descList.add(defaultJSON.get(key).getAsString());
                    }catch (Exception e){
                        try {
                            descList.add(enJson.get(key).getAsString());
                        }catch (Exception e1){
                            log.info(e1.getMessage());
                        }
                    }
                }
            } else if (!s.isBlank() && !s.contains("{@pagebreak}")){
                descList.add(s);
            }
        }
    }
}
