package listeners;

import commands.Command;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * This listener listens to all button click events and ensures they are executed by the right command.
 * 
 * @author Smc
 */
public class ButtonClickListener extends ListenerAdapter {
	
	@Override
	public void onButtonClick(ButtonClickEvent p_event) {
		String trigger = p_event.getComponentId().split("\\|\\|\\|")[0];
		Command command = Command.findCommand(trigger);
		
		if(command != null)
			command.onButtonClick(p_event, p_event.getComponentId().replace(trigger + "|||", "").split("\\|\\|\\|"));
		else p_event.deferEdit().queue();
	}
}
