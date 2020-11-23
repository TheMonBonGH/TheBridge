package plugily.projects.thebridge.events;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import pl.plajerlair.commonsbox.minecraft.compat.XMaterial;
import plugily.projects.thebridge.ConfigPreferences;
import plugily.projects.thebridge.Main;
import plugily.projects.thebridge.arena.Arena;
import plugily.projects.thebridge.arena.ArenaManager;
import plugily.projects.thebridge.arena.ArenaRegistry;
import plugily.projects.thebridge.handlers.items.SpecialItemManager;
import plugily.projects.thebridge.utils.Utils;

/**
 * @author Tigerpanzer_02
 * <p>
 * Created at 23.11.2020
 */
public class Events implements Listener {

  private final Main plugin;

  public Events(Main plugin) {
    this.plugin = plugin;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  public void onItemSwap(PlayerSwapHandItemsEvent e) {
    if (ArenaRegistry.isInArena(e.getPlayer())) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onDrop(PlayerDropItemEvent event) {
    if (ArenaRegistry.isInArena(event.getPlayer())) {
      event.setCancelled(true);
    }
  }


  @EventHandler(priority = EventPriority.HIGHEST)
  public void onCommandExecute(PlayerCommandPreprocessEvent event) {
    Arena arena = ArenaRegistry.getArena(event.getPlayer());
    if (arena == null) {
      return;
    }
    if (!plugin.getConfig().getBoolean("Block-Commands-In-Game", true)) {
      return;
    }
    String command = event.getMessage().substring(1);
    command = (command.indexOf(' ') >= 0 ? command.substring(0, command.indexOf(' ')) : command);
    for (String msg : plugin.getConfig().getStringList("Whitelisted-Commands")) {
      if (command.equalsIgnoreCase(msg)) {
        return;
      }
    }
    if (event.getPlayer().isOp() || event.getPlayer().hasPermission("thebridge.admin") || event.getPlayer().hasPermission("thebridge.command.bypass")) {
      return;
    }
    if (command.equalsIgnoreCase("mm") || command.equalsIgnoreCase("thebridge")
      || event.getMessage().contains("thebridgeadmin") || event.getMessage().contains("leave")
      || command.equalsIgnoreCase("stats") || command.equalsIgnoreCase("mma")) {
      return;
    }
    event.setCancelled(true);
    event.getPlayer().sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Only-Command-Ingame-Is-Leave"));
  }

  @EventHandler
  public void onInGameInteract(PlayerInteractEvent event) {
    Arena arena = ArenaRegistry.getArena(event.getPlayer());
    if (arena == null || event.getClickedBlock() == null) {
      return;
    }
    if (event.getClickedBlock().getType() == XMaterial.PAINTING.parseMaterial() || event.getClickedBlock().getType() == XMaterial.FLOWER_POT.parseMaterial()) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onInGameBedEnter(PlayerBedEnterEvent event) {
    if (ArenaRegistry.isInArena(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onLeave(PlayerInteractEvent event) {
    if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL) {
      return;
    }
    Arena arena = ArenaRegistry.getArena(event.getPlayer());
    ItemStack itemStack = event.getPlayer().getInventory().getItemInMainHand();
    if (arena == null || !Utils.isNamed(itemStack)) {
      return;
    }
    String key = SpecialItemManager.getRelatedSpecialItem(itemStack);
    if (key == null) {
      return;
    }
    if (SpecialItemManager.getRelatedSpecialItem(itemStack).equalsIgnoreCase("Leave")) {
      event.setCancelled(true);
      if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
        plugin.getBungeeManager().connectToHub(event.getPlayer());
      } else {
        ArenaManager.leaveAttempt(event.getPlayer(), arena);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onFoodLevelChange(FoodLevelChangeEvent event) {
    if (event.getEntity().getType() == EntityType.PLAYER && ArenaRegistry.isInArena((Player) event.getEntity())) {
      event.setFoodLevel(20);
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  //highest priority to fully protect our game
  public void onBlockBreakEvent(BlockBreakEvent event) {
    if (ArenaRegistry.isInArena(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  //highest priority to fully protect our game
  public void onBuild(BlockPlaceEvent event) {
    if (ArenaRegistry.isInArena(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  //highest priority to fully protect our game
  public void onHangingBreakEvent(HangingBreakByEntityEvent event) {
    if (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof Painting) {
      if (event.getRemover() instanceof Player && ArenaRegistry.isInArena((Player) event.getRemover())) {
        event.setCancelled(true);
        return;
      }
      if (!(event.getRemover() instanceof Arrow)) {
        return;
      }
      Arrow arrow = (Arrow) event.getRemover();
      if (arrow.getShooter() instanceof Player && ArenaRegistry.isInArena((Player) arrow.getShooter())) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onArmorStandDestroy(EntityDamageByEntityEvent e) {
    if (!(e.getEntity() instanceof LivingEntity)) {
      return;
    }
    LivingEntity livingEntity = (LivingEntity) e.getEntity();
    if (livingEntity.getType() != EntityType.ARMOR_STAND) {
      return;
    }
    if (e.getDamager() instanceof Player && ArenaRegistry.isInArena((Player) e.getDamager())) {
      e.setCancelled(true);
    } else if (e.getDamager() instanceof Arrow) {
      Arrow arrow = (Arrow) e.getDamager();
      if (arrow.getShooter() instanceof Player && ArenaRegistry.isInArena((Player) arrow.getShooter())) {
        e.setCancelled(true);
        return;
      }
      e.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onInteractWithArmorStand(PlayerArmorStandManipulateEvent event) {
    if (ArenaRegistry.isInArena(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onCraft(PlayerInteractEvent event) {
    if (!ArenaRegistry.isInArena(event.getPlayer())) {
      return;
    }
    if (event.getPlayer().getTargetBlock(null, 7).getType() == XMaterial.CRAFTING_TABLE.parseMaterial()) {
      event.setCancelled(true);
    }
  }

}