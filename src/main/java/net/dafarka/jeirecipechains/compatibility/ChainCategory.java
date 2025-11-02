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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;


import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ChainCategory implements IRecipeCategory<RecipeChain> {
  private static final ResourceLocation ICON_TEXTURE = new ResourceLocation("jeirecipechains", "textures/gui/icon_16x16.png");
  private final IDrawable icon;

  private final IDrawable background;
  private final Component title;

  private static final int SPACING_X = 20;
  private static final int SPACING_Y = 20;

  private final Map<ChainNode, Point> nodePositions = new HashMap<>();

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
    nodePositions.put(node, new Point(x, y));

    int childY = y + SPACING_Y;

    for (ChainNode child : node.getChildren()) {
      int childX = getChildX(child, x, node.getChildren());
      builder.addSlot(RecipeIngredientRole.INPUT, childX, childY).addItemStack(child.getStack());
      nodePositions.put(child, new Point(childX, childY));
      renderTree(builder, child, childX, childY);
    }

    return childY + SPACING_Y;
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

    // 1) compute subtree widths (units)
    double[] widths = new double[total];
    double totalWidth = 0;
    for (int i = 0; i < total; i++) {
      widths[i] = computeSubtreeWidth(siblings.get(i));
      totalWidth += widths[i];
    }

    // 2) compute center positions relative to left origin:
    // place intervals adjacent: childCenter = cumulative + widths[i]/2
    double[] centers = new double[total];
    double cumulative = 0;
    for (int i = 0; i < total; i++) {
      centers[i] = cumulative + widths[i] / 2.0;
      cumulative += widths[i];
    }

    // 3) compute group center and offset each center so the group is centered at parentX
    double groupCenter = (totalWidth) / 2.0;
    double localOffset = centers[index] - groupCenter; // negative => left, positive => right

    // 4) scale to pixels
    double pixelOffset = localOffset * SPACING_X;

    return parentX + (int) Math.round(pixelOffset);
  }

  @Override
  public void draw(RecipeChain recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
    drawBoxesFromMap(graphics, recipe.getRoot());

    Minecraft mc = Minecraft.getInstance();
    Font font = mc.font;

    drawRectange(graphics, 2, 2, 8, 8, (0xff << 24) + 0x00ff00);
    drawRectange(graphics, 2, 16, 8, 8, (0xff << 24) + 0x0000ff);
    graphics.drawString(font, "Craft Together", 12, 2, 0x000000, false);
    graphics.drawString(font, "Alternatives", 12, 16, 0x000000, false);
  }

  private void drawBoxesFromMap(GuiGraphics guiGraphics, ChainNode node) {
    int color = node.getType() == ChainType.AND ? 0x00ff00 : 0x0000ff;

    List<ChainNode> children = node.getChildren();
    if (children.isEmpty()) return;

    List<Point> points = new ArrayList<>();
    for (ChainNode child : children) {
      Point p = nodePositions.get(child);
      if (p != null) points.add(p);
    }

    if (!points.isEmpty()) {
      int minX = points.stream().mapToInt(p -> p.x).min().orElse(0);
      int maxX = points.stream().mapToInt(p -> p.x).max().orElse(0);
      int minY = points.stream().mapToInt(p -> p.y).min().orElse(0);
      int maxY = points.stream().mapToInt(p -> p.y).max().orElse(0);

      drawRectange(guiGraphics, minX, minY, (maxX - minX) + 16, (maxY - minY) + 16, (0x40 << 24) + color);
    }

    for (ChainNode child : children) {
      drawBoxesFromMap(guiGraphics, child);
    }
  }

  private void drawRectange(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
    guiGraphics.fill(x, y, x + width, y + height, color);
  }
}
