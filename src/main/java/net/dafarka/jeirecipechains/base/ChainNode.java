package net.dafarka.jeirecipechains.base;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChainNode {
  private ItemStack stack;
  private final List<ChainNode> children = new ArrayList<>();

  private ChainType type = ChainType.AND;

  public ChainNode(ItemStack stack) {
    this.stack = stack;
  }

  public void addChild(ChainNode child) {
    children.add(child);
  }

  public ItemStack getStack() {
    return stack;
  }

  public void setStack(ItemStack stack) {
    if (stack == null) return;
    this.stack = stack;
  }

  public List<ChainNode> getChildren() {
    return children;
  }

  public ChainType getType() {
    return type;
  }

  public void setType(ChainType type) {
    this.type = type;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("[stack=");
    sb.append(stack);
    sb.append(", children=");
    for (ChainNode child : children) {
      sb.append(child.toString());
    }

    return sb.append("]").toString();
  }
}
