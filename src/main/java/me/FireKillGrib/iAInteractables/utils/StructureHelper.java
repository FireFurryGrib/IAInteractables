package me.FireKillGrib.iAInteractables.utils;

import java.util.List;

public class StructureHelper {
    public static String[] translate(List<String> structure, List<String> funcs) {
        String[] mapped = new String[structure.size()];
        int fIdx = 0;
        for (int i = 0; i < structure.size(); i++) {
            StringBuilder sb = new StringBuilder();
            for (char c : structure.get(i).replace(" ", "").toCharArray()) {
                if (c == '$') {
                    if (funcs != null && fIdx < funcs.size()) {
                        String tag = funcs.get(fIdx++);
                        if (tag.equals("PRO")) sb.append('1');
                        else if (tag.equals("RES")) sb.append('2');
                        else if (tag.equals("BAC")) sb.append('3');
                        else if (tag.equals("BUC")) sb.append('4');
                        else if (tag.equals("LQ1")) sb.append('5');
                        else if (tag.equals("LQ2")) sb.append('6');
                        else if (tag.equals("LQ3")) sb.append('7');
                        else if (tag.equals("LQU")) sb.append('8');
                        else sb.append('X');
                    } else sb.append('X');
                } else {
                    sb.append(c);
                }
            }
            mapped[i] = sb.toString();
        }
        return mapped;
    }
}