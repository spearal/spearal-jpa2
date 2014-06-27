/**
 * == @Spearal ==>
 * 
 * Copyright (C) 2014 Franck WOLFF & William DRAI (http://www.spearal.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spearal.jpa2;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;

/**
 * @author Franck WOLFF
 */
public abstract class AbstractHibernate4TestUnit {

	protected EntityManagerFactory entityManagerFactory;
	
	@Before
	public void setUp() throws Exception {
		Properties props = new Properties();
		props.put("javax.persistence.jdbc.driver", "org.h2.Driver");
		props.put("javax.persistence.jdbc.url", "jdbc:h2:mem:testdb");
		props.put("javax.persistence.jdbc.user", "sa");
		props.put("javax.persistence.jdbc.password", "");
		entityManagerFactory = Persistence.createEntityManagerFactory("hibernate4-pu", props);
	}

	@After
	public void tearDown() throws Exception {
		entityManagerFactory.close();
		entityManagerFactory = null;
	}
}
