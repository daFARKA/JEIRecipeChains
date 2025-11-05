package net.dafarka.jeirecipechains.compatibility;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.dafarka.jeirecipechains.base.ChainNode;
import net.dafarka.jeirecipechains.base.ChainType;
import net.dafarka.jeirecipechains.base.RecipeChain;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;


import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class ChainCategory implements IRecipeCategory<RecipeChain> {
  private static final ResourceLocation ICON_TEXTURE = new ResourceLocation("jeirecipechains", "textures/gui/icon_16x16.png");
  private final IDrawable icon;

  private final IDrawable background;
  private final Component title;

  private static final int SPACING_X = 20;
  private static final int SPACING_Y = 20;

  private static final int COLOR_ADD = 0x00ff00;
  private static final int COLOR_OR = 0x0000ff;

  private static final Set<String> IGNORED_TAGS = Set.of(
      "minecraft:trim_materials"
  );

  public ChainCategory(IGuiHelper helper) {
    this.background = helper.createBlankDrawable(320, 240);
    this.title = Component.translatable("chain.category.title");

    IDrawableBuilder builder = helper.drawableBuilder(ICON_TEXTURE, 0, 0, 16, 16);
    builder.setTextureSize(16, 16);
    this.icon = builder.build();
  }


  @Override
  public RecipeType<RecipeChain> getRecipeType() {
    return JEIRecipeChainsPlugin.CHAIN_RECIPE_TYPE;
  }


  @Override
  public Component getTitle() { return title; }


  @Override
  public IDrawable getBackground() { return background; }

  @Override
  public @Nullable IDrawable getIcon() {
    return icon;
  }


  @Override
  public void setRecipe(IRecipeLayoutBuilder builder, RecipeChain recipe, IFocusGroup focuses) {
    renderTree(builder, recipe.getRoot(), getWidth() / 2, 8);
  }

  private int renderTree(IRecipeLayoutBuilder builder, ChainNode node, int x, int y) {
    if (node == null) return y;

    builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).addItemStack(node.getStack());
    node.setGuiPosition(new Point(x, y));

    int childY = y + SPACING_Y;

    List<ChainNode> children = node.getChildren();
    Set<ChainNode> visited = new HashSet<>();

    // Build a map of node -> set of tags (as strings)
    Map<ChainNode, Set<String>> nodeTags = new HashMap<>();
    for (ChainNode child : children) {
      if (visited.contains(child)) continue;

      Set<String> tags = child.getStack().getItem()
          .builtInRegistryHolder()
          .tags()
          .map(tag -> tag.location().toString())
          .filter(tag -> !IGNORED_TAGS.contains(tag))
          .collect(Collectors.toSet());

      nodeTags.put(child, tags);
      visited.add(child);
    }

    // Group by shared tags
    Map<String, List<ChainNode>> groups = new HashMap<>();
    visited.clear();

    for (ChainNode child : children) {
      if (visited.contains(child)) continue;

      Set<String> childTags = nodeTags.getOrDefault(child, Set.of());
      if (childTags.isEmpty()) {
        // no tags: just make a unique group per item
        String itemKey = BuiltInRegistries.ITEM.getKey(child.getStack().getItem()).toString();
        groups.put(itemKey, List.of(child));
        visited.add(child);
        continue;
      }

      // Find group tag key (the tag this group represents)
      String groupTagKey = null;
      List<ChainNode> group = new ArrayList<>();
      group.add(child);
      visited.add(child);

      for (ChainNode other : children) {
        if (child == other || visited.contains(other)) continue;

        Set<String> otherTags = nodeTags.getOrDefault(other, Set.of());
        if (otherTags.isEmpty()) continue;

        // Intersection
        Set<String> shared = new HashSet<>(childTags);
        shared.retainAll(otherTags);

        if (!shared.isEmpty()) {
          group.add(other);
          visited.add(other);

          // Use first shared tag as key (consistent grouping)
          if (groupTagKey == null) {
            groupTagKey = shared.iterator().next();
          }
        }
      }

      // Fallback key if none found
      if (groupTagKey == null) {
        groupTagKey = childTags.iterator().next();
      }

      groups.put(groupTagKey, group);
    }

    // If no tags found at all, ensure each child is shown
    if (groups.isEmpty() && !children.isEmpty()) {
      for (ChainNode c : children) groups.put("", List.of(c));
    }

    // groups: Map<String, List<ChainNode>> created earlier
    List<List<ChainNode>> groupedLists = new ArrayList<>(groups.values());

    // Pick one representative node per group (for positioning)
    List<ChainNode> representativeNodes = new ArrayList<>();
    for (List<ChainNode> group : groupedLists) {
      if (!group.isEmpty()) {
        representativeNodes.add(group.get(0));
      }
    }

    // Render each representative node
    for (int i = 0; i < representativeNodes.size(); i++) {
      ChainNode representative = representativeNodes.get(i);
      List<ChainNode> group = groupedLists.get(i);

      ChainNode pruned = pruneNode(representative);
      List<ChainNode> prunedSiblings = new ArrayList<>();
      for (ChainNode child : representativeNodes) {
        prunedSiblings.add(pruneNode(child));
      }

      int childX = getChildX(pruned, x, prunedSiblings);

      // JEI automatically cycles through multiple stacks in one slot
      builder.addSlot(RecipeIngredientRole.INPUT, childX, childY)
          .addItemStacks(
              group.stream()
                  .map(c -> c.getStack())
                  .toList()
          );

      representative.setGuiPosition(new Point(childX, childY));

      // Recurse using the representative node
      renderTree(builder, representative, childX, childY);
    }

    return childY + SPACING_Y;
  }

  private ChainNode pruneNode(ChainNode node) {
    if (node == null) return null;

    List<ChainNode> children = node.getChildren();
    if (children.isEmpty()) return node;

    // Map children to their tags
    Map<ChainNode, Set<String>> nodeTags = new HashMap<>();
    for (ChainNode child : children) {
      Set<String> tags = child.getStack().getItem()
          .builtInRegistryHolder()
          .tags()
          .map(tag -> tag.location().toString())
          .filter(tag -> !IGNORED_TAGS.contains(tag))
          .collect(Collectors.toSet());
      nodeTags.put(child, tags);
    }

    // Group children by shared tags
    Map<String, List<ChainNode>> groups = new HashMap<>();
    Set<ChainNode> visited = new HashSet<>();

    for (ChainNode child : children) {
      if (visited.contains(child)) continue;

      Set<String> childTags = nodeTags.getOrDefault(child, Set.of());
      if (childTags.isEmpty()) {
        // Use item key as group
        String itemKey = BuiltInRegistries.ITEM.getKey(child.getStack().getItem()).toString();
        groups.put(itemKey, List.of(child));
        visited.add(child);
        continue;
      }

      String groupTagKey = null;
      List<ChainNode> group = new ArrayList<>();
      group.add(child);
      visited.add(child);

      for (ChainNode other : children) {
        if (child == other || visited.contains(other)) continue;
        Set<String> otherTags = nodeTags.getOrDefault(other, Set.of());
        if (otherTags.isEmpty()) continue;

        Set<String> shared = new HashSet<>(childTags);
        shared.retainAll(otherTags);

        if (!shared.isEmpty()) {
          group.add(other);
          visited.add(other);

          if (groupTagKey == null) groupTagKey = shared.iterator().next();
        }
      }

      if (groupTagKey == null) groupTagKey = childTags.iterator().next();
      groups.put(groupTagKey, group);
    }

    // Build pruned children: pick one representative per group
    List<ChainNode> prunedChildren = groups.values().stream()
        .map(list -> pruneNode(list.get(0))) // recursive prune
        .toList();

    // Create a new ChainNode with only pruned children
    ChainNode prunedNode = new ChainNode(node.getStack(), node.getType());
    prunedNode.getChildren().addAll(prunedChildren);

    return prunedNode;
  }

  private int computeSubtreeWidth(ChainNode node) {
    if (node.getChildren().isEmpty()) return 1;

    int width = 0;
    for (ChainNode child : node.getChildren()) {
      width += computeSubtreeWidth(child);
    }

    return Math.max(width, node.getChildren().size()); // ensure width ≥ number of children
  }

  private int getChildX(ChainNode child, int parentX, List<ChainNode> siblings) {
    if (siblings == null || siblings.isEmpty()) return parentX;

    int total = siblings.size();
    int index = siblings.indexOf(child);
    if (index < 0) return parentX;

    double[] widths = new double[total];
    double totalWidth = 0;
    for (int i = 0; i < total; i++) {
      widths[i] = computeSubtreeWidth(siblings.get(i));
      totalWidth += widths[i];
    }

    // compute center positions relative to left origin
    double[] centers = new double[total];
    double cumulative = 0;
    for (int i = 0; i < total; i++) {
      centers[i] = cumulative + widths[i] / 2.0;
      cumulative += widths[i];
    }

    // center group at parentX
    double groupCenter = totalWidth / 2.0;
    double localOffset = centers[index] - groupCenter;

    double pixelOffset = localOffset * SPACING_X;

    return parentX + (int) Math.round(pixelOffset);
  }

  @Override
  public void draw(RecipeChain recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
    drawBoxesFromMap(graphics, recipe.getRoot());

    Minecraft mc = Minecraft.getInstance();
    Font font = mc.font;

    drawRectangle(graphics, 2, 2, 8, 8, (0xff << 24) + COLOR_ADD);
    drawRectangle(graphics, 2, 16, 8, 8, (0xff << 24) + COLOR_OR);
    graphics.drawString(font, "Craft Together", 12, 2, 0x000000, false);
    graphics.drawString(font, "Alternatives", 12, 16, 0x000000, false);
  }

  private void drawBoxesFromMap(GuiGraphics guiGraphics, ChainNode node) {
    int color = node.getType() == ChainType.AND ? COLOR_ADD : COLOR_OR;

    List<ChainNode> children = node.getChildren();
    if (children.isEmpty()) return;

    List<Point> points = new ArrayList<>();
    for (ChainNode child : children) {
      Point p = child.getGuiPosition();
      if (p != null) points.add(p);
    }

    if (!points.isEmpty()) {
      int minX = points.stream().mapToInt(p -> p.x).min().orElse(0);
      int maxX = points.stream().mapToInt(p -> p.x).max().orElse(0);
      int minY = points.stream().mapToInt(p -> p.y).min().orElse(0);
      int maxY = points.stream().mapToInt(p -> p.y).max().orElse(0);

      drawRectangle(guiGraphics, minX, minY, (maxX - minX) + 16, (maxY - minY) + 16, (0x40 << 24) + color);
    }

    for (ChainNode child : children) {
      drawBoxesFromMap(guiGraphics, child);
    }
  }

  private void drawRectangle(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
    guiGraphics.fill(x, y, x + width, y + height, color);
  }
}
