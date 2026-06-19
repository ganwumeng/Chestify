package com.ganwumeng.chestify;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public final class Chestify implements ModInitializer {
    public static final String MOD_ID = "chestify";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static int searchRadius() {
        return ChestifyConfig.searchRadius();
    }

    private static double maxOpenDistance() {
        return ChestifyConfig.searchRadius() + 2.0D;
    }

    @Override
    public void onInitialize() {
        ChestifyConfig.load();
        PayloadTypeRegistry.serverboundPlay().register(OpenChestPayload.ID, OpenChestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetRadiusPayload.ID, SetRadiusPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RadiusSyncPayload.ID, RadiusSyncPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OpenChestPayload.ID, (payload, context) ->
                context.server().execute(() -> openMarkedChest(context.player(), payload.pos())));
        ServerPlayNetworking.registerGlobalReceiver(SetRadiusPayload.ID, (payload, context) ->
                context.server().execute(() -> updateRadius(context.player(), payload.radius())));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> syncRadius(handler.player)));
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> interactWithFrame(player, world, entity));
    }

    private static InteractionResult interactWithFrame(net.minecraft.world.entity.player.Player player, Level world, Entity entity) {
        if (player.isSecondaryUseActive() || !(entity instanceof ItemFrame frame) || frame.getItem().isEmpty()) {
            return InteractionResult.PASS;
        }

        BlockPos chestPos = findMarkedChestPos(world, frame);
        if (chestPos == null) {
            return InteractionResult.PASS;
        }

        if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            openMarkedChest(serverPlayer, chestPos);
        }

        return InteractionResult.SUCCESS;
    }

    private static void openMarkedChest(ServerPlayer player, BlockPos pos) {
        ServerLevel world = player.level();

        if (!player.blockPosition().closerThan(pos, maxOpenDistance())) {
            return;
        }

        if (!isChest(world.getBlockState(pos)) || !hasMarkerFrame(world, pos)) {
            return;
        }

        BlockState state = world.getBlockState(pos);
        MenuProvider factory = state.getMenuProvider(world, pos);
        Container container = ChestBlock.getContainer((ChestBlock) state.getBlock(), state, world, pos, false);
        if (factory != null && container != null) {
            player.openMenu(remoteChestMenu(factory.getDisplayName(), container, pos));
        }
    }

    private static MenuProvider remoteChestMenu(Component title, Container container, BlockPos pos) {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                int rows = Math.max(1, Math.min(6, (container.getContainerSize() + 8) / 9));
                return new ChestMenu(menuType(rows), syncId, playerInventory,
                        new RemoteChestContainer(container, pos), rows);
            }
        };
    }

    private static void updateRadius(ServerPlayer player, int radius) {
        if (!canEditConfig(player)) {
            syncRadius(player);
            return;
        }

        ChestifyConfig.setSearchRadius(radius);
        ChestifyConfig.save();
        for (ServerPlayer onlinePlayer : player.level().getServer().getPlayerList().getPlayers()) {
            syncRadius(onlinePlayer);
        }
    }

    private static void syncRadius(ServerPlayer player) {
        ServerPlayNetworking.send(player, new RadiusSyncPayload(ChestifyConfig.searchRadius(), canEditConfig(player)));
    }

    private static boolean canEditConfig(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server.isSingleplayerOwner(player.nameAndId())
                || server.getPlayerList().isOp(player.nameAndId());
    }

    private static MenuType<ChestMenu> menuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> MenuType.GENERIC_9x3;
        };
    }

    public static boolean isChest(BlockState state) {
        return state.getBlock() instanceof ChestBlock;
    }

    public static BlockPos findMarkedChestPos(Level world, ItemFrame frame) {
        BlockPos framePos = frame.getPos();
        BlockPos[] candidates = {
                framePos,
                framePos.relative(frame.getDirection()),
                framePos.relative(frame.getDirection().getOpposite())
        };

        for (BlockPos candidate : candidates) {
            if (isChest(world.getBlockState(candidate))) {
                return candidate.immutable();
            }
        }

        return null;
    }

    private static boolean hasMarkerFrame(ServerLevel world, BlockPos pos) {
        AABB searchBox = new AABB(pos).inflate(1.0D);
        return !world.getEntities(EntityTypeTest.forClass(ItemFrame.class), searchBox, frame ->
                pos.equals(findMarkedChestPos(world, frame)) && !frame.getItem().isEmpty()).isEmpty();
    }

    private record RemoteChestContainer(Container delegate, BlockPos pos) implements Container {
        @Override
        public int getContainerSize() {
            return delegate.getContainerSize();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return delegate.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return delegate.removeItem(slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return delegate.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            delegate.setItem(slot, stack);
        }

        @Override
        public void setChanged() {
            delegate.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return player.level() instanceof ServerLevel world
                    && player.blockPosition().closerThan(pos, maxOpenDistance())
                    && isChest(world.getBlockState(pos))
                    && hasMarkerFrame(world, pos);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return delegate.canPlaceItem(slot, stack);
        }

        @Override
        public boolean canTakeItem(Container target, int slot, ItemStack stack) {
            return delegate.canTakeItem(target, slot, stack);
        }

        @Override
        public int countItem(Item item) {
            return delegate.countItem(item);
        }

        @Override
        public boolean hasAnyOf(java.util.Set<Item> items) {
            return delegate.hasAnyOf(items);
        }

        @Override
        public boolean hasAnyMatching(java.util.function.Predicate<ItemStack> predicate) {
            return delegate.hasAnyMatching(predicate);
        }

        @Override
        public void clearContent() {
            delegate.clearContent();
        }
    }
}
