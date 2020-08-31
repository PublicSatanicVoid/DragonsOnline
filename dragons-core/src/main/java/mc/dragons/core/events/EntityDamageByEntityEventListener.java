package mc.dragons.core.events;

import java.util.Set;
import java.util.logging.Logger;
import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.loader.ItemLoader;
import mc.dragons.core.gameobject.loader.NPCLoader;
import mc.dragons.core.gameobject.loader.RegionLoader;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCConditionalActions;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.user.SkillType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.HologramUtil;
import mc.dragons.core.util.MathUtil;
import mc.dragons.core.util.ProgressBarUtil;
import mc.dragons.core.util.StringUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class EntityDamageByEntityEventListener implements Listener {
	private Logger LOGGER = Dragons.getInstance().getLogger();

	private RegionLoader regionLoader;

	private Dragons plugin;

	public EntityDamageByEntityEventListener(Dragons instance) {
		this.regionLoader = GameObjectType.REGION.<Region, RegionLoader>getLoader();
		this.plugin = instance;
	}

	@EventHandler
	public void onEntityDamage(EntityDamageByEntityEvent event) {
		this.LOGGER.finer("Damage event on " + StringUtil.entityToString(event.getEntity()) + " by " + StringUtil.entityToString(event.getDamager()));
		Entity damager = event.getDamager();
		User userDamager = null;
		NPC npcDamager = null;
		if (damager instanceof Player) {
			userDamager = UserLoader.fromPlayer((Player) damager);
		} else if (damager instanceof Arrow) {
			Arrow arrow = (Arrow) damager;
			if (arrow.getShooter() instanceof Entity)
				npcDamager = NPCLoader.fromBukkit((Entity) arrow.getShooter());
		} else {
			npcDamager = NPCLoader.fromBukkit(damager);
		}
		if (npcDamager != null && npcDamager.getNPCType() != NPC.NPCType.HOSTILE) {
			event.setCancelled(true);
			return;
		}
		boolean external = false;
		Entity target = event.getEntity();
		User userTarget = null;
		NPC npcTarget = null;
		if (target instanceof Player) {
			userTarget = UserLoader.fromPlayer((Player) target);
		} else if (target instanceof org.bukkit.entity.ArmorStand) {
			if (target.hasMetadata("partOf")) {
				this.LOGGER.finer("-Target is part of a complex entity!");
				npcTarget = (NPC) ((MetadataValue) target.getMetadata("partOf").get(0)).value();
				external = true;
			}
		} else {
			npcTarget = NPCLoader.fromBukkit(target);
			if (npcTarget != null) {
				if (npcTarget.isImmortal() || target.isInvulnerable()) {
					event.setCancelled(true);
					if (userDamager != null) {
						Item item = ItemLoader.fromBukkit(userDamager.getPlayer().getInventory().getItemInMainHand());
						if (item != null && item.getClassName().equals("Special:ImmortalOverride")) {
							npcTarget.getEntity().remove();
							this.plugin.getGameObjectRegistry().removeFromDatabase((GameObject) npcTarget);
							userDamager.getPlayer().sendMessage(ChatColor.GREEN + "Removed NPC successfully.");
							return;
						}
						immortalTarget(target, userDamager);
					}
					npcTarget.updateHealthBar();
					return;
				}
			} else {
				this.LOGGER.finer("-ERROR: TARGET IS AN ENTITY BUT NOT AN NPC!!!! HasHandle=" + target.hasMetadata("handle"));
			}
		}
		if (userDamager != null && npcTarget != null && userDamager.hasActiveDialogue()) {
			userDamager.sendActionBar(ChatColor.GRAY + "PVE is disabled during quest dialogue!");
			event.setCancelled(true);
			return;
		}
		if (npcDamager != null && userTarget != null && userTarget.hasActiveDialogue()) {
			event.setCancelled(true);
			return;
		}
		double distance = damager.getLocation().distance(target.getLocation());
		double damage = event.getDamage();
		if ((userDamager == null && npcDamager == null) || (userTarget == null && npcTarget == null) || (npcDamager != null && npcTarget != null))
			return;
		if (userDamager != null && userDamager.isGodMode()) {
			if (npcTarget != null) {
				npcTarget.remove();
			} else if (!(target instanceof Player)) {
				target.remove();
			} else {
				((Player) target).setHealth(0.0D);
			}
			return;
		}
		if (userTarget != null && userTarget.isGodMode()) {
			immortalTarget(target, userDamager);
			event.setCancelled(true);
			return;
		}
		Set<Region> regions = this.regionLoader.getRegionsByLocation(target.getLocation());
		if (userTarget != null) {
			userTarget.debug("user target");
			for (Region region : regions) {
				if (!Boolean.valueOf(region.getFlags().getString("pve")).booleanValue()) {
					userTarget.debug("- Cancelled damage due to a region " + region.getName() + " PVE flag = false");
					event.setCancelled(true);
					return;
				}
			}
		}
		if (npcDamager != null) {
			double weightedLevelDiscrepancy = Math.max(0.0D, npcDamager.getLevel() - 0.3D * userTarget.getLevel());
			damage += 0.25D * weightedLevelDiscrepancy;
		} else {
			final Item attackerHeldItem = ItemLoader.fromBukkit(userDamager.getPlayer().getInventory().getItemInMainHand());
			double itemDamage = 0.5D;
			if (attackerHeldItem != null) {
				if (attackerHeldItem.hasCooldownRemaining()) {
					userDamager.sendActionBar(ChatColor.RED + "- WAIT - " + MathUtil.round(attackerHeldItem.getCooldownRemaining()) + "s -");
					event.setCancelled(true);
					return;
				}
				if (!external)
					attackerHeldItem.registerUse();
				final User fUserDamager = userDamager;
				(new BukkitRunnable() {
					public void run() {
						Item currentHeldItem = ItemLoader.fromBukkit(fUserDamager.getPlayer().getInventory().getItemInMainHand());
						if (currentHeldItem == null)
							return;
						if (!currentHeldItem.equals(attackerHeldItem))
							return;
						double percentRemaining = attackerHeldItem.getCooldownRemaining() / attackerHeldItem.getCooldown();
						String cooldownName = String.valueOf(attackerHeldItem.getDecoratedName()) + ChatColor.DARK_GRAY + " [" + ChatColor.RESET + "Recharging "
								+ ProgressBarUtil.getCountdownBar(percentRemaining) + ChatColor.DARK_GRAY + "]";
						fUserDamager.getPlayer().getInventory().setItemInMainHand(attackerHeldItem.localRename(cooldownName));
						if (!attackerHeldItem.hasCooldownRemaining()) {
							fUserDamager.getPlayer().getInventory().setItemInMainHand(attackerHeldItem.localRename(attackerHeldItem.getDecoratedName()));
							cancel();
						}
					}
				}).runTaskTimer((Plugin) this.plugin, 0L, 5L);
				itemDamage = attackerHeldItem.getDamage();
				damage += itemDamage;
			}
			if (userTarget == null) {
				for (Region region : regions) {
					if (!Boolean.valueOf(region.getFlags().getString("pve")).booleanValue()) {
						event.setCancelled(true);
						userDamager.sendActionBar(ChatColor.GRAY + "PVE is disabled in this region.");
						return;
					}
				}
			} else {
				for (Region region : regions) {
					if (!Boolean.valueOf(region.getFlags().getString("pvp")).booleanValue()) {
						event.setCancelled(true);
						userDamager.sendActionBar(ChatColor.GRAY + "PVP is disabled in this region.");
						return;
					}
				}
			}
			userDamager.incrementSkillProgress(SkillType.MELEE, Math.min(0.5D, 1.0D / distance));
			double randomMelee = Math.random() * userDamager.getSkillLevel(SkillType.MELEE) / distance;
			damage += randomMelee;
		}
		if (userTarget != null) {
			double randomDefense = Math.random() * Math.random() * userTarget.getSkillLevel(SkillType.DEFENSE);
			damage -= randomDefense;
			Item targetHeldItem = ItemLoader.fromBukkit(userTarget.getPlayer().getInventory().getItemInMainHand());
			double itemDefense = 0.0D;
			if (targetHeldItem != null)
				itemDefense = targetHeldItem.getArmor();
			byte b;
			int i;
			ItemStack[] arrayOfItemStack;
			for (i = (arrayOfItemStack = userTarget.getPlayer().getInventory().getArmorContents()).length, b = 0; b < i;) {
				ItemStack itemStack = arrayOfItemStack[b];
				Item armorItem = ItemLoader.fromBukkit(itemStack);
				if (armorItem != null)
					itemDefense += armorItem.getArmor();
				b++;
			}
			double actualItemDefense = Math.min(damage, Math.random() * itemDefense);
			damage -= actualItemDefense;
			userTarget.incrementSkillProgress(SkillType.DEFENSE, Math.random() * actualItemDefense);
		}
		damage = Math.max(0.0D, damage);
		if (npcTarget != null)
			npcTarget.setDamageExternalized(external);
		if (external) {
			npcTarget.damage(damage, damager);
			event.setDamage(0.0D);
			this.LOGGER.finer("-Damage event external from " + StringUtil.entityToString(target) + " to " + StringUtil.entityToString(npcTarget.getEntity()));
		} else {
			event.setDamage(damage);
			if (userDamager != null) {
				String tag = ChatColor.RED + "-" + Math.round(damage) + "❤";
				if (target.getNearbyEntities(10.0D, 10.0D, 10.0D).stream().filter(e -> (e.getType() == EntityType.PLAYER)).count() > 1L)
					tag = String.valueOf(tag) + ChatColor.GRAY + " from " + userDamager.getName();
				HologramUtil.temporaryArmorStand(target, tag, 20, false);
			}
		}
		if (npcTarget != null) {
			npcTarget.getNPCClass().handleTakeDamage(npcTarget, (npcDamager != null) ? (GameObject) npcDamager : (GameObject) userDamager, damage);
			npcTarget.updateHealthBar(damage);
			if (userDamager != null)
				npcTarget.getNPCClass().executeConditionals(NPCConditionalActions.NPCTrigger.HIT, userDamager, npcTarget);
		}
		if (npcDamager != null)
			npcDamager.getNPCClass().handleDealDamage(npcDamager, (npcTarget != null) ? (GameObject) npcTarget : (GameObject) userTarget, damage);
	}

	public void immortalTarget(Entity target, User damager) {
		if (damager == null)
			return;
		damager.sendActionBar(ChatColor.RED + "Target is immortal!");
		HologramUtil.temporaryArmorStand(target, ChatColor.LIGHT_PURPLE + "✦ Immortal Object", 40, false);
	}
}
