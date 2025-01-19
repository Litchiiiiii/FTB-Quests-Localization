package me.litchi.ftbqlocal.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.command.FTBQuestsCommands;
import dev.ftb.mods.ftbquests.quest.*;
import me.litchi.ftbqlocal.FtbQuestLocalizerMod;
import me.litchi.ftbqlocal.handler.impl.Handler;
import me.litchi.ftbqlocal.utils.BackPortUtils;
import me.litchi.ftbqlocal.utils.Constants;
import me.litchi.ftbqlocal.utils.HandlerCounter;
import me.litchi.ftbqlocal.utils.PackUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class FTBQLangConvert {
    public static String originalLangCode = "en_us";
    private static final String[] langList = {"en_us","zh_cn","zh_tw","zh_hk","de_de","es_es","fr_fr","ja_jp","ko_kr","ru_ru"};

    public FTBQLangConvert(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register((Commands.literal("ftblang")
                .then(Commands.literal("export")
                                .then(Commands.argument("originalLangCode",StringArgumentType.word())

//                .requires(s->s.getServer() != null && s.getServer().isSingleplayer() || s.hasPermission(2))
                                        .executes(ctx ->{
                                            try{
                                                File parent = new File(Constants.PackMCMeta.GAMEDIR, Constants.PackMCMeta.OUTPUTFOLDER);
                                                File kubejsOutput = new File(parent, Constants.PackMCMeta.KUBEJSFOLDER);
                                                File questsFolder = new File(Constants.PackMCMeta.GAMEDIR, Constants.PackMCMeta.QUESTFOLDER);
                                                File kubejsBackupFile = new File(parent,Constants.PackMCMeta.KUBEJSBACKUPFOLDER);
                                                File mcKubeJsOut = new File(Constants.PackMCMeta.KUBEJSFOLDER);
                                                originalLangCode = ctx.getArgument("originalLangCode", String.class);
                                                for (String langCode : langList) {
                                                    Handler handler = new Handler();
                                                    File output2 = new File(Constants.PackMCMeta.GAMEDIR, Constants.PackMCMeta.QUESTFOLDER);
                                                    ServerQuestFile serverQuestFile = ServerQuestFile.INSTANCE;
                                                    serverQuestFile.save();
                                                    serverQuestFile.saveNow();
                                                    if (mcKubeJsOut.exists()){
                                                        FileUtils.copyDirectory(mcKubeJsOut,kubejsBackupFile);
                                                    }
                                                    BackPortUtils.backport(langCode);
                                                    QuestFile questFile = FTBQuests.PROXY.getQuestFile(false);
                                                    handler.handleRewardTables(questFile.rewardTables);
                                                    List<ChapterGroup> chapterGroups = questFile.chapterGroups;
                                                    chapterGroups.forEach(handler::handleChapterGroup);
                                                    HandlerCounter.setCounter(0);
                                                    List<Chapter> allChapters = questFile.getAllChapters();
                                                    allChapters.forEach(chapter -> {
                                                        handler.handleChapter(chapter);
                                                        handler.handleQuests(chapter.getQuests());
                                                        HandlerCounter.addChapters();
                                                    });
                                                    File output = new File(parent, Constants.PackMCMeta.QUESTFOLDER);
                                                    questFile.writeDataFull(output.toPath());
                                                    questFile.writeDataFull(output2.toPath());
                                                    ServerQuestFile.INSTANCE.load();
                                                    saveLang(langCode, kubejsOutput);
                                                    saveLang(langCode, mcKubeJsOut);
                                                }
                                                ctx.getSource().getPlayerOrException().displayClientMessage(Component.literal("FTB quests files exported to: " + parent.getAbsolutePath()), true);

                                            }catch(Exception e){
                                                e.printStackTrace();
                                            }
                                            return 1;
                                        })
                        )))

        );

    }
    private void saveLang(String lang, File parent) throws IOException
    {
        File fe = new File(parent, lang.toLowerCase(Locale.ROOT) + ".json");
        FileUtils.write(fe, FtbQuestLocalizerMod.gson.toJson(HandlerCounter.transKeys), StandardCharsets.UTF_8);
        PackUtils.createResourcePack(parent, FMLPaths.GAMEDIR.get().toFile()+"\\FTBLang\\FTB-Quests-Localization-Resourcepack.zip");
    }
}
