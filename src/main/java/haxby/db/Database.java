package haxby.db;

import haxby.map.MapApp;
import haxby.map.Overlay;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import javax.swing.JComponent;

/**
 	A database can be loaded, enabled, and disabled.
*/
public abstract interface Database extends Overlay {

	/**
		Gets the name of the Database.
		@return The name of the Database.
	*/
	public String getDBName();

	/**
		Gets the command associated with the Database.
		@return The command for the Database.
	 */
	public String getCommand();

	/**
	 	Gets a larger description of the database.
	 	@return The larger description of a database.
	 */
	public String getDescription();

	/** Load data into this Database object.
		@return Returns true if the Database loaded properly,
		false if not.
	 */
	public boolean loadDB();

	/** 
	 	Gets if the database has finished loaded.
	 	@return True if the Database has loaded, false if not.
	 */
	public boolean isLoaded();
	
	/**
	 * Set the initialize/loaded parameter to false, requiring the DB to be reloaded
	 */
	public void unloadDB();

	/** 
		Dispose of this Database object, and free up its resources.
	 */
	public void disposeDB();

	/**
	 	Enables or disables the database and its listeners.
	 	@param if true for enabled, false for not.
	*/
	public void setEnabled( boolean tf );

	/**
		Gets if the database is enabled (Listeners active).
		@return true if the database is enabled (Listeners active).
	*/
	public boolean isEnabled();

	/**
		Gets the JComponent of the SelectionDialog(Panel on right)
		@return the JComponent of the right panel.
	*/
	public JComponent getSelectionDialog();

	/**
		Gets the JComponent of the DataDisplay(Panel on bottom).
		@return the JComponent of the bottom panel.
	*/
	public JComponent getDataDisplay();
	
	/**
	 * Does some setup before loading the DB.
	 */
	public default void setup() {
	}
	
	/**
	 * Checks to see if the DB loading was cancelled.
	 * @return true iff the DB loading was cancelled.
	 */
	public default boolean isLoadCancelled() {
		return false;
	}
	
	public default Map<String, String> getCacheMap() {
		return null;
	}
	
	public default String getCacheFile(String remoteDir) {
		Map<String, String> cacheMap = getCacheMap();
		if(null != getCacheMap() && cacheMap.containsKey(remoteDir)) {
			return cacheMap.get(remoteDir);
		}
		return remoteDir.replace(MapApp.BASE_URL + "data/portals", getCacheBaseDir());
	}
	
	public static String getCacheBaseDir() {
		return MapApp.getGMARoot().getAbsolutePath() + String.join(File.separator, "", "menus_cache", "portals");
	}
	
	public default String getPortalCacheBaseDir() {
		return getCacheBaseDir() + File.separator + getCommand();
	}
}