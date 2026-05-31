package com.ysh.serverbot.network;

import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundDebugSubscriptionRequestPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetGameRulePacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetTestBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSpectateEntityPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundTestInstanceBlockActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class BotNetworkHandler extends ServerGamePacketListenerImpl {

    public BotNetworkHandler(MinecraftServer server, ServerPlayer player, Connection connection) {
        super(server, connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
    }

    @Override
    public void tick() {
    }

    @Override
    public void disconnect(Component message) {
    }

    @Override
    public void disconnect(DisconnectionDetails details) {
    }

    @Override
    public void onDisconnect(DisconnectionDetails reason) {
    }

    @Override
    public void handlePlayerInput(ServerboundPlayerInputPacket packet) {
    }

    @Override
    public void handleMoveVehicle(ServerboundMoveVehiclePacket packet) {
    }

    @Override
    public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket packet) {
    }

    @Override
    public void handleAcceptPlayerLoad(ServerboundPlayerLoadedPacket packet) {
    }

    @Override
    public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket packet) {
    }

    @Override
    public void handleBundleItemSelectedPacket(ServerboundSelectBundleItemPacket packet) {
    }

    @Override
    public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket packet) {
    }

    @Override
    public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket packet) {
    }

    @Override
    public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket packet) {
    }

    @Override
    public void handleSetCommandBlock(ServerboundSetCommandBlockPacket packet) {
    }

    @Override
    public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket packet) {
    }

    @Override
    public void handlePickItemFromBlock(ServerboundPickItemFromBlockPacket packet) {
    }

    @Override
    public void handlePickItemFromEntity(ServerboundPickItemFromEntityPacket packet) {
    }

    @Override
    public void handleRenameItem(ServerboundRenameItemPacket packet) {
    }

    @Override
    public void handleSetBeaconPacket(ServerboundSetBeaconPacket packet) {
    }

    @Override
    public void handleSetGameRule(ServerboundSetGameRulePacket packet) {
    }

    @Override
    public void handleSetStructureBlock(ServerboundSetStructureBlockPacket packet) {
    }

    @Override
    public void handleSetTestBlock(ServerboundSetTestBlockPacket packet) {
    }

    @Override
    public void handleTestInstanceBlockAction(ServerboundTestInstanceBlockActionPacket packet) {
    }

    @Override
    public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket packet) {
    }

    @Override
    public void handleJigsawGenerate(ServerboundJigsawGeneratePacket packet) {
    }

    @Override
    public void handleSelectTrade(ServerboundSelectTradePacket packet) {
    }

    @Override
    public void handleEditBook(ServerboundEditBookPacket packet) {
    }

    @Override
    public void handleEntityTagQuery(ServerboundEntityTagQueryPacket packet) {
    }

    @Override
    public void handleContainerSlotStateChanged(ServerboundContainerSlotStateChangedPacket packet) {
    }

    @Override
    public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQueryPacket packet) {
    }

    @Override
    public void handleMovePlayer(ServerboundMovePlayerPacket packet) {
    }

    @Override
    public void handlePlayerAction(ServerboundPlayerActionPacket packet) {
    }

    @Override
    public void handleUseItemOn(ServerboundUseItemOnPacket packet) {
    }

    @Override
    public void handleUseItem(ServerboundUseItemPacket packet) {
    }

    @Override
    public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket packet) {
    }

    @Override
    public void handlePaddleBoat(ServerboundPaddleBoatPacket packet) {
    }

    @Override
    public void handleSetCarriedItem(ServerboundSetCarriedItemPacket packet) {
    }

    @Override
    public void handleChat(ServerboundChatPacket packet) {
    }

    @Override
    public void handleChatCommand(ServerboundChatCommandPacket packet) {
    }

    @Override
    public void handleSignedChatCommand(ServerboundChatCommandSignedPacket packet) {
    }

    @Override
    public void handleChatAck(ServerboundChatAckPacket packet) {
    }

    @Override
    public void handleAnimate(ServerboundSwingPacket packet) {
    }

    @Override
    public void handlePlayerCommand(ServerboundPlayerCommandPacket packet) {
    }

    @Override
    public void handlePingRequest(ServerboundPingRequestPacket packet) {
    }

    @Override
    public void handleKeepAlive(ServerboundKeepAlivePacket packet) {
    }

    @Override
    public void handleAttack(ServerboundAttackPacket packet) {
    }

    @Override
    public void handleInteract(ServerboundInteractPacket packet) {
    }

    @Override
    public void handleSpectateEntity(ServerboundSpectateEntityPacket packet) {
    }

    @Override
    public void handleClientCommand(ServerboundClientCommandPacket packet) {
    }

    @Override
    public void handleContainerClose(ServerboundContainerClosePacket packet) {
    }

    @Override
    public void handleContainerClick(ServerboundContainerClickPacket packet) {
    }

    @Override
    public void handlePlaceRecipe(ServerboundPlaceRecipePacket packet) {
    }

    @Override
    public void handleContainerButtonClick(ServerboundContainerButtonClickPacket packet) {
    }

    @Override
    public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet) {
    }

    @Override
    public void handleSignUpdate(ServerboundSignUpdatePacket packet) {
    }

    @Override
    public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet) {
    }

    @Override
    public void handleClientInformation(ServerboundClientInformationPacket packet) {
    }

    @Override
    public void handleChangeDifficulty(ServerboundChangeDifficultyPacket packet) {
    }

    @Override
    public void handleChangeGameMode(ServerboundChangeGameModePacket packet) {
    }

    @Override
    public void handleLockDifficulty(ServerboundLockDifficultyPacket packet) {
    }

    @Override
    public void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket packet) {
    }

    @Override
    public void handleConfigurationAcknowledged(ServerboundConfigurationAcknowledgedPacket packet) {
    }

    @Override
    public void handleChunkBatchReceived(ServerboundChunkBatchReceivedPacket packet) {
    }

    @Override
    public void handleDebugSubscriptionRequest(ServerboundDebugSubscriptionRequestPacket packet) {
    }

    @Override
    public void handleCustomPayload(ServerboundCustomPayloadPacket packet) {
    }

    @Override
    public void handleCookieResponse(ServerboundCookieResponsePacket packet) {
    }

    @Override
    public void handleClientTickEnd(ServerboundClientTickEndPacket packet) {
    }
}
