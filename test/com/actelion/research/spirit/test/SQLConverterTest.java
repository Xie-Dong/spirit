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

package com.actelion.research.spirit.test;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.migration.MigrationScript;
import com.actelion.research.spiritcore.util.SQLConverter;
import com.actelion.research.spiritcore.util.SQLConverter.SQLVendor;

/**
 * Unit Tests used to test the SQL conversion from the migration script from the Open Source of Spirit
 * @author Joel Freyss
 *
 */
public class SQLConverterTest {
	static Connection hsqlConn;

	@BeforeClass
	public static void initDB() throws Exception {
		JPAUtil.closeFactory();
		hsqlConn = DriverManager.getConnection("jdbc:hsqldb:mem:spirit", "SA", "");;
	}

	@Test
	public void testConvert() throws Exception {
		Assert.assertEquals("bigint", SQLConverter.convertScript("number(19)", SQLVendor.HSQL));
		Assert.assertEquals("number(19)", SQLConverter.convertScript("number(19)", SQLVendor.ORACLE));
	}

	@Test
	public void testConversion() throws Exception {
		//Test some schema creation on HSQL using Oracle syntax
		String script = "create schema test;\n"
				+ "create table test.revinfo (rev number(10,0), revtstmp number(19,0), primary key (rev));\n"
				+ "create table test.biosample (id number(19) not null, constraint bio_pk primary key (id));\n"
				+ "create table test.spirit_property (id varchar2(64 char) not null, value varchar2(128 char), constraint spp_pk primary key (id));\n"
				+ "create table test.biosample_biosample (biosample_id number(19) not null, linked_id number(19) not null, bm_id number(19) not null);\n"
				+ "alter table test.revinfo add (userid2 varchar2(20 char));\n"
				+ "alter table test.biosample_biosample add constraint bb_fk1 foreign key (biosample_id) references test.biosample (id);\n"
				+ "alter table test.spirit_property modify (id varchar2(80 char));\n"
				+ "alter table test.spirit_property add (bool number(1));\n"
				+ "alter table test.spirit_property add (c1 varchar2(256));\n"
				+ "alter table test.spirit_property add (c2 varchar2(256));\n"
				+ "";

		String converted = SQLConverter.convertScript(script, SQLVendor.HSQL);
		MigrationScript.executeScript(hsqlConn, converted, true, null);
	}



	@Test
	public void testConversion2() throws Exception {
		//Test some schema creation on HSQL using Oracle syntax
		String script = "alter table spirit.spirit_property modify (id varchar2(64 CHAR));";
		Assert.assertEquals("alter table spirit.spirit_property modify id varchar(64);", SQLConverter.convertScript(script, SQLVendor.MYSQL));
		Assert.assertEquals("alter table spirit.employee_group add (disabled integer, creuser varchar(20), credate time);", SQLConverter.convertScript("alter table spirit.employee_group add (disabled number(3), creuser varchar2(20), credate time);", SQLVendor.MYSQL));
	}

	@Test
	public void testConversion3() throws Exception {
		//Test some table updates on HSQL using Oracle syntax
		String script = "create schema test2;\n"
				+ "create table test2.spirit_property (id varchar2(64 char) not null, value varchar2(128 char), constraint spp_pk primary key (id));"
				+ "insert into test2.spirit_property values('myProp', 'myValue')"
				+ "update test2.spirit_property set value = concat('VALUE=', replace(replace(value, '\\', '\\\\'), ';', '\\;'));\n";
		MigrationScript.executeScript(hsqlConn, SQLConverter.convertScript(script, SQLVendor.HSQL), true, null);
	}

	@Test
	public void testConversion4() throws Exception {
		//Test some table updates on HSQL using Oracle syntax
		String script = "create schema test3;\n"
				+ "create table test3.t1 (id varchar2(64 char) not null, value varchar2(128 char));"
				+ "insert into test3.t1 values('myProp', 'myValue');\n"
				+ "insert into test3.t1 (select id, concat('a', value, value) from test3.t1);\n";

		MigrationScript.executeScript(hsqlConn, SQLConverter.convertScript(script, SQLVendor.HSQL), true, null);

	}

	@AfterClass
	public static void after() throws Exception {
		hsqlConn.close();
	}
}
