package org.spearal.jpa2;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;

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
