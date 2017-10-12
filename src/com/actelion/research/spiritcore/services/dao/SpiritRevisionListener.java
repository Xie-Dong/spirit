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

package com.actelion.research.spiritcore.services.dao;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionListener;

import com.actelion.research.spiritcore.services.SpiritUser;

/**
 * When a new revision is created, we make sure to add the logged in user, and the date of the DB.
 *
 * @author Joel Freyss
 *
 */
@RevisionEntity
public class SpiritRevisionListener implements RevisionListener {

	@Override
	public void newRevision(Object obj) {
		SpiritRevisionEntity rev = (SpiritRevisionEntity) obj;
		SpiritUser user = JPAUtil.getSpiritUser();
		rev.setUserId(user==null?"NA": user.getUsername());
		rev.setTimestamp(JPAUtil.getCurrentDateFromDatabase().getTime());
		DAOBarcode.reset();
	}
}