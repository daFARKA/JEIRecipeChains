package net.dafarka.jeirecipechains.base;

import net.minecraft.world.item.ItemStack;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChainNode {
  private ItemStack stack;
  private final List<ChainNode> children = new ArrayList<>();
  private Point guiPosition;

  private ChainType type = ChainType.AND;

  public ChainNode(ItemStack stack) {
    this.stack = stack;
  }

  public ChainNode(ItemStack stack, ChainType type) {
    this.stack = stack;
    this.type = type;
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

  public Point getGuiPosition() {
    return guiPosition;
  }

  public void setGuiPosition(Point guiPosition) {
    this.guiPosition = guiPosition;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("[stack=");
    sb.append(stack);
    sb.append(", type=");
    sb.append(type);
    sb.append(", children=");
    for (ChainNode child : children) {
      sb.append(child.toString());
    }

    return sb.append("]").toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ChainNode other)) return false;
    return ItemStack.matches(this.stack, other.stack) && this.type == other.type && this.children.equals(other.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stack.getItem(), type);
  }
}
