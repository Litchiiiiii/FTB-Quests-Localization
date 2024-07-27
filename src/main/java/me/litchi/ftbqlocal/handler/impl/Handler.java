package me.litchi.ftbqlocal.handler.impl;

import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterGroup;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.util.TextUtils;
import me.litchi.ftbqlocal.handler.FtbQHandler;
import me.litchi.ftbqlocal.service.impl.JSONService;
import me.litchi.ftbqlocal.utils.HandlerCounter;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.TreeMap;

public class Handler implements FtbQHandler {
    private final TreeMap<String, String> transKeys = HandlerCounter.transKeys;
    private final JSONService handleJSON = new JSONService();
    @Override
    public void handleRewardTables(List<RewardTable> rewardTables) {
        rewardTables.forEach(rewardTable -> {
            HandlerCounter.addCounter();
            transKeys.put("ftbquests.loot_table.title"+HandlerCounter.getCounter(), rewardTable.title);
            rewardTable.title = "{" + "ftbquests.loot_table.title" + HandlerCounter.getCounter() + "}";
        });
        HandlerCounter.setCounter(0);
    }

    @Override
    public void handleChapterGroup(ChapterGroup chapterGroup) {
        if(chapterGroup.getTitle() != null){
            if (!chapterGroup.title.isEmpty()){
                transKeys.put("ftbquests.chapter_groups.title" + HandlerCounter.getCounter(), chapterGroup.title);
                chapterGroup.title = "{" + "ftbquests.chapter_groups.title" + HandlerCounter.getCounter() + "}";
                HandlerCounter.addCounter();
            }
        }
    }

    @Override
    public void handleChapter(Chapter chapter) {
        HandlerCounter.setPrefix("ftbquests.chapter."+chapter.getFilename());
        String prefix = HandlerCounter.getPrefix();
        if(chapter.getTitle() != null){
            transKeys.put(prefix + ".title", chapter.title);
            chapter.title = "{" + prefix + ".title" + "}";
        }
        if(!chapter.subtitle.isEmpty()){
            transKeys.put(prefix + ".subtitle", String.join("\n", chapter.subtitle));
            chapter.subtitle.clear();
            chapter.subtitle.add("{" + prefix + ".subtitle" + "}");
        }
    }

    private void handleTasks(List<Task> tasks) {
        tasks.stream().filter(task -> !task.title.isEmpty()).forEach(task -> {
            HandlerCounter.addCounter();
            String textKey = HandlerCounter.getPrefix() + ".task.title" + HandlerCounter.getCounter();
            transKeys.put(textKey, task.title);
            task.title = "{"+textKey+"}";
        });
        HandlerCounter.setCounter(0);
    }
    private void handleRewards(List<Reward> rewards) {
        rewards.stream().filter(reward -> !reward.title.isEmpty()).forEach(reward -> {
            HandlerCounter.addCounter();
            String textKey = HandlerCounter.getPrefix() + ".reward.title" + HandlerCounter.getCounter();
            transKeys.put(textKey, reward.title);
            HandlerCounter.descList.add("{"+textKey+"}");
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
                    transKeys.put(prefix + ".title", quest.title);
                    quest.title = "{" + prefix + ".title" + "}";
                }
            }
            if(!quest.subtitle.isEmpty()){
                transKeys.put(prefix + ".subtitle", quest.subtitle);
                quest.subtitle = "{" + prefix + ".subtitle" + "}";
            }
            handleTasks(quest.tasks);
            handleRewards(quest.rewards.stream().toList());
            handleQuestDescriptions(quest.description);

            quest.description.clear();
            quest.description.addAll(HandlerCounter.descList);
            HandlerCounter.descList.clear();
        });
        HandlerCounter.setQuests(0);
    }

    private void handleQuestDescriptions(List<String> descriptions) {
        descriptions.forEach(desc -> {
            HandlerCounter.addImage();

            if (desc.isBlank()) {
                HandlerCounter.descList.add("");
            }
            else if (desc.contains("{image:")){
                handleDescriptionImage(desc);
            }
            else if(desc.contains("{@pagebreak}")){
                HandlerCounter.descList.add(desc);
            }
            else if((desc.startsWith("[") && desc.endsWith("]")) || (desc.startsWith("{") && desc.endsWith("}"))){
                HandlerCounter.addDescription();
                Component parsedText = TextUtils.parseRawText(desc);
                handleJSON.handleJSON(parsedText);
            }
            else {
                HandlerCounter.addDescription();
                String textKey = HandlerCounter.getPrefix() + ".description" + HandlerCounter.getDescription();
                transKeys.put(textKey, desc);
                HandlerCounter.descList.add("{"+textKey+"}");
            }
        });
        HandlerCounter.setDescription(0);
        HandlerCounter.setImage(0);
    }
    private void handleDescriptionImage(String desc){
        String imgKey = HandlerCounter.getPrefix() + ".image" + HandlerCounter.getImage();
        transKeys.put(imgKey, desc);
        HandlerCounter.descList.add("{" + imgKey + "}");
        HandlerCounter.addImage();
    }
}
