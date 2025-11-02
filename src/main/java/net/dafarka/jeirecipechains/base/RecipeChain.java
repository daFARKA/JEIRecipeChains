package net.dafarka.jeirecipechains.base;

public class RecipeChain {
  private final ChainNode root;

  public RecipeChain(ChainNode root) {
    this.root = root;
  }

  public ChainNode getRoot() {
    return root;
  }

  @Override
  public String toString() {
    return root.toString();
  }
}