package me.litchi.ftbqlocal.handler.impl;

import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterGroup;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.util.TextUtils;
import me.litchi.ftbqlocal.handler.FtbQHandler;
import me.litchi.ftbqlocal.service.impl.JSONService;
import me.litchi.ftbqlocal.utils.HandlerCounter;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static me.litchi.ftbqlocal.utils.HandlerCounter.*;

public class Handler implements FtbQHandler {
    private final TreeMap<String, String> transKeys = HandlerCounter.transKeys;
    private final JSONService handleJSON = new JSONService();
    @Override
    public void handleRewardTables(List<RewardTable> rewardTables) {
        rewardTables.forEach(rewardTable -> {
            HandlerCounter.addCounter();
            transKeys.put("ftbquests.loot_table_"+rewardTable.id+".title", addPercent(rewardTable.title));

            rewardTable.title = ("{" + "ftbquests.loot_table_"+rewardTable.id+".title"+ "}");
        });
        HandlerCounter.setCounter(0);
    }

    @Override
    public void handleChapterGroup(ChapterGroup chapterGroup) {
        if(chapterGroup.getTitle() != null){
            if (!chapterGroup.title.isEmpty()){
                transKeys.put("ftbquests.chapter_groups_"+chapterGroup.id+".title", addPercent(chapterGroup.title));
                chapterGroup.title = "{" + "ftbquests.chapter_groups_"+chapterGroup.id+".title" + "}";
                HandlerCounter.addCounter();
            }
        }
    }

    @Override
    public void handleChapter(Chapter chapter) {
        HandlerCounter.setPrefix("ftbquests.chapter."+chapter.getFilename());
        String prefix = HandlerCounter.getPrefix();
        if(chapter.getTitle() != null){
            transKeys.put(prefix + ".title", addPercent(chapter.title));
            chapter.title = "{" + prefix + ".title" + "}";
        }
        if(!chapter.subtitle.isEmpty()){
            int num = 0;
            List<String> subtitle = new ArrayList<>(chapter.subtitle);
            for (String s : subtitle) {
                String key = prefix + ".subtitle"+num;
                transKeys.put(key, addPercent(s));
                chapter.subtitle.remove(s);
                chapter.subtitle.add("{" + key+ "}");
                num++;
            }
        }
        List<ChapterImage> images = chapter.images;
        List<String> newHoverList = new ArrayList<>();
        for (ChapterImage image : images) {
            List<String> hover = image.hover;
            for (String hoverText : hover) {
                HandlerCounter.addImageNum();
                String key = prefix+".image.hovertext"+HandlerCounter.getImageNum();
                transKeys.put(key,addPercent(hoverText));
                newHoverList.add(key);
            }
            hover.clear();
            hover.addAll(newHoverList);
            newHoverList.clear();
        }
        HandlerCounter.setImageNum(0);
    }

    private void handleTasks(List<Task> tasks) {
        tasks.stream().filter(task -> !task.title.isEmpty()).forEach(task -> {
            HandlerCounter.addCounter();
            String textKey = HandlerCounter.getPrefix() + ".task_"+task.id+".title";
            transKeys.put(textKey, addPercent(task.title));
            task.title = "{"+textKey+"}";
        });
        HandlerCounter.setCounter(0);
    }
    private void handleRewards(List<Reward> rewards) {
        rewards.stream().filter(reward -> !reward.title.isEmpty()).forEach(reward -> {
            HandlerCounter.addCounter();
            String textKey = HandlerCounter.getPrefix() + ".reward_"+reward.id+".title";
            transKeys.put(textKey, addPercent(reward.title));
            reward.title = "{"+textKey+"}";
        });
        HandlerCounter.setCounter(0);
    }

    @Override
    public void handleQuests(List<Quest> allQuests) {
        allQuests.forEach(quest ->{
            HandlerCounter.addQuests();
            HandlerCounter.setPrefix("ftbquests.chapter." + quest.getChapter().getFilename() + ".quest" + HandlerCounter.getQuests());
            String prefix = HandlerCounter.getPrefix();
            if(quest.getTitle() != null){
                if (!quest.title.isEmpty()){
                    transKeys.put(prefix + ".title", addPercent(quest.title));
                    quest.title = "{" + prefix + ".title" + "}";
                }
            }
            if(!quest.subtitle.isEmpty()){
                transKeys.put(prefix + ".subtitle", addPercent(quest.subtitle));
                quest.subtitle = "{" + prefix + ".subtitle" + "}";
            }
            handleTasks(quest.tasks);
            handleRewards(quest.rewards.stream().toList());
            handleQuestDescriptions(quest.description);

            quest.description.clear();
            quest.description.addAll(descList);
            descList.clear();
        });
        HandlerCounter.setQuests(0);
    }

    private void handleQuestDescriptions(List<String> descriptions) {
        String rich_desc_regex = "\\s*[\\[\\{].*\"+.*[\\]\\}]\\s*";
        Pattern rich_desc_pattern = Pattern.compile(rich_desc_regex);
        descriptions.forEach(desc -> {
            if (desc.isBlank()) {
                descList.add("");
            }
            else if (desc.contains("{image:")){
                handleDescriptionImage(desc);
            }
            else if(desc.contains("{@pagebreak}")){
                descList.add(desc);
            }
            else if(rich_desc_pattern.matcher(desc).find()){
                HandlerCounter.addDescription();
                Component parsedText = TextUtils.parseRawText(desc);
                descList.add(handleJSON.handleJSON(parsedText));
            } else {
                HandlerCounter.addDescription();
                String textKey = HandlerCounter.getPrefix() + ".description" + HandlerCounter.getDescription();
                transKeys.put(textKey, addPercent(desc));
                descList.add("{"+textKey+"}");
            }
        });
        HandlerCounter.setDescription(0);
        HandlerCounter.setImage(0);
    }
    private void handleDescriptionImage(String desc){
        String imgKey = HandlerCounter.getPrefix() + ".image" + HandlerCounter.getImage();
        transKeys.put(imgKey, desc);
        descList.add("{" + imgKey + "}");
        HandlerCounter.addImage();
    }
}
