package com.leyline.hollowforged;
/** Original container geometry, retained before vanilla normalizes CraftingInput. */
public interface GridOrigin {
 void hollowforged$origin(int width,int height,int left,int top);
 boolean hollowforged$matchesOrigin(int width,int height,int left,int top);
}
