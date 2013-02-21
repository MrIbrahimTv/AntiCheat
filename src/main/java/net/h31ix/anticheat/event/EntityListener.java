/*
 * AntiCheat for Bukkit.
 * Copyright (C) 2012-2013 AntiCheat Team | http://gravitydevelopment.net
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.h31ix.anticheat.event;

import net.h31ix.anticheat.Anticheat;
import net.h31ix.anticheat.manage.Backend;
import net.h31ix.anticheat.manage.CheckManager;
import net.h31ix.anticheat.manage.CheckType;
import net.h31ix.anticheat.util.CheckResult;
import net.h31ix.anticheat.util.Configuration;
import net.h31ix.anticheat.util.Distance;
import net.h31ix.anticheat.util.Utilities;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.potion.PotionEffectType;

public class EntityListener extends EventListener {
    private final Backend backend = getBackend();
    private final CheckManager checkManager = getCheckManager();
    private final Configuration config = Anticheat.getManager().getConfiguration();
    
    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (checkManager.willCheck(player, CheckType.FAST_BOW)) {
                CheckResult result = backend.checkFastBow(player, event.getForce());
                if (result.failed()) {
                    event.setCancelled(!config.silentMode());
                    log(result.getMessage(), player, CheckType.FAST_BOW);
                } else {
                    decrease(player);
                }
            }
        }
        
        Anticheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }
    
    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player && event.getRegainReason() == RegainReason.SATIATED) {
            Player player = (Player) event.getEntity();
            if (checkManager.willCheck(player, CheckType.FAST_HEAL)) {
                CheckResult result = backend.checkFastHeal(player);
                if (result.failed()) {
                    event.setCancelled(!config.silentMode());
                    log(result.getMessage(), player, CheckType.FAST_HEAL);
                } else {
                    decrease(player);
                    backend.logHeal(player);
                }
            }
        }
        
        Anticheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }
    
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (player.getFoodLevel() < event.getFoodLevel() && checkManager.willCheck(player, CheckType.FAST_EAT)) // Make sure it's them actually gaining a food level
            {
                CheckResult result = backend.checkFastEat(player);
                if (result.failed()) {
                    event.setCancelled(!config.silentMode());
                    log(result.getMessage(), player, CheckType.FAST_EAT);
                } else {
                    decrease(player);
                }
            }
        }
        
        Anticheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        boolean noHack = true;
        if (event instanceof EntityDamageByEntityEvent) {
            System.out.println("ENTITY DAMAGE BY ENTITY");
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                // Keep players from shooting an arrow at themselves in order to fly
                if (e.getDamager() instanceof Arrow) {
                    Arrow arrow = (Arrow) e.getDamager();
                    if (arrow.getShooter() instanceof Player && event.getEntity() == arrow.getShooter()) {
                        event.setCancelled(true);
                    }
                }
                if (Utilities.hasArmorEnchantment(player, Enchantment.THORNS)) {
                    backend.logAnimation(player);
                }
                if (e.getDamager() instanceof Player) {
                    Player p = (Player) e.getDamager();
                    backend.logDamage(p, 1);
                    int value = p.getInventory().getItemInHand().containsEnchantment(Enchantment.KNOCKBACK) ? 2 : 1;
                    backend.logDamage(player, value);
                    if (checkManager.willCheck(p, CheckType.LONG_REACH)) {
                        Distance distance = new Distance(player.getLocation(), p.getLocation());
                        CheckResult result = backend.checkLongReachDamage(player, distance.getXDifference(), distance.getYDifference(), distance.getZDifference());
                        if (result.failed()) {
                            event.setCancelled(!config.silentMode());
                            log(result.getMessage(), p, CheckType.LONG_REACH);
                            noHack = false;
                        }
                    }
                } else {
                    if (e.getDamager() instanceof TNTPrimed || e.getDamager() instanceof Creeper) {
                        backend.logDamage(player, 3);
                    } else {
                        backend.logDamage(player, 1);
                    }
                }
            }
            if (e.getDamager() instanceof Player) {
                Player player = (Player) e.getDamager();
                backend.logDamage(player, 1);
                if (checkManager.willCheck(player, CheckType.AUTOTOOL)) {
                    CheckResult result = backend.checkAutoTool(player);
                    if(result.failed()) {
                        event.setCancelled(!config.silentMode());
                        log(result.getMessage(), player, CheckType.AUTOTOOL);
                        noHack = false;                        
                    }
                }
                if (checkManager.willCheck(player, CheckType.FORCEFIELD)) {
                    CheckResult result = backend.checkSprintDamage(player);
                    if(result.failed()) {
                        event.setCancelled(!config.silentMode());
                        log(result.getMessage(), player, CheckType.FORCEFIELD);
                        noHack = false;
                    }
                }
                if (checkManager.willCheck(player, CheckType.NO_SWING)) {
                    CheckResult result = backend.checkAnimation(player, event.getEntity());
                    if(result.failed()) {                    
                        event.setCancelled(!config.silentMode());
                        log(result.getMessage(), player, CheckType.NO_SWING);
                        noHack = false;
                    }
                }
                if (checkManager.willCheck(player, CheckType.FORCEFIELD)) {
                    CheckResult result = backend.checkSight(player, e.getEntity());
                    if(result.failed()) {
                        event.setCancelled(!config.silentMode());
                        log(result.getMessage(), player, CheckType.FORCEFIELD);
                        noHack = false;
                    }
                }
                if (noHack) {
                    decrease(player);
                }
            }
        }
        
        Anticheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }
}
