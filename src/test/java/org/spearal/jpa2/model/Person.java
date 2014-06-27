package org.spearal.jpa2.model;

import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="PERSON")
public class Person extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@Basic
	private String firstName;

	@Basic
	private String lastName;
	
	@ManyToOne(fetch=FetchType.LAZY, optional=true, cascade=CascadeType.ALL)
	private Person bestFriend;
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy="person", orphanRemoval=true, cascade=CascadeType.ALL)
	private Set<Contact> contacts;
	
	public Person() {
	}

	public Person(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Person getBestFriend() {
		return bestFriend;
	}

	public void setBestFriend(Person bestFriend) {
		this.bestFriend = bestFriend;
	}

	public Set<Contact> getContacts() {
		return contacts;
	}

	public void setContacts(Set<Contact> contacts) {
		this.contacts = contacts;
	}
}
