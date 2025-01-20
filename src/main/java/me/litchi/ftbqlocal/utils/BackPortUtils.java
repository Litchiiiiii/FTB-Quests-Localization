package me.litchi.ftbqlocal.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
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
import java.util.List;
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
    private static final String[] langList = {"en_us","zh_cn","zh_tw","zh_hk","de_de","es_es","fr_fr","ja_jp","ko_kr","ru_ru"};
    public static void backport(String langStr){
        try {
            enJson =JsonParser.parseString(FileUtils.readFileToString(new File(KUBEJS_LANG_DIR+"en_us.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            log.info("en_us.json is null");
        }
        defaultJSON = null;
        try {
            BaseQuestFile questFile = FTBQuestsAPI.api().getQuestFile(false);
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
            backportU.handleRewardTables(questFile.getRewardTables());
            questFile.forAllChapterGroups(backportU::handleChapterGroup);
            questFile.forAllChapters(chapter -> {
                backportU.handleChapter(chapter);
                backportU.handleQuests(chapter.getQuests());
            });
            File output = new File(Constants.PackMCMeta.GAMEDIR, Constants.PackMCMeta.QUESTFOLDER);
            questFile.writeDataFull(output.toPath());
            ServerQuestFile.INSTANCE.markDirty();
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
                rewardTable.setRawTitle(defaultJSON.get(rewardTable.getRawTitle().replaceAll("[{}]","")).getAsString());
            }catch (Exception e){
                try {
                    rewardTable.setRawTitle(enJson.get(rewardTable.getRawTitle().replaceAll("[{}]","")).getAsString());
                }catch (Exception e1){
                    log.info("RewardTables is null");
                }
                log.info("RewardTables is null");
            }
        });
    }

    @Override
    public void handleChapterGroup(ChapterGroup chapterGroup) {
        try {
            chapterGroup.setRawTitle(defaultJSON.get(chapterGroup.getRawTitle().replaceAll("[{}]","")).getAsString());
        }catch (Exception e){
            try {
                chapterGroup.setRawTitle(enJson.get(chapterGroup.getRawTitle().replaceAll("[{}]","")).getAsString());
            }catch (Exception e1){
                log.info("ChapterGroup is null");
            }
            log.info("ChapterGroup is null");
        }
    }

    @Override
    public void handleChapter(Chapter chapter) {
        try {
            chapter.setRawTitle(defaultJSON.get(chapter.getRawTitle().replaceAll("[{}]","")).getAsString());
            Field rawSubtitle = chapter.getClass().getDeclaredField("rawSubtitle");
            rawSubtitle.setAccessible(true);
            List<String> subtitle = new ArrayList<>(chapter.getRawSubtitle());
            List<String> subtitleList = new ArrayList<>();
            for (String s : subtitle) {
                if (s.contains("{")){
                    subtitleList.add(defaultJSON.get(s.replaceAll("[{}]", "")).getAsString());
                }
            }
            rawSubtitle.set(chapter,subtitleList);
        }catch (Exception e){
            try {
                chapter.setRawTitle(enJson.get(chapter.getRawTitle().replaceAll("[{}]","")).getAsString());
                Field rawSubtitle = chapter.getClass().getDeclaredField("rawSubtitle");
                rawSubtitle.setAccessible(true);
                List<String> subtitle = new ArrayList<>(chapter.getRawSubtitle());
                List<String> subtitleList = new ArrayList<>();
                for (String s : subtitle) {
                    if (s.contains("{")){
                        subtitleList.add(enJson.get(s.replaceAll("[{}]", "")).getAsString());
                    }
                }
                rawSubtitle.set(chapter,subtitleList);
            }catch (Exception e1){
                log.info(e1.getMessage());
            }
            log.info(e.getMessage());
        }
    }

    @Override
    public void handleQuests(List<Quest> allQuests) {
        allQuests.forEach(quest -> {
            try {
                try {
                    quest.setRawTitle(defaultJSON.get(quest.getRawTitle().replaceAll("[{}]","")).getAsString());
                }catch (Exception e){
                    try {
                        quest.setRawTitle(enJson.get(quest.getRawTitle().replaceAll("[{}]","")).getAsString());
                    }catch (Exception e1){
                        log.info("questTitle is null");
                    }
                    log.info("questTitle is null");
                }
                try {
                    quest.setRawSubtitle(defaultJSON.get(quest.getRawSubtitle().replaceAll("[{}]","")).getAsString());
                }catch (Exception e){
                    try{
                        quest.setRawSubtitle(enJson.get(quest.getRawSubtitle().replaceAll("[{}]","")).getAsString());
                    }catch (Exception e1){
                        log.info("questSubtitle is null");
                    }
                    log.info("questSubtitle is null");
                }
                quest.getRewards()
                        .stream()
                        .filter(reward -> !reward.getRawTitle().isEmpty())
                        .forEach(reward -> reward.setRawTitle(defaultJSON.get(reward.getRawTitle().replaceAll("[{}]","")).getAsString()));

                quest.getTasksAsList()
                        .stream()
                        .filter(task -> !task.getRawTitle().isEmpty())
                        .forEach(task -> task.setRawTitle(defaultJSON.get(task.getRawTitle().replaceAll("[{}]","")).getAsString()));
                List<String> rawDescription = quest.getRawDescription();
                handleQuestDescriptions(rawDescription);
                quest.getRawDescription().clear();
                quest.getRawDescription().addAll(descList);
                descList.clear();
            }catch (Exception e){
                try {
                    quest.getRewards()
                            .stream()
                            .filter(reward -> !reward.getRawTitle().isEmpty())
                            .forEach(reward -> reward.setRawTitle(enJson.get(reward.getRawTitle().replaceAll("[{}]","")).getAsString()));
                    quest.getTasksAsList()
                            .stream()
                            .filter(task -> !task.getRawTitle().isEmpty())
                            .forEach(task -> task.setRawTitle(enJson.get(task.getRawTitle().replaceAll("[{}]","")).getAsString()));
                    List<String> rawDescription = quest.getRawDescription();
                    handleQuestDescriptions(rawDescription);
                    quest.getRawDescription().clear();
                    quest.getRawDescription().addAll(descList);
                    descList.clear();
                }catch (Exception e1){
                    log.info("quests is null");
                }
                log.info("quests is null");
            }
        });
    }
    private void handleQuestDescriptions(List<String> descriptions) {
        String rich_desc_regex = "\\s*[\\[{].*\"+.*[]}]\\s*";
        Pattern rich_desc_pattern = Pattern.compile(rich_desc_regex);
        descriptions.forEach(desc -> {
            try {
                if (desc.isBlank()) {
                    descList.add("");
                }
                else if(desc.contains("{@pagebreak}")){
                    descList.add(desc);
                }
                else if(rich_desc_pattern.matcher(desc).find()){
                    Pattern pattern = Pattern.compile("ftbquests\\.chapter\\.[a-zA-Z0-9_]+\\.quest\\d+\\.[a-zA-Z_]+description\\d");
                    Matcher matcher = pattern.matcher(desc);
                    while (matcher.find()){
                        desc = desc.replace(matcher.group(0),defaultJSON.get(matcher.group(0)).getAsString()).replace("translate","text");
                    }
                    descList.add(desc);
                } else if (desc.contains("ftbquests")){
                    String key = desc.replaceAll("[{}]","");
                    descList.add(defaultJSON.get(key).getAsString());
                } else {
                    descList.add(desc);
                }
            }catch (Exception e){
                try {
                    if (desc.isBlank()) {
                        descList.add("");
                    }
                    else if(desc.contains("{@pagebreak}")){
                        descList.add(desc);
                    }
                    else if(rich_desc_pattern.matcher(desc).find()){
                        Pattern pattern = Pattern.compile("ftbquests\\.chapter\\.[a-zA-Z0-9_]+\\.quest\\d+\\.[a-zA-Z_]+description\\d");
                        Matcher matcher = pattern.matcher(desc);
                        while (matcher.find()){
                            desc = desc.replace(matcher.group(0),enJson.get(matcher.group(0)).getAsString()).replace("translate","text");
                        }
                        descList.add(desc);
                    } else if (desc.contains("ftbquests")){
                        String key = desc.replaceAll("[{}]","");
                        descList.add(enJson.get(key).getAsString());
                    } else {
                        descList.add(desc);
                    }
                }catch (Exception e1){
                    log.info("rich_desc is null");
                }
                log.info("rich_desc is null");
            }
        });
    }
}
