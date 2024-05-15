import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.HealingAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.waterbending.healing.HealingWaters;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class Aromatherapy extends HealingAbility implements AddonAbility, ComboAbility {
	
	private final String path = "";
	
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.SELECT_RANGE)
	private double select_range;
	@Attribute(Attribute.RADIUS)
	private double max_radius;
	
	private double radius;
	
	private Block target;
	private Location origin;
	
	public Aromatherapy(Player player) {
		super(player);
		
		if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
			return;
		} else if (bPlayer.isOnCooldown(this)) {
			return;
		} else if (!bPlayer.canWaterHeal() || !bPlayer.canPlantbend()) {
			return;
		} else if (RegionProtection.isRegionProtectedFromBuild(player, player.getLocation(), this)) {
			return;
		}
		this.cooldown = ConfigManager.getConfig().getLong(path + "Cooldown");
		this.select_range = ConfigManager.getConfig().getDouble(path + "SelectRange");
		this.max_radius = ConfigManager.getConfig().getDouble(path + "Radius");
		
		this.target = rayTraceBlock(player, this.select_range);
		
		if (this.target == null) return;
		if (RegionProtection.isRegionProtectedFromBuild(player, this.target.getLocation(), this)) {
			return;
		}
		this.origin = this.target.getLocation().clone().add(0.5, 0.5, 0.5);
		
		if (CoreAbility.hasAbility(player, HealingWaters.class)) {
			getAbility(player, HealingWaters.class).remove();
		}
		start();
	}
	
	@Override
	public void progress() {
		if (!player.isSneaking()) {
			remove();
			return;
		}
		if (player.getLocation().distanceSquared(this.origin) > this.max_radius * this.max_radius) {
			remove();
			return;
		}
		this.radius += 0.1;
		if (this.radius > this.max_radius) this.radius = 0;
		
		player.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, this.origin, 10, this.max_radius / 2.0, this.max_radius / 2.0, this.max_radius / 2.0, 0);
		
		circle(this.origin, this.radius, l -> {
			if (ThreadLocalRandom.current().nextInt(80) == 0) {
				player.getWorld().spawnParticle(Particle.GLOW, l, 1, 0, 0, 0, 0.1);
				ParticleEffect.FALLING_DUST.display(l, 1, 0.35F, 0.35F, 0.35F, 0.01F, l.getBlock().getBlockData());
			}
			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(l, 1)) {
				if (entity instanceof LivingEntity) {
					if (entity instanceof Player) {
						if (Commands.invincible.contains(entity.getName())) continue;
					}
					if (!((LivingEntity) entity).hasPotionEffect(PotionEffectType.REGENERATION)) {
						if (isFlowers(l.getBlock().getType())) {
							((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 75, 1));
						} else {
							((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 150, 0));
						}
					}
				}
			}
		});
	}
	
	private void circle(Location location, double size, Consumer<Location> consumer) {
		for (int angle = 0; angle < 180; angle++) {
			double x = size * Math.cos(Math.toRadians(angle - 360) * 2);
			double z = size * Math.sin(Math.toRadians(angle - 360) * 2);
			
			Location circle = location.clone().add(x, 0, z);
			Block top = GeneralMethods.getTopBlock(circle, 3);
			
			if (!GeneralMethods.isRegionProtectedFromBuild(this, top.getLocation())) {
				if (isPlant(top)) {
					consumer.accept(top.getLocation().clone().add(0.5, 0.5, 0.5));
				}
			}
		}
	}

	public static Block rayTraceBlock(Player player, double range) {
		RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation().clone(), player.getEyeLocation().getDirection(), range);
		if (result != null) {
			return result.getHitBlock();
		}
		return null;
	}

	public static boolean isFlowers(Material material) {
		switch (material) {
			case DANDELION:
			case POPPY:
			case BLUE_ORCHID:
			case ALLIUM:
			case AZURE_BLUET:
			case RED_TULIP:
			case ORANGE_TULIP:
			case PINK_TULIP:
			case WHITE_TULIP:
			case OXEYE_DAISY:
			case CORNFLOWER:
			case LILY_OF_THE_VALLEY:
			case WITHER_ROSE:
			case SUNFLOWER:
			case LILAC:
			case ROSE_BUSH:
			case PEONY:
				return true;
		}
		return false;
	}
	
	public static boolean isFlowers(Block block) {
		return isFlowers(block.getType());
	}
	
	@Override
	public boolean isSneakAbility() {
		return true;
	}
	
	@Override
	public boolean isHarmlessAbility() {
		return true;
	}
	
	@Override
	public long getCooldown() {
		return cooldown;
	}
	
	@Override
	public String getName() {
		return "Aromatherapy";
	}
	
	@Override
	public Location getLocation() {
		return null;
	}
	
	@Override
	public void remove() {
		bPlayer.addCooldown(this);
		super.remove();
	}
	
	@Override
	public void load() { }
	
	@Override
	public void stop() { }
	
	@Override
	public boolean isEnabled() {
		return ConfigManager.getConfig().getBoolean("Aromatherapy.Enabled", true);
	}
	
	@Override
	public String getAuthor() {
		return "Prride";
	}
	
	@Override
	public String getVersion() {
		return "1.0.0";
	}
	
	@Override
	public String getDescription() {
		return Element.WATER.getColor() + "Similarly to how waterbenders are able to use water in order to heal people, they are able to "
				+ "cause widespread healing with the use of water containing special healing properties within plants and flowers by aromatherapy."
				+ "\n(Default: being near plants provide regeneration 1 while flowers provide regeneration 2)";
	}
	
	@Override
	public String getInstructions() {
		return "PlantManipulation (Right click block) > HealingWaters (Hold sneak at a plant block)";
	}
	
	@Override
	public Object createNewComboInstance(Player player) {
		return new Aromatherapy(player);
	}
	
	@Override
	public ArrayList<AbilityInformation> getCombination() {
		ArrayList<AbilityInformation> info = new ArrayList<>();
		info.add(new AbilityInformation("PlantManipulation", ClickType.RIGHT_CLICK_BLOCK));
		info.add(new AbilityInformation("HealingWaters", ClickType.SHIFT_DOWN));
		return info;
	}
}
