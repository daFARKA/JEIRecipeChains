package net.dafarka.jeirecipechains.util;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;

public class ExclusionUtil {
  private static final String[] COLOR_NAMES = {
      "white", "orange", "magenta", "light_blue", "yellow", "lime",
      "pink", "gray", "light_gray", "cyan", "purple", "blue",
      "brown", "green", "red", "black"
  };

  public static Set<Item> getExcludedItems(RecipeManager recipeManager, RegistryAccess registryAccess) {
    Set<Item> excluded = new HashSet<>();

    excluded.addAll(getColoredItems());
    excluded.addAll(getRecyclables(recipeManager, registryAccess));
    excluded.addAll(getRawBlocks());

    return excluded;
  }

  private static Set<Item> getRecyclables(RecipeManager recipeManager, RegistryAccess registryAccess) {
    Set<Item> excluded = new HashSet<>();

    for (Recipe<?> recipe : recipeManager.getRecipes()) {
      if (recipe instanceof SmeltingRecipe smelt) {
        Item output = smelt.getResultItem(registryAccess).getItem();

        for (var ing : smelt.getIngredients()) {
          for (var stack : ing.getItems()) {
            Item input = stack.getItem();

            // Only exclude if the input is a crafted item (tool/armor) and the output is a material
            if (isRecyclable(input, output)) {
              excluded.add(input);
            }
          }
        }
      }
    }

    return excluded;
  }

  private static boolean isRecyclable(Item input, Item output) {
    // Basic heuristic:
    // - Input is a tool/armor/crafted item
    // - Output is a base material (ingot/nugget)
    String inputName = BuiltInRegistries.ITEM.getKey(input).getPath();
    String outputName = BuiltInRegistries.ITEM.getKey(output).getPath();

    // Example: iron_pickaxe → iron_nugget
    return (inputName.contains("pickaxe") || inputName.contains("axe") ||
        inputName.contains("shovel") || inputName.contains("sword") ||
        inputName.contains("helmet") || inputName.contains("chestplate") ||
        inputName.contains("hoe") || inputName.contains("horse_armor") ||
        inputName.contains("leggings") || inputName.contains("boots")) &&
        (outputName.contains("nugget"));
  }

  private static Set<Item> getColoredItems() {
    Set<Item> excluded = new HashSet<>();
    for (Item item : BuiltInRegistries.ITEM) {
      String name = BuiltInRegistries.ITEM.getKey(item).getPath(); // e.g., "blue_wool"
      for (String color : COLOR_NAMES) {
        if (name.startsWith(color + "_")) {
          excluded.add(item);
          break;
        }
      }
    }
    return excluded;
  }

  public static Set<Item> getRawBlocks() {
    Set<Item> excluded = new HashSet<>();

    for (Item item : BuiltInRegistries.ITEM) {
      String path = BuiltInRegistries.ITEM.getKey(item).getPath();

      if (path.startsWith("raw_")) {
        excluded.add(item);
      }
    }

    return excluded;
  }
}
