package ml.chriscommunity.survivalplus.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;

public class Main extends JavaPlugin implements Listener {
	DataManager data = new DataManager(this);
	
	private static final Logger log = Logger.getLogger("Minecraft");
	private static Economy econ = null;
	private static Permission perms = null;
	private static Chat chat = null;
	

	@Override
	public void onEnable() {
		if(!setupEconomy()) {
			log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
		}
		setupPermissions();
		setupChat();
		data.saveConfig();

		getServer().getPluginManager().registerEvents(this, this);
	}
	
	private boolean setupEconomy() {
		if(getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if(rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
				
		return econ != null;
	}
	
	private boolean setupChat() {
		RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
		chat = rsp.getProvider();
		return chat != null;
	}
	
	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
		perms = rsp.getProvider();
		return chat != null;
	}

	@Override
	public void onDisable() {

	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player player = (Player) sender;
		if(player.hasPermission("nte.admin") || player.hasPermission("nte.owner")) {
			if(label.equalsIgnoreCase("coin")) {
				if(args.length == 0) {
					player.getInventory().setItem(player.getInventory().firstEmpty(), getCoin());
					return true;
				} else if(args.length == 1) {
					if(args[0].equalsIgnoreCase("give")) {
						player.sendMessage(ChatColor.RED + "Usage: /coin give <player> <num>");
						return true;
					} else if (args[0].equalsIgnoreCase("take")){
						player.sendMessage(ChatColor.RED + "Usage: /coin take <player> <num>");
						return true;
					} else {
						player.sendMessage(ChatColor.RED + "Usage: /coin take or /coin give");
						return true;
					}
				} else if(args.length == 2) {
					if(args[0].equalsIgnoreCase("give")) {
						player.sendMessage(ChatColor.RED + "Usage: /coin give <player> <num>");
						return true;
					} else if(args[0].equalsIgnoreCase("take")) {
						player.sendMessage(ChatColor.RED + "Usage: /coin take <player> <num>");
						return true;
					}
				} else if(args.length == 3) {
					if(args[0].equalsIgnoreCase("give")) {
						if(Double.parseDouble(args[2]) <= 0) {
							player.sendMessage(ChatColor.RED + "Amount cannot be less than 0.");
							return true;
						}
						EconomyResponse r = econ.depositPlayer(Bukkit.getPlayer(args[1]), Double.parseDouble(args[2]));
						if(r.transactionSuccess()) {
							Bukkit.getPlayer(args[1]).sendMessage(String.format("You were given %s and now have %f", econ.format(r.amount), r.amount + r.balance));
							data.getConfig().set(Bukkit.getPlayer(args[1]).getUniqueId() + ".balance", r.balance);
							data.saveConfig();
						}
						
						return true;
					} else if(args[0].equalsIgnoreCase("take")) {
						if(Double.parseDouble(args[2]) < 0) {
							player.sendMessage(ChatColor.RED + "Amount cannot be less than 0.");
							return true;
						}
						EconomyResponse r = econ.withdrawPlayer(Bukkit.getPlayer(args[1]), Double.parseDouble(args[2]));
						if(r.transactionSuccess()) {
							Bukkit.getPlayer(args[1]).sendMessage(String.format("%s was taken from you and now have %f", econ.format(Math.abs(r.amount)), r.balance - r.amount));
							data.getConfig().set(Bukkit.getPlayer(args[1]).getUniqueId() + ".balance", r.balance);
							data.saveConfig();
						}
						
						return true;
					}
				}
			}
		}
		if(label.equalsIgnoreCase("rtp")) {
			if(getServer().getWorld("public_sv") == null) {
				getServer().createWorld(new WorldCreator("public_sv"));
			}
			Random rand = new Random();
			int x = 0;
			int y = 0;
			int z = 0;
			do {
				x = rand.nextInt(20000) - 10000;
				y = 256;
				z = rand.nextInt(20000) - 10000;
				while(getServer().getWorld("public_sv").getBlockAt(x, y, z).getType() == Material.AIR) {
					y--;
				}
			} while(getServer().getWorld("public_sv").getBlockAt(x, y-1, z).getType() == Material.LAVA || getServer().getWorld("public_sv").getBlockAt(x, y+1, z).getType() != Material.AIR);
			y+=6;
			Location loc = new Location(getServer().getWorld("public_sv"), x, y, z);
			getServer().getWorld("public_sv").loadChunk(x, z, true);
			player.teleport(loc);
		}
		

		return false;
	}

	ItemStack getCoin() {
		String url = "http://textures.minecraft.net/texture/8a03a8a877de7a4d6b167633a96ae3983998fd9d9a4c5e3fa817d138e81e4499";
		ArrayList<String> lore = new ArrayList<>();
		lore.add("Coin");

		GameProfile profile = new GameProfile(UUID.randomUUID(), "coin");
		PropertyMap propertyMap = profile.getProperties();
		if (propertyMap == null) {
			throw new IllegalStateException("Profile doesn't contain a property map");
		}
		byte[] encodedData = Base64.encodeBase64(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
		propertyMap.put("textures", new Property("textures", new String(encodedData)));
		ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
		ItemMeta headMeta = head.getItemMeta();
		Class<?> headMetaClass = headMeta.getClass();
		Reflections.getField(headMetaClass, "profile", GameProfile.class).set(headMeta, profile);
		headMeta.setDisplayName("Coin");
		head.setItemMeta(headMeta);

		return head;
	}

	@EventHandler
	void onCoinClick(PlayerInteractEvent event) {
		Player player = (Player) event.getPlayer();
		Block clickedBlock = null; 

		if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			clickedBlock = event.getClickedBlock();
			if (clickedBlock.getState() instanceof Skull) {
				Skull skull = (Skull) clickedBlock.getState();
				if(skull.getOwner() == null)
					return;
				if(skull.getOwner().equalsIgnoreCase("coin") && !data.getConfig().getStringList(player.getUniqueId().toString() + ".coins").contains(clickedBlock.getLocation().toString())) {
					List<String> list = data.getConfig().getStringList(player.getUniqueId() + ".coins");
					list.add(clickedBlock.getLocation().toString());
					EconomyResponse r = econ.depositPlayer(player, 1);
					if(r.transactionSuccess()) {
						player.sendMessage(ChatColor.GOLD + "+1 coin");
					}
					
					data.getConfig().set(player.getUniqueId() + ".coins", list);
					data.saveConfig();
				}
			}
		} else {
			return;
		}
	}
	
	@EventHandler
	void onCommandPreprocess (PlayerCommandPreprocessEvent event) {
		if(event.getMessage().equalsIgnoreCase("/bal") || event.getMessage().equalsIgnoreCase("/balance")) {
			event.setCancelled(true);
			Player player = (Player) event.getPlayer();
			player.sendMessage(String.format("You have %s", econ.format(econ.getBalance(player))));
		}
	}
}
