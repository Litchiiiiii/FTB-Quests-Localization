package me.litchi.ftbqlocal.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class LangObjectArgument implements ArgumentType<String> {
    private static final List<String> examples = ImmutableList.of("en_us", "zh_cn");
    private static final String[] LANGCODES =  {"en_us","zh_cn","zh_tw","zh_hk","de_de","es_es","fr_fr","ja_jp","ko_kr","ru_ru"};

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(LANGCODES).toList(),builder);
    }

    @Override
    public Collection<String> getExamples() {
        return examples;
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }
}
