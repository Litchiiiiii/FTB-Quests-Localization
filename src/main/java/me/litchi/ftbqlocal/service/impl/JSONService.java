package me.litchi.ftbqlocal.service.impl;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import me.litchi.ftbqlocal.service.FtbQService;
import me.litchi.ftbqlocal.utils.HandlerCounter;
import net.minecraft.Util;
import net.minecraft.network.chat.*;

import java.util.ArrayList;
import java.util.List;

import static me.litchi.ftbqlocal.utils.HandlerCounter.*;

public class JSONService implements FtbQService {
    @Override
    public  String handleJSON(Component parsedText) {
        try{
            String jsonString;
            List<Component> flatList = parsedText.toFlatList(Style.EMPTY);
            StringBuilder jsonStringBuilder = new StringBuilder("[\"\",");
            for(Component c : flatList){
                HandlerCounter.addCounter();
                String text = c.getContents().toString().substring(8, c.getContents().toString().length() -1);
                Style style = c.getStyle();
                if(style != Style.EMPTY){
                    jsonStringBuilder.append("{");
                    TextColor color = style.getColor();
                    if(color != null){
                        jsonStringBuilder.append("\"color\":\"").append(color).append("\",");
                    }
                    if (style.isUnderlined()){
                        jsonStringBuilder.append("\"underlined\":"+ 1).append(",");
                    }
                    if (style.isStrikethrough()){
                        jsonStringBuilder.append("\"strikethrough\":"+ 1).append(",");
                    }
                    if (style.isBold()){
                        jsonStringBuilder.append("\"bold\":"+ 1).append(",");
                    }
                    if (style.isItalic()){
                        jsonStringBuilder.append("\"italic\":"+ 1).append(",");
                    }
                    if (style.isObfuscated()){
                        jsonStringBuilder.append("\"obfuscated\":"+ 1).append(",");
                    }
                    String textKey = HandlerCounter.getPrefix() + ".rich_description" + HandlerCounter.getCounter();
                    HandlerCounter.transKeys.put(textKey, addPercent(text));
                    ClickEvent clickEvent = style.getClickEvent();

                    if(clickEvent != null){
                        jsonStringBuilder.append("\"translate\":\"").append(textKey).append("\",");
                        String clickEventValue = clickEvent.getValue();
                        String clickEventAction = clickEvent.getAction().getName();
                        jsonStringBuilder.append("\"clickEvent\":{\"action\":\"").append(clickEventAction).append("\",\"value\":\"").append(clickEventValue).append("\"},");
                    } else {
                        jsonStringBuilder.append("\"translate\":\"").append(textKey).append("\"},");
                    }
                    HoverEvent hoverEvent = style.getHoverEvent();
                    if(hoverEvent != null){
                        String hoverEventAction = hoverEvent.getAction().getName();
                        JsonObject hoverEventJSON = hoverEvent.serialize();
                        //System.out.println(hoverEventJSON);
                        JsonObject hoverValue = hoverEventJSON.get("contents").getAsJsonObject();
                        String hoverText = hoverValue.get("text").getAsString();
                        addCounter();
                        textKey = HandlerCounter.getPrefix() + ".rich_description" + HandlerCounter.getCounter();
                        String hoverString = "\"hoverEvent\":{\"action\":\"" + hoverEventAction + "\",\"contents\":{\"translate\":\"" + textKey +"\"}},";
                        HandlerCounter.transKeys.put(textKey, addPercent(hoverText));
                        jsonStringBuilder.append(hoverString);
                    }
                } else{
                    String textKey = HandlerCounter.getPrefix() + ".rich_description" + HandlerCounter.getCounter();
                    HandlerCounter.transKeys.put(textKey, addPercent(text));
                    jsonStringBuilder.append("{\"translate\":\"").append(textKey).append("\"},");
                }
            }
            jsonString = jsonStringBuilder.toString();
            jsonString = jsonString.substring(0, jsonString.length()-1);
            jsonString += "}]";
            return jsonString;
        }catch(Exception e){
            HandlerCounter.log.info(e.getMessage());
            return "";
        }
    }

}
