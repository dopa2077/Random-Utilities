package com.dopa.randomutilities.compat.jei;

import com.dopa.randomutilities.config.GeneratorOutputMode;
import com.dopa.randomutilities.config.GeneratorRecipe;
import com.dopa.randomutilities.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.config.GeneratorResource;
import com.dopa.randomutilities.config.GeneratorType;
import com.dopa.randomutilities.dOPasRandomUtilities;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import com.dopa.randomutilities.filteritem.client.FilterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@JeiPlugin
public class dOPasJeiPlugin implements IModPlugin {
    private static final Identifier UID =
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(FilterScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(FilterScreen screen) {
                return screen.getPanelHost().collectExtraAreas(
                        screen.leftPos(),
                        screen.topPos(),
                        screen.imageWidth()
                );
            }
        });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        for (GeneratorType type : GeneratorType.values()) {
            registration.addRecipeCategories(new ResourceGeneratorRecipeCategory(guiHelper, type));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (GeneratorType type : GeneratorType.values()) {
            List<GeneratorJeiRecipe> recipes = new ArrayList<>();
            for (GeneratorRecipe recipe : GeneratorRecipeConfig.getRecipes(type)) {
                recipes.add(new GeneratorJeiRecipe(type, recipe));
            }
            registration.addRecipes(ResourceGeneratorRecipeCategory.recipeType(type), recipes);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (GeneratorType type : GeneratorType.values()) {
            ItemStack stack = generatorStack(type);
            if (!stack.isEmpty()) {
                registration.addCraftingStation(ResourceGeneratorRecipeCategory.recipeType(type), stack);
            }
        }
    }

    private static ItemStack generatorStack(GeneratorType type) {
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, type.id())
        );
        return block.asItem() == Items.AIR ? ItemStack.EMPTY : new ItemStack(block.asItem());
    }

    public record GeneratorJeiRecipe(GeneratorType type, GeneratorRecipe recipe) {
        public Identifier recipeId() {
            return Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, type.id() + "/" + recipe.id());
        }

        public ItemStack generatorStack() {
            return dOPasJeiPlugin.generatorStack(type);
        }

        public List<SideIngredient> sideIngredients() {
            List<SideIngredient> sides = new ArrayList<>(4);
            for (int i = 0; i < GeneratorRecipe.SIDE_COUNT; i++) {
                GeneratorResource resource = recipe.resources().get(i);
                if (resource != null) {
                    sides.add(new SideIngredient(resource, recipe.consume()[i]));
                }
            }
            return sides;
        }

        public boolean isInsertOutput() {
            return recipe.outputMode() == GeneratorOutputMode.INSERT || recipe.isFluidResult();
        }

        public boolean isDropOutput() {
            return !isInsertOutput() && recipe.outputMode() == GeneratorOutputMode.DROP;
        }

        public boolean isPlaceOutput() {
            return !isInsertOutput() && recipe.outputMode() == GeneratorOutputMode.PLACE;
        }

        public int resultFluidMillibuckets() {
            long millibuckets = (long) recipe.amount() * FluidType.BUCKET_VOLUME;
            return millibuckets > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millibuckets;
        }

        public List<ItemStack> resultStacks() {
            if (recipe.isFluidResult()) {
                return List.of();
            }
            if (!recipe.isRandomResult()) {
                Block result = recipe.result();
                return result == null || result.asItem() == Items.AIR
                        ? List.of()
                        : List.of(new ItemStack(result.asItem(), recipe.amount()));
            }
            List<Block> pool = switch (type.mode()) {
                case RANDOM_ORE -> GeneratorRecipeConfig.ores();
                case METAL_BLOCK -> GeneratorRecipeConfig.metalBlocks();
                case RECIPE -> List.of();
            };
            List<ItemStack> stacks = new ArrayList<>(pool.size());
            for (Block block : pool) {
                if (block.asItem() != Items.AIR) {
                    stacks.add(new ItemStack(block.asItem(), recipe.amount()));
                }
            }
            return stacks;
        }

        public record SideIngredient(GeneratorResource resource, boolean consume) {}
    }

    public static final class ResourceGeneratorRecipeCategory extends AbstractRecipeCategory<GeneratorJeiRecipe> {
        private static final Map<GeneratorType, IRecipeType<GeneratorJeiRecipe>> RECIPE_TYPES =
                new EnumMap<>(GeneratorType.class);

        static {
            for (GeneratorType type : GeneratorType.values()) {
                RECIPE_TYPES.put(type, IRecipeType.create(dOPasRandomUtilities.MOD_ID, type.id(), GeneratorJeiRecipe.class));
            }
        }

        public static IRecipeType<GeneratorJeiRecipe> recipeType(GeneratorType type) {
            return RECIPE_TYPES.get(type);
        }

        public static final int WIDTH = 152;
        public static final int HEIGHT = 112;

        private static final int GEN_X = 68;
        private static final int GEN_Y = 52;
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
        private static final float HINT_Y = BELOW_Y + 22.0F;
        private static final int COLOR_KEEP = 0x7DCEA0;
        private static final int COLOR_CONSUME = 0xFF6B6B;
        private static final int COLOR_RATE = 0xF0C75E;

        private static final List<ItemStack> OUTPUT_CONTAINERS = List.of(
                new ItemStack(Items.CHEST), new ItemStack(Items.BARREL), new ItemStack(Items.HOPPER),
                new ItemStack(Items.TRAPPED_CHEST), new ItemStack(Items.SHULKER_BOX)
        );
        private static final List<ItemStack> OUTPUT_TANKS = List.of(
                new ItemStack(Items.CAULDRON), new ItemStack(Items.BUCKET),
                new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.LAVA_BUCKET)
        );

        private final IDrawable consumeMarker;
        private final IDrawableStatic recipeArrow;

        public ResourceGeneratorRecipeCategory(IGuiHelper guiHelper, GeneratorType type) {
            super(
                    recipeType(type),
                    Component.translatable("block." + dOPasRandomUtilities.MOD_ID + "." + type.id()),
                    guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, generatorStack(type)),
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
                IRecipeSlotBuilder slot = builder.addInputSlot(sidePositions[i][0], sidePositions[i][1])
                        .setStandardSlotBackground()
                        .setSlotName("side_" + i)
                        .setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16);
                if (side.resource().isFluid()) {
                    Fluid fluid = side.resource().fluid();
                    if (fluid != null) {
                        slot.add(fluid, FluidType.BUCKET_VOLUME);
                    }
                } else {
                    Block block = side.resource().block();
                    if (block != null && block.asItem() != Items.AIR) {
                        slot.add(new ItemStack(block.asItem()));
                    }
                }
                if (side.consume()) {
                    slot.setOverlay(consumeMarker, 10, 10);
                    slot.addRichTooltipCallback((slotView, tooltip) -> tooltip.add(sideTooltip(
                            "jei.dopasrandomutilities.tooltip.side_prefix",
                            side.resource().isFluid()
                                    ? "jei.dopasrandomutilities.tooltip.side_consume_fluid"
                                    : "jei.dopasrandomutilities.tooltip.side_consume_block",
                            COLOR_CONSUME
                    )));
                } else {
                    slot.addRichTooltipCallback((slotView, tooltip) -> tooltip.add(sideTooltip(
                            "jei.dopasrandomutilities.tooltip.side_prefix",
                            "jei.dopasrandomutilities.tooltip.side_keep",
                            COLOR_KEEP
                    )));
                }
            }

            Block below = recipe.recipe().requiredUnder();
            if (below != null && below.asItem() != Items.AIR) {
                builder.addInputSlot(BELOW_X, BELOW_Y)
                        .setStandardSlotBackground()
                        .add(new ItemStack(below.asItem()))
                        .setSlotName("below")
                        .addRichTooltipCallback((slotView, tooltip) ->
                                tooltip.add(Component.translatable("jei.dopasrandomutilities.tooltip.below")));
            }

            if (recipe.isInsertOutput()) {
                boolean fluidResult = recipe.recipe().isFluidResult();
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, CONTAINER_X, CONTAINER_Y)
                        .setStandardSlotBackground()
                        .addItemStacks(fluidResult ? OUTPUT_TANKS : OUTPUT_CONTAINERS)
                        .setSlotName("container")
                        .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(Component.translatable(
                                fluidResult
                                        ? "jei.dopasrandomutilities.tooltip.tank"
                                        : "jei.dopasrandomutilities.tooltip.container"
                        )));
            }

            GeneratorRecipe data = recipe.recipe();
            int resultY = recipe.isInsertOutput() ? OUTPUT_Y : CONTAINER_Y;
            IRecipeSlotBuilder output = builder.addOutputSlot(OUTPUT_X, resultY)
                    .setStandardSlotBackground()
                    .setSlotName("result");
            if (recipe.recipe().isFluidResult()) {
                Fluid fluid = recipe.recipe().resultFluid();
                int millibuckets = recipe.resultFluidMillibuckets();
                output.setFluidRenderer(millibuckets, false, 16, 16);
                if (fluid != null) {
                    output.add(fluid, millibuckets);
                }
                output.addRichTooltipCallback((slotView, tooltip) -> {
                    tooltip.add(Component.translatable("jei.dopasrandomutilities.tooltip.output_insert_fluid"));
                    tooltip.add(rateLine(data.amount(), data.ticks()));
                });
            } else {
                List<ItemStack> results = recipe.resultStacks();
                if (!results.isEmpty()) {
                    output.addItemStacks(results);
                }
                String outputTooltipKey = recipe.isInsertOutput()
                        ? "jei.dopasrandomutilities.tooltip.output_insert"
                        : recipe.isPlaceOutput()
                                ? "jei.dopasrandomutilities.tooltip.output_place"
                                : "jei.dopasrandomutilities.tooltip.output_drop";
                output.addRichTooltipCallback((slotView, tooltip) -> {
                    tooltip.add(Component.translatable(outputTooltipKey));
                    tooltip.add(rateLine(data.amount(), data.ticks()));
                });
            }
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, GeneratorJeiRecipe recipe, IFocusGroup focuses) {}

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
                drawV(guiGraphics, centerX, CONTAINER_Y + 18, OUTPUT_Y - 1);
            }
            drawUpArrow(guiGraphics, centerX, resultY + 18, GEN_Y);
            if (recipe.recipe().requiredUnder() != null) {
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
            var pose = guiGraphics.pose();
            pose.pushMatrix();
            pose.translate(centerX, midY);
            pose.rotate(-(float) (Math.PI / 2.0));
            pose.scale(ARROW_SCALE, ARROW_SCALE);
            pose.translate(-recipeArrow.getWidth() / 2.0F, -recipeArrow.getHeight() / 2.0F);
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
                case 1 -> new int[][] {{GEN_X - SIDE_OFFSET_X, GEN_Y}};
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
            IDrawable barrier = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.BARRIER));
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

        private static void drawSideLinks(GuiGraphicsExtractor graphics, int[][] positions, int genCenterX, int spineY) {
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
            graphics.fill(Math.min(x1, x2), y, Math.max(x1, x2) + 1, y + 1, 0xFF8A8A8A);
        }

        private static void drawV(GuiGraphicsExtractor graphics, int x, int y1, int y2) {
            graphics.fill(x, Math.min(y1, y2), x + 1, Math.max(y1, y2) + 1, 0xFF8A8A8A);
        }
    }
}
