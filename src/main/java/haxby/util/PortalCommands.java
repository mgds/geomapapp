package haxby.util;

import java.util.ArrayList;

public class PortalCommands {

	public static String[] portal_commands = {
		"bgm_cmd",
		"multibeam_bathymetry_cmd",
		"petdb_cmd",
		"ics_cmd",
		"cmt_cmd",		// Focal Mechanism Portal
		"seve_cmd",
		"mcs_cmd",
		"scs_cmd",
		"magnetic_anomaly_cmd",
		"seafloor_driling_cmd",
		"radar_cmd",
		"ship_cmd",
		"survey_planner_cmd",
		"velocity_vectors_cmd"};

	public static ArrayList<String> getPortalCommands() {
		ArrayList<String> portal_commands_ht =  new ArrayList<String>();
		for ( int i = 0; i < portal_commands.length; i++ ) {
			portal_commands_ht.add(portal_commands[i]);
		}
		return portal_commands_ht;
	}
}