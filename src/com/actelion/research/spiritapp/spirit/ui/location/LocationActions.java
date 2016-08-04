/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2016 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16,
 * CH-4123 Allschwil, Switzerland.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * @author Joel Freyss
 */

package com.actelion.research.spiritapp.spirit.ui.location;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.services.print.PrintLabel;
import com.actelion.research.spiritapp.spirit.ui.location.edit.LocationBatchEditDlg;
import com.actelion.research.spiritapp.spirit.ui.print.BrotherLabelsDlg;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.Privacy;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class LocationActions {

	public static class Action_New extends AbstractAction {
		private Location parent;

		public Action_New() {
			super("New Location");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('l'));
			putValue(AbstractAction.SMALL_ICON, IconType.LOCATION.getIcon());
		}
		public Action_New(Location parent) {
			super("New Location" + (parent==null?"": " (under "+parent.getName()+")"));
			this.parent = parent;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('l'));
			putValue(AbstractAction.SMALL_ICON, IconType.LOCATION.getIcon());
			setEnabled(SpiritRights.canRead(parent, Spirit.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(Spirit.getUser()==null) return;
			Location location = new Location();
			if(parent!=null) {
				location.setLocationType(parent.getLocationType().getDefaultChild());
				location.setCols(parent.getCols());
				location.setRows(parent.getRows());
				location.setParent(parent);
			}
			location.setName("");
			
			
			if(Spirit.getUser()!=null  && !Spirit.getUser().isSuperAdmin() && (parent==null || parent.getInheritedPrivacy()==Privacy.PUBLIC)) {
				location.setPrivacy(Privacy.PROTECTED);
				location.setEmployeeGroup(Spirit.getUser().getMainGroup());
			}
			
			LocationBatchEditDlg.edit(Collections.singletonList(location));
		}
	}
	
	public static class Action_Delete extends AbstractAction {
		private List<Location> locations;

		public Action_Delete(Location location) {
			super("Delete Location ("+location+")");
			this.locations = Collections.singletonList(location);
			putValue(AbstractAction.MNEMONIC_KEY, (int)('l'));
			putValue(AbstractAction.SMALL_ICON, IconType.DELETE.getIcon());
			setEnabled(SpiritRights.canEdit(location, Spirit.getUser()));
		}
		
		public Action_Delete(List<Location> locations) {
			super("Delete Batch");
			this.locations = locations;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('l'));
			putValue(AbstractAction.SMALL_ICON, IconType.DELETE.getIcon());
			boolean enabled = true;
			for (Location l : locations) {
				if(!SpiritRights.canEdit(l, Spirit.getUser())) {
					enabled = false;
				}
			}
			setEnabled(enabled);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(Spirit.getUser()==null) return;
			LocationBatchEditDlg.deleteInNewContext(locations);
		}
	}
	
	public static class Action_Print extends AbstractAction {
		private List<Location> locations;

		public Action_Print(Location location) {
			super("Print Label");
			this.locations = Collections.singletonList(location);
			putValue(AbstractAction.MNEMONIC_KEY, (int)('p'));
			putValue(AbstractAction.SMALL_ICON, IconType.PRINT.getIcon());
			setEnabled(SpiritRights.canRead(location, Spirit.getUser()));
		}
		
		public Action_Print(List<Location> locations) {
			super("Print Labels");
			this.locations = locations;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('p'));
			putValue(AbstractAction.SMALL_ICON, IconType.PRINT.getIcon());
			boolean enabled = true;
			for (Location l : locations) {
				if(!SpiritRights.canRead(l, Spirit.getUser())) {
					enabled = false;
				}
			}
			setEnabled(enabled);
		}
		
		public List<Location> getLocations() {
			return locations;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			List<PrintLabel> labels = new ArrayList<PrintLabel>();
			for (Location loc : getLocations()) {
				labels.add(new PrintLabel("" + loc.getLocationId(), loc.getName()+ "\n" + (loc.getDescription()==null?"": loc.getDescription())));
			}
			new BrotherLabelsDlg(labels);
		}
	}
	public static class Action_Duplicate extends AbstractAction {
		private List<Location> locations;
		public Action_Duplicate(Location location) {
			super("Duplicate Location");
			this.locations = Collections.singletonList(location);
			putValue(AbstractAction.MNEMONIC_KEY, (int)('d'));
			putValue(AbstractAction.SMALL_ICON, IconType.DUPLICATE.getIcon());
			setEnabled(SpiritRights.canEdit(location.getParent(), Spirit.getUser()));
		}
		
		public Action_Duplicate(List<Location> locations) {
			super("Duplicate Batch");
			this.locations = locations;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('d'));
			putValue(AbstractAction.SMALL_ICON, IconType.DUPLICATE.getIcon());
			boolean enabled = true;
			for (Location location : locations) {
				if(!SpiritRights.canEdit(location.getParent(), Spirit.getUser())) {
					enabled = false;
					break;
				}
			}
			setEnabled(enabled);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if(Spirit.getUser()==null) return;
			

			LocationBatchEditDlg.duplicate(locations);
		}
	}
	
	
	public static class Action_EditBatch extends AbstractAction {
		private List<Location> locations;
		
		public Action_EditBatch(Location location) {
			super("Edit / Move Location ("+location.getName()+")");
			this.locations = Collections.singletonList(location);
			putValue(AbstractAction.MNEMONIC_KEY, (int)('e'));
			putValue(AbstractAction.SMALL_ICON, IconType.EDIT.getIcon());
			setEnabled(SpiritRights.canEdit(location, Spirit.getUser()));

		}
		
		public Action_EditBatch(List<Location> locations) {
			super("Edit Batch");
			this.locations = locations;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('e'));
			putValue(AbstractAction.SMALL_ICON, IconType.EDIT.getIcon());
			boolean enabled = true;
			for(Location l: locations) {
				if(!SpiritRights.canEdit(l, Spirit.getUser())) enabled = false;
			}
			
			setEnabled(enabled);

		}
		@Override
		public void actionPerformed(ActionEvent e) {
			LocationBatchEditDlg.edit(locations);
		}
	}
	
	public static class Action_Move extends AbstractAction {
		private List<Location> locations;
		public Action_Move(Location location) {
			super("Move Location");
			this.locations = Collections.singletonList(location);
			putValue(AbstractAction.MNEMONIC_KEY, (int)('m'));
			setEnabled(SpiritRights.canEdit(location, Spirit.getUser()));
		}
		public Action_Move(List<Location> locations) {
			super("Move Batch");
			this.locations = locations;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('m'));			
			if(locations.size()==0) {
				setEnabled(false);
			} else {
				boolean enabled = true;
				Location parent = locations.get(0).getParent();
				for (Location l : locations) {
					if(!SpiritRights.canEdit(l, Spirit.getUser()) || CompareUtils.compare(parent, l.getParent())!=0) {
						enabled = false;
					}
				}
				setEnabled(enabled);				
			}
		}

		
		@Override
		public void actionPerformed(ActionEvent e) {
			new MoveLocationDialog(locations);
		}
	}
	
//	public static class Action_ScanUpdate extends AbstractAction {
//		public Action_ScanUpdate(Location loc) {
//			super("Scan & Update");
//			setEnabled(loc!=null && loc.getLocationType()==LocationType.RACK && SpiritRights.canEdit(loc, Spirit.getUser()));
//		}
//		
//		@Override
//		public void actionPerformed(ActionEvent e) {
//			
//		}
//		
//	}
	
	public static JPopupMenu createPopup(List<Location> locations) {
		JPopupMenu popupMenu = new JPopupMenu();
		
		if(locations.size()==1) {
			Location l = locations.get(0);
			popupMenu.add(new JMenuItem(new Action_New(l)));
			popupMenu.add(new JSeparator());
			popupMenu.add(new JMenuItem(new Action_EditBatch(l)));			
			popupMenu.add(new JMenuItem(new Action_Duplicate(l)));				
			popupMenu.add(new JMenuItem(new Action_Delete(l)));
//			popupMenu.add(new JMenuItem(new Action_Move(l)));
			popupMenu.add(new JSeparator());
			popupMenu.add(new JMenuItem(new Action_Print(l)));			
		} else if(locations.size()>1) {
			popupMenu.add(new JMenuItem(new Action_EditBatch(locations)));			
			popupMenu.add(new JMenuItem(new Action_Duplicate(locations)));				
			popupMenu.add(new JMenuItem(new Action_Delete(locations)));				
//			popupMenu.add(new JMenuItem(new Action_Move(locations)));
			popupMenu.add(new JSeparator());
			popupMenu.add(new JMenuItem(new Action_Print(locations)));
		}
		
		return popupMenu;
	}
	
	
	public static JPopupMenu createPopup(Location location) {
		return createPopup(location==null? new ArrayList<Location>(): Collections.singletonList(location));		
	}
		
	public static void attachPopup(final LocationTable table) {
		table.addMouseListener(new PopupAdapter(table) {
			@Override
			protected void showPopup(MouseEvent e) {

				JPopupMenu popupMenu = LocationActions.createPopup(table.getSelection());
				popupMenu.insert(table.new TreeViewExpandAll(true, true), 0);
				popupMenu.insert(table.new TreeViewExpandAll(false, true), 1);
				popupMenu.insert(new JSeparator(), 2);
				popupMenu.show(table, e.getX(), e.getY());
			}
		});
	}

	
}