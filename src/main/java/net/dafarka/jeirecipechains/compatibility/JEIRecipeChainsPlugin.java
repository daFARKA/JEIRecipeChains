package net.dafarka.jeirecipechains.compatibility;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.dafarka.jeirecipechains.JEIRecipeChains;
import net.dafarka.jeirecipechains.base.ChainNode;
import net.dafarka.jeirecipechains.base.RecipeChain;
import net.dafarka.jeirecipechains.util.ChainBuilder;
import net.dafarka.jeirecipechains.util.ExclusionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JeiPlugin
public class JEIRecipeChainsPlugin implements IModPlugin {
  public static final RecipeType<RecipeChain> CHAIN_RECIPE_TYPE =
      new RecipeType<>(new ResourceLocation(JEIRecipeChains.MODID, "chain"), RecipeChain.class);

  @Override
  public ResourceLocation getPluginUid() {
    return new ResourceLocation(JEIRecipeChains.MODID, "jei_plugin");
  }

  @Override
  public void registerCategories(IRecipeCategoryRegistration registration) {
    registration.addRecipeCategories(new ChainCategory(registration.getJeiHelpers().getGuiHelper()));
  }

  @Override
  public void registerRecipes(IRecipeRegistration registration) {
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;

    RecipeManager recipeManager = mc.level.getRecipeManager();

    Set<Item> excluded = ExclusionUtil.getExcludedItems(recipeManager, mc.level.registryAccess());
    var builderUtil = new ChainBuilder(recipeManager, mc.level.registryAccess(), excluded);

    // Build one chain recipe per item
    List<RecipeChain> recipes = new ArrayList<>();
    for (Recipe<?> recipe : recipeManager.getRecipes()) {
      Item outputItem = recipe.getResultItem(mc.level.registryAccess()).getItem();
      ChainNode root = builderUtil.buildChain(outputItem, 0, new HashSet<>());

      if (root == null) continue;

      recipes.add(new RecipeChain(root));
      JEIRecipeChains.LOGGER.info(root.toString());
    }

    registration.addRecipes(CHAIN_RECIPE_TYPE, recipes);
  }
}
