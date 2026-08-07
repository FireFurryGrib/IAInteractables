package me.FireKillGrib.iAInteractables.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import xyz.xenondevs.invui.item.ItemProvider;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Getter
public class SmithingTable {
    private final String name;
    private final String title;
    private final String namespacedID;
    private final List<String> structure;
    private final ItemProvider filler;
    private final Set<SmithingRecipe> recipes;
    private final WorkbenchEffects effects;
    private final List<String> specialFunctions; // Новое поле для тегов '$'
}