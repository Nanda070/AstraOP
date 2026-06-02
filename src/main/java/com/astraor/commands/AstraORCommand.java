package com.astraor.commands;

import com.astraor.AstraORPlugin;
import com.astraor.gui.OnboardingGUI;
import com.astraor.state.AstraClass;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class AstraORCommand {

    public static void register(AstraORPlugin plugin) {
        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            LiteralCommandNode<CommandSourceStack> node = Commands.literal("astraor")
                .requires(source -> source.getSender().hasPermission("astraor.admin"))
                
                // 1. spawnnpc subcommand
                .then(Commands.literal("spawnnpc")
                    .executes(context -> {
                        if (!(context.getSource().getSender() instanceof Player admin)) {
                            context.getSource().getSender().sendMessage(Component.text("§cКоманду может использовать только игрок."));
                            return 0;
                        }

                        Villager npc = (Villager) admin.getWorld().spawnEntity(admin.getLocation(), EntityType.VILLAGER);
                        npc.setAI(false);
                        npc.setInvulnerable(true);
                        npc.setSilent(true);
                        npc.setGravity(false); // keep them perfectly in place
                        npc.customName(Component.text("§6§lTemple Guide"));
                        npc.setCustomNameVisible(true);
                        npc.setVillagerType(Villager.Type.SNOW);
                        npc.setProfession(Villager.Profession.CLERIC);
                        
                        NamespacedKey npcKey = new NamespacedKey(plugin, "temple_npc");
                        npc.getPersistentDataContainer().set(npcKey, PersistentDataType.BYTE, (byte) 1);

                        admin.sendMessage(Component.text("§aNPC Хранитель Храма заспавнен на вашей позиции."));
                        return Command.SINGLE_SUCCESS;
                    })
                )
                
                // 1.5 despawnnpc subcommand
                .then(Commands.literal("despawnnpc")
                    .executes(context -> {
                        if (!(context.getSource().getSender() instanceof Player admin)) {
                            context.getSource().getSender().sendMessage(Component.text("§cOnly players can run this."));
                            return 0;
                        }

                        NamespacedKey npcKey = new NamespacedKey(plugin, "temple_npc");
                        int count = 0;
                        for (Entity entity : admin.getNearbyEntities(5.0, 5.0, 5.0)) {
                            if (entity.getPersistentDataContainer().has(npcKey, PersistentDataType.BYTE)) {
                                entity.remove();
                                count++;
                            }
                        }

                        if (count > 0) {
                            admin.sendMessage(Component.text("§aУдалено " + count + " NPC поблизости."));
                        } else {
                            admin.sendMessage(Component.text("§cВ радиусе 5 блоков NPC не найдены."));
                        }
                        
                        return Command.SINGLE_SUCCESS;
                    })
                )
                
                // 2. reset subcommand
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String input = builder.getRemainingLowerCase();
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (p.getName().toLowerCase().startsWith(input)) {
                                    builder.suggest(p.getName());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String targetName = StringArgumentType.getString(context, "player");
                            Player target = Bukkit.getPlayer(targetName);
                            if (target == null) {
                                context.getSource().getSender().sendMessage(Component.text("§cИгрок не найден."));
                                return 0;
                            }

                            plugin.getClassManager().clearClass(target);
                            plugin.getClassManager().setClass(target, null);
                            plugin.getClassManager().setChoosingClass(target, true);
                            
                            OnboardingGUI.openMainGUI(target);
                            
                            context.getSource().getSender().sendMessage(Component.text("§aУспешно сброшен класс для " + target.getName() + " и открыто меню."));
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                )
                
                // 3. set subcommand
                .then(Commands.literal("set")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String input = builder.getRemainingLowerCase();
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (p.getName().toLowerCase().startsWith(input)) {
                                    builder.suggest(p.getName());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("class", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                String input = builder.getRemainingLowerCase();
                                for (AstraClass c : AstraClass.values()) {
                                    if (c.getId().toLowerCase().startsWith(input)) {
                                        builder.suggest(c.getId());
                                    }
                                }
                                return builder.buildFuture();
                            })
                            .executes(context -> {
                                String targetName = StringArgumentType.getString(context, "player");
                                Player target = Bukkit.getPlayer(targetName);
                                if (target == null) {
                                    context.getSource().getSender().sendMessage(Component.text("§cИгрок не найден."));
                                    return 0;
                                }

                                String className = StringArgumentType.getString(context, "class");
                                AstraClass astraClass = AstraClass.fromId(className);
                                if (astraClass == null) {
                                    String valid = Arrays.stream(AstraClass.values()).map(AstraClass::getId).collect(Collectors.joining(", "));
                                    context.getSource().getSender().sendMessage(Component.text("§cНеверный класс. Доступные: " + valid));
                                    return 0;
                                }

                                plugin.getClassManager().setChoosingClass(target, false);
                                plugin.getClassManager().setClass(target, astraClass);
                                plugin.getMechanicsManager().applyClass(target, astraClass);

                                context.getSource().getSender().sendMessage(Component.text("§aВы принудительно установили класс " + astraClass.getId() + " для игрока " + target.getName()));
                                target.sendMessage(Component.text("§aАдминистратор установил вам класс " + astraClass.getId()));
                                
                                return Command.SINGLE_SUCCESS;
                            })
                        )
                    )
                )
                .build();

            commands.register(node, "AstraOR Admin Command");
        });
    }
}
