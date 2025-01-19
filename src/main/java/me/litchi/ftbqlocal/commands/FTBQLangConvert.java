package me.litchi.ftbqlocal.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
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
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.eventbus.EventBus;
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
                                        .then(Commands.argument("originalLangCode",new LangObjectArgument())

//                .requires(s->s.getServer() != null && s.getServer().isSingleplayer() || s.hasPermission(2))
                                        .executes(ctx ->{
                                            try{
                                                File parent = new File(Constants.PackMCMeta.GAMEDIR, Constants.PackMCMeta.OUTPUTFOLDER);
                                                File kubejsOutput = new File(Constants.PackMCMeta.GAMEDIR, Constants.PackMCMeta.KUBEJSFOLDER);
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
                                                        //System.out.println(chapter.filename);
                                                        handler.handleChapter(chapter);
                                                        handler.handleQuests(chapter.quests);
                                                        HandlerCounter.addChapters();
                                                    });

                                                    File output = new File(parent, Constants.PackMCMeta.QUESTFOLDER);
                                                    questFile.writeDataFull(output.toPath());
                                                    questFile.writeDataFull(output2.toPath());
                                                    ServerQuestFile.INSTANCE.load();
                                                    saveLang(langCode, kubejsOutput);
                                                    saveLang(langCode, mcKubeJsOut);
//                                                    if(!langStr.equalsIgnoreCase("en_us")){
//                                                        saveLang("en_us", kubejsOutput);
//                                                    }
                                                }
                                                ctx.getSource().getPlayerOrException().displayClientMessage(new TextComponent("FTB quests files exported to: " + parent.getAbsolutePath()), true);

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
        //FileUtils.copyFile(new File(FMLPaths.GAMEDIR.get().toFile() + "\\FTBLang\\FTB-Quests-Localization-Resourcepack.zip"), new File(Constants.PackMCMeta.GAMEDIR + "\\resourcepacks\\" + Constants.PackMCMeta.PACKNAME));
    }
}
