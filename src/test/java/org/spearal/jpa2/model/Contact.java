package org.spearal.jpa2.model;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="CONTACT")
public class Contact extends AbstractEntity {

	private static final long serialVersionUID = 1L;

    @ManyToOne(fetch=FetchType.LAZY, optional=false, cascade=CascadeType.ALL)
	private Person person;
	
	@Basic
	private String email;
	
	@Basic
	private String mobile;
	
	public Contact() {
	}

	public Contact(Person person, String email, String mobile) {
		this.person = person;
		this.email = email;
		this.mobile = mobile;
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
}
