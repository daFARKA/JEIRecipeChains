package net.dafarka.jeirecipechains.util;

import net.dafarka.jeirecipechains.JEIRecipeChains;
import net.dafarka.jeirecipechains.base.ChainNode;
import net.dafarka.jeirecipechains.base.ChainType;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.*;

public class ChainBuilder {
  private final RecipeManager recipeManager;
  private final RegistryAccess registryAccess;
  private final Set<Item> excludedItems;


  public ChainBuilder(RecipeManager recipeManager, RegistryAccess registryAccess, Set<Item> excludedItems) {
    this.recipeManager = recipeManager;
    this.registryAccess = registryAccess;
    this.excludedItems = excludedItems;
  }

  public ChainNode buildChain(Item item, int depth, Set<Item> path) {
    if (depth > 8) return new ChainNode(new ItemStack(item)); // limit recursion
    if (excludedItems.contains(item)) return null;
    if (path.contains(item)) return new ChainNode(new ItemStack(item)); // stop loops

    ChainNode node = new ChainNode(new ItemStack(item));

    path.add(item);

    List<Recipe<?>> producingRecipes = recipeManager.getRecipes().stream()
        .filter(r -> r.getResultItem(registryAccess).getItem() == item)
        .toList();

    // If multiple recipes produce the same item => OR node
    if (producingRecipes.size() > 1) {
      node.setType(ChainType.OR);
    }

    for (Recipe<?> recipe : recipeManager.getRecipes()) {
      ItemStack result = recipe.getResultItem(registryAccess);
      if (result.getItem() == item) {
        // Collect unique ingredient counts
        Map<Item, Integer> ingredientCounts = new HashMap<>();
        for (Ingredient ing : recipe.getIngredients()) {
          ItemStack[] options = ing.getItems();

          for (ItemStack option : options) {
            if (!excludedItems.contains(option.getItem())) {
              ingredientCounts.merge(option.getItem(), option.getCount(), Integer::sum);
            }
          }
        }

        // Recursively add children
        for (Map.Entry<Item, Integer> entry : ingredientCounts.entrySet()) {
          Item child = entry.getKey();
          ItemStack childStack = new ItemStack(child, entry.getValue());

          ChainNode childNode = buildChain(child, depth + 1, new HashSet<>(path)); // pass a copy
          if (childNode != null) {
            childNode.setStack(childStack);
            node.addChild(childNode);
          }
        }
      }
    }

    return node;
  }
}