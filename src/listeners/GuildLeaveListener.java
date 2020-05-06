package listeners;

import managers.ApplicationStats;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Listens to guild leave events from discord and gathers some runtime stats.
 * 
 * @author Smc
 */
public class GuildLeaveListener extends ListenerAdapter {

	@Override
	public void onGuildLeave(GuildLeaveEvent p_event) {
		ApplicationStats.getInstance().addServerCount(-1);
	}
}
