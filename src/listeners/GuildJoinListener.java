package listeners;

import managers.ApplicationStats;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import utils.SQLUtils;

/**
 * Listens to guild join events from discord and ensures everything is setup properly.
 * 
 * @author Smc
 */
public class GuildJoinListener extends ListenerAdapter {

	@Override
	public void onGuildJoin(GuildJoinEvent p_event) {
		SQLUtils.setupGuildSQL(p_event.getGuild().getId());
		
		ApplicationStats.getInstance().addServerCount(1);
	}
}
