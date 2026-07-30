package com.dopa.randomutilities.compat.jei;

import com.dopa.randomutilities.config.GeneratorRecipe;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.registry.ModBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Mini world-schematic JEI category for resource generators:
 * inventory above result, adjacent sides beside the generator, optional below block.
 */
public class ResourceGeneratorRecipeCategory extends AbstractRecipeCategory<GeneratorJeiRecipe> {
    public static final IRecipeType<GeneratorJeiRecipe> RECIPE_TYPE = IRecipeType.create(
            dOPasRandomUtilities.MOD_ID,
            "resource_generator",
            GeneratorJeiRecipe.class
    );

    public static final int WIDTH = 152;
    public static final int HEIGHT = 112;

    private static final int GEN_X = 68;
    private static final int GEN_Y = 52;
    /** Inventory (chest) on top, result directly under it — same size/alignment. */
    private static final int CONTAINER_X = 68;
    private static final int CONTAINER_Y = 1;
    private static final int OUTPUT_X = 68;
    private static final int OUTPUT_Y = 20;
    private static final int BELOW_X = 68;
    private static final int BELOW_Y = 76;

    private static final int SIDE_OFFSET_X = 42;
    private static final int SIDE_STACK_Y = 12;

    private static final float ARROW_SCALE = 0.42F;
    private static final float HINT_SCALE = 0.75F;
    /** Clear gap under the below-slot (slot is 18px tall including background). */
    private static final float HINT_Y = BELOW_Y + 22.0F;

    private static final int COLOR_KEEP = 0x7DCEA0;
    private static final int COLOR_CONSUME = 0xFF6B6B;
    private static final int COLOR_RATE = 0xF0C75E;

    private static final List<ItemStack> OUTPUT_CONTAINERS = List.of(
            new ItemStack(Items.CHEST),
            new ItemStack(Items.BARREL),
            new ItemStack(Items.HOPPER),
            new ItemStack(Items.TRAPPED_CHEST),
            new ItemStack(Items.SHULKER_BOX)
    );

    private final IDrawable consumeMarker;
    private final IDrawableStatic recipeArrow;

    public ResourceGeneratorRecipeCategory(IGuiHelper guiHelper) {
        super(
                RECIPE_TYPE,
                Component.translatable("jei.dopasrandomutilities.resource_generator"),
                guiHelper.createDrawableIngredient(
                        VanillaTypes.ITEM_STACK,
                        new ItemStack(ModBlocks.BASIC_STONE_GENERATOR.asItem())
                ),
                WIDTH,
                HEIGHT
        );
        this.consumeMarker = createTinyBarrier(guiHelper);
        this.recipeArrow = guiHelper.getRecipeArrow();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GeneratorJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, GEN_X, GEN_Y)
                .setStandardSlotBackground()
                .add(recipe.generatorStack())
                .setSlotName("generator")
                .addRichTooltipCallback((slotView, tooltip) ->
                        tooltip.add(Component.translatable("jei.dopasrandomutilities.tooltip.generator")));

        List<GeneratorJeiRecipe.SideIngredient> sides = recipe.sideIngredients();
        int[][] sidePositions = sidePositions(sides.size());
        for (int i = 0; i < sides.size() && i < sidePositions.length; i++) {
            GeneratorJeiRecipe.SideIngredient side = sides.get(i);
            int x = sidePositions[i][0];
            int y = sidePositions[i][1];
            IRecipeSlotBuilder slot = builder.addInputSlot(x, y)
                    .setStandardSlotBackground()
                    .setSlotName("side_" + i)
                    .setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16);

            if (side.isFluid()) {
                Fluid fluid = side.fluid();
                if (fluid != null) {
                    slot.add(fluid, FluidType.BUCKET_VOLUME);
                }
            } else {
                Block block = side.block();
                if (block != null && block.asItem() != Items.AIR) {
                    slot.add(new ItemStack(block.asItem()));
                }
            }

            if (side.consume()) {
                slot.setOverlay(consumeMarker, 10, 10);
                String emphasisKey = side.isFluid()
                        ? "jei.dopasrandomutilities.tooltip.side_consume_fluid"
                        : "jei.dopasrandomutilities.tooltip.side_consume_block";
                slot.addRichTooltipCallback((slotView, tooltip) ->
                        tooltip.add(sideTooltip(
                                "jei.dopasrandomutilities.tooltip.side_prefix",
                                emphasisKey,
                                COLOR_CONSUME
                        )));
            } else {
                slot.addRichTooltipCallback((slotView, tooltip) ->
                        tooltip.add(sideTooltip(
                                "jei.dopasrandomutilities.tooltip.side_prefix",
                                "jei.dopasrandomutilities.tooltip.side_keep",
                                COLOR_KEEP
                        )));
            }
        }

        Block below = recipe.belowBlock();
        if (below != null && below.asItem() != Items.AIR) {
            builder.addInputSlot(BELOW_X, BELOW_Y)
                    .setStandardSlotBackground()
                    .add(new ItemStack(below.asItem()))
                    .setSlotName("below")
                    .addRichTooltipCallback((slotView, tooltip) ->
                            tooltip.add(Component.translatable("jei.dopasrandomutilities.tooltip.below")));
        }

        if (recipe.isInsertOutput()) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, CONTAINER_X, CONTAINER_Y)
                    .setStandardSlotBackground()
                    .addItemStacks(OUTPUT_CONTAINERS)
                    .setSlotName("container")
                    .addRichTooltipCallback((slotView, tooltip) ->
                            tooltip.add(Component.translatable("jei.dopasrandomutilities.tooltip.container")));
        }

        GeneratorRecipe data = recipe.recipe();
        List<ItemStack> results = recipe.resultStacks();
        // DROP: result sits where the inventory would be. INSERT: result under the inventory.
        int resultX = OUTPUT_X;
        int resultY = recipe.isInsertOutput() ? OUTPUT_Y : CONTAINER_Y;
        IRecipeSlotBuilder output = builder.addOutputSlot(resultX, resultY)
                .setStandardSlotBackground()
                .setSlotName("result");
        if (!results.isEmpty()) {
            output.addItemStacks(results);
        }
        if (recipe.isInsertOutput()) {
            output.addRichTooltipCallback((slotView, tooltip) -> {
                tooltip.add(Component.translatable("jei.dopasrandomutilities.tooltip.output_insert"));
                tooltip.add(rateLine(data.amount(), data.ticks()));
            });
        } else {
            output.addRichTooltipCallback((slotView, tooltip) -> {
                tooltip.add(Component.translatable("jei.dopasrandomutilities.tooltip.output_drop"));
                tooltip.add(rateLine(data.amount(), data.ticks()));
            });
        }

        if (sides.size() > 1) {
            builder.setShapeless(WIDTH - 18, 0);
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, GeneratorJeiRecipe recipe, IFocusGroup focuses) {
        // Hint is drawn scaled in draw() so it always fits the panel width.
    }

    @Override
    public void draw(
            GeneratorJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphicsExtractor guiGraphics,
            double mouseX,
            double mouseY
    ) {
        int centerX = GEN_X + 8;
        int genCenterY = GEN_Y + 8;

        int resultY = recipe.isInsertOutput() ? OUTPUT_Y : CONTAINER_Y;
        if (recipe.isInsertOutput()) {
            // Inventory → result
            drawV(guiGraphics, centerX, CONTAINER_Y + 18, OUTPUT_Y - 1);
        }

        // Generator → result (upward furnace arrow instead of a plain line)
        drawUpArrow(guiGraphics, centerX, resultY + 18, GEN_Y);

        if (recipe.belowBlock() != null) {
            drawV(guiGraphics, centerX, GEN_Y + 18, BELOW_Y - 1);
        }

        drawSideLinks(guiGraphics, sidePositions(recipe.sideIngredients().size()), centerX, genCenterY);
        drawHoverHint(guiGraphics);
    }

    @Override
    public @Nullable Identifier getIdentifier(GeneratorJeiRecipe recipe) {
        return recipe.recipeId();
    }

    private void drawUpArrow(GuiGraphicsExtractor guiGraphics, int centerX, int gapTop, int gapBottom) {
        int midY = (gapTop + gapBottom) / 2;
        int arrowW = recipeArrow.getWidth();
        int arrowH = recipeArrow.getHeight();

        var pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(centerX, midY);
        // Vanilla recipe arrow points right; rotate so it points up toward the result.
        pose.rotate(-(float) (Math.PI / 2.0));
        pose.scale(ARROW_SCALE, ARROW_SCALE);
        pose.translate(-arrowW / 2.0F, -arrowH / 2.0F);
        recipeArrow.draw(guiGraphics);
        pose.popMatrix();
    }

    private static void drawHoverHint(GuiGraphicsExtractor guiGraphics) {
        Font font = Minecraft.getInstance().font;
        Component hint = Component.translatable("jei.dopasrandomutilities.hover_hint");
        float width = font.width(hint) * HINT_SCALE;

        var pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate((WIDTH - width) / 2.0F, HINT_Y);
        pose.scale(HINT_SCALE, HINT_SCALE);
        guiGraphics.text(font, hint, 0, 0, 0xFF555555, false);
        pose.popMatrix();
    }

    private static int[][] sidePositions(int count) {
        return switch (Math.min(count, 4)) {
            case 1 -> new int[][] {
                    {GEN_X - SIDE_OFFSET_X, GEN_Y}
            };
            case 2 -> new int[][] {
                    {GEN_X - SIDE_OFFSET_X, GEN_Y},
                    {GEN_X + SIDE_OFFSET_X, GEN_Y}
            };
            case 3 -> new int[][] {
                    {GEN_X - SIDE_OFFSET_X, GEN_Y - SIDE_STACK_Y},
                    {GEN_X - SIDE_OFFSET_X, GEN_Y + SIDE_STACK_Y},
                    {GEN_X + SIDE_OFFSET_X, GEN_Y}
            };
            case 4 -> new int[][] {
                    {GEN_X - SIDE_OFFSET_X, GEN_Y - SIDE_STACK_Y},
                    {GEN_X - SIDE_OFFSET_X, GEN_Y + SIDE_STACK_Y},
                    {GEN_X + SIDE_OFFSET_X, GEN_Y - SIDE_STACK_Y},
                    {GEN_X + SIDE_OFFSET_X, GEN_Y + SIDE_STACK_Y}
            };
            default -> new int[0][];
        };
    }

    private static MutableComponent sideTooltip(String prefixKey, String emphasisKey, int emphasisColor) {
        return Component.translatable(prefixKey)
                .append(Component.literal(" "))
                .append(Component.translatable(emphasisKey).withColor(TextColor.fromRgb(emphasisColor)));
    }

    private static MutableComponent rateLine(int amount, int ticks) {
        return Component.translatable("jei.dopasrandomutilities.tooltip.output_rate", amount, ticks)
                .withColor(TextColor.fromRgb(COLOR_RATE));
    }

    private static IDrawable createTinyBarrier(IGuiHelper guiHelper) {
        IDrawable barrier = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(Items.BARRIER)
        );
        return new IDrawable() {
            @Override
            public int getWidth() {
                return 7;
            }

            @Override
            public int getHeight() {
                return 7;
            }

            @Override
            public void draw(GuiGraphicsExtractor guiGraphics, int xOffset, int yOffset) {
                var pose = guiGraphics.pose();
                pose.pushMatrix();
                pose.translate(xOffset, yOffset);
                pose.scale(7.0F / 16.0F, 7.0F / 16.0F);
                barrier.draw(guiGraphics);
                pose.popMatrix();
            }
        };
    }

    /**
     * Clean side wiring: one horizontal spine through the generator, with short vertical stubs
     * to each side slot (no overlapping L-shapes through the center).
     */
    private static void drawSideLinks(
            GuiGraphicsExtractor graphics,
            int[][] positions,
            int genCenterX,
            int spineY
    ) {
        if (positions.length == 0) {
            return;
        }

        int leftmost = genCenterX;
        int rightmost = genCenterX;
        for (int[] pos : positions) {
            int slotCenterX = pos[0] + 8;
            leftmost = Math.min(leftmost, slotCenterX);
            rightmost = Math.max(rightmost, slotCenterX);
        }

        if (leftmost < genCenterX) {
            drawH(graphics, leftmost, genCenterX, spineY);
        }
        if (rightmost > genCenterX) {
            drawH(graphics, genCenterX, rightmost, spineY);
        }

        for (int[] pos : positions) {
            int slotCenterX = pos[0] + 8;
            int slotCenterY = pos[1] + 8;
            if (slotCenterY != spineY) {
                drawV(graphics, slotCenterX, slotCenterY, spineY);
            }
        }
    }

    private static void drawH(GuiGraphicsExtractor graphics, int x1, int x2, int y) {
        int left = Math.min(x1, x2);
        int right = Math.max(x1, x2);
        graphics.fill(left, y, right + 1, y + 1, 0xFF8A8A8A);
    }

    private static void drawV(GuiGraphicsExtractor graphics, int x, int y1, int y2) {
        int top = Math.min(y1, y2);
        int bottom = Math.max(y1, y2);
        graphics.fill(x, top, x + 1, bottom + 1, 0xFF8A8A8A);
    }
}
