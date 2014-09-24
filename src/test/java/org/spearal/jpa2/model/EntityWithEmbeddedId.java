package org.spearal.jpa2.model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
public class EntityWithEmbeddedId implements Serializable {

	private static final long serialVersionUID = 1L;

	public static class EntityEmbeddedId implements Serializable {
		
		private static final long serialVersionUID = 1L;

		private String firstName;
		private String lastName;
		
		public EntityEmbeddedId() {
		}

		public EntityEmbeddedId(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getLastName() {
			return lastName;
		}
	}
	
	@EmbeddedId
	private EntityEmbeddedId id;
	
	private int age; 
	
	@ElementCollection
	private List<String> phones;
	
	public EntityWithEmbeddedId() {
	}

	public EntityEmbeddedId getId() {
		return id;
	}

	public void setId(EntityEmbeddedId id) {
		this.id = id;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public List<String> getPhones() {
		return phones;
	}

	public void setPhones(List<String> phones) {
		this.phones = phones;
	}
}
