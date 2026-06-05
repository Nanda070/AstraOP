package com.astraor.commands;

import com.astraor.AstraORPlugin;
import com.astraor.state.KeybindManager.AbilityKeybind;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class AstraKeybindCommand {

    public static void register(AstraORPlugin plugin) {
        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            LiteralCommandNode<CommandSourceStack> node = Commands.literal("abilitykeybind")
                .executes(context -> {
                    if (!(context.getSource().getSender() instanceof Player player)) {
                        context.getSource().getSender().sendMessage(Component.text("§cТолько для игроков."));
                        return 0;
                    }
                    AbilityKeybind current = plugin.getKeybindManager().getPlayerKeybind(player);
                    player.sendMessage(Component.text("§aВаш текущий бинд для способностей: §e" + current.getDisplayName()));
                    player.sendMessage(Component.text("§7Чтобы изменить, используйте: §e/abilitykeybind set <bind>"));
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("set")
                    .then(Commands.argument("bind", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (AbilityKeybind kb : AbilityKeybind.values()) {
                                builder.suggest(kb.name());
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            if (!(context.getSource().getSender() instanceof Player player)) {
                                context.getSource().getSender().sendMessage(Component.text("§cТолько для игроков."));
                                return 0;
                            }
                            
                            String bindName = StringArgumentType.getString(context, "bind").toUpperCase();
                            try {
                                AbilityKeybind newBind = AbilityKeybind.valueOf(bindName);
                                plugin.getKeybindManager().setPlayerKeybind(player, newBind);
                                player.sendMessage(Component.text("§aБинд для способностей изменен на: §e" + newBind.getDisplayName()));
                                return Command.SINGLE_SUCCESS;
                            } catch (IllegalArgumentException e) {
                                String valid = Arrays.stream(AbilityKeybind.values()).map(Enum::name).collect(Collectors.joining(", "));
                                player.sendMessage(Component.text("§cНеверный бинд. Доступные: " + valid));
                                return 0;
                            }
                        })
                    )
                )
                .build();

            commands.register(node, "Сменить бинд способностей");
        });
    }
}
