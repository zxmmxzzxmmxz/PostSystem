package com.sfu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sfu.Logging.LogType;

public class Office {
	private String name;
	private int transitTime;
	private int requiredPostage;
	private int capacity;
	private int persuasionAmount;
	private int maxPackageLength;
	private List<Deliverable> toMail = new ArrayList<>();
	private List<Deliverable> toPickUp = new ArrayList<>();

	private Set<String> wanted;
	public void setWanted(Set<String> wanted) {
		this.wanted = wanted;
	}
	private Network network;
	public void setNetwork(Network network) {
		this.network = network;
	}

	public Office(String name, int transitTime, int requiredPostage,
			int capacity, int persuasionAmount, int maxPackageLength) {
		super();
		this.name = name;
		this.transitTime = transitTime;
		this.requiredPostage = requiredPostage;
		this.capacity = capacity;
		this.persuasionAmount = persuasionAmount;
		this.maxPackageLength = maxPackageLength;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getTransitTime() {
		return transitTime;
	}
	public void setTransitTime(int transitTime) {
		this.transitTime = transitTime;
	}
	public int getRequiredPostage() {
		return requiredPostage;
	}
	public void setRequiredPostage(int requiredPostage) {
		this.requiredPostage = requiredPostage;
	}
	public int getCapacity() {
		return capacity;
	}
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	public int getPersuasionAmount() {
		return persuasionAmount;
	}
	public void setPersuasionAmount(int persuasionAmount) {
		this.persuasionAmount = persuasionAmount;
	}
	public int getMaxPackageLength() {
		return maxPackageLength;
	}
	public void setMaxPackageLength(int maxPackageLength) {
		this.maxPackageLength = maxPackageLength;
	}

	public void acceptLetterIfGood(Letter letter) {
		boolean hasCriminalRecipient = wanted.contains(letter.getRecipient());
		boolean officeFull = isFull();
		Office destOffice = letter.getDestOffice();
		if (destOffice != null && !hasCriminalRecipient && !officeFull) {
			accept(letter);
		} else {
			Logging.rejectDeliverable(LogType.MASTER, letter);
			Logging.rejectDeliverable(LogType.OFFICE, letter);
		}
	}

	//Receive from person
	public void accept(Deliverable d) {
		Logging.deliverableAccepted(LogType.OFFICE, d);
		toMail.add(d);
	}

	//Receive from network
	public void receiveFromNetwork(Deliverable d) {
		if (d instanceof Package) {
			Package p = (Package) d;
			if (this.maxPackageLength < p.getLength()) {
				Logging.deliverableDestroyed(LogType.MASTER, d);
				Logging.deliverableDestroyed(LogType.OFFICE, d);
				return;
			}
		}

		if (isFull()) {
			Logging.deliverableDestroyed(LogType.MASTER, d);
			Logging.deliverableDestroyed(LogType.OFFICE, d);
			return;
		}

		toPickUp.add(d);
	}

	public void sendToNetwork() {
		for (int idx = toMail.size()-1 ; idx >= 0 ; idx--) {
			Deliverable d = toMail.get(idx);
			toMail.remove(idx);
			network.put(d);
			Logging.transitSent(LogType.OFFICE, d);

		}
	}

	public void pickUp(String recipient, int day) {
		int size = toPickUp.size();
		for (int idx = size-1 ; idx >= 0 ; idx--) {
			Deliverable d = toPickUp.get(idx);
			if (recipient.equals(d.getRecipient())) {
				toPickUp.remove(idx);
				Logging.itemComplete(LogType.OFFICE, d, day);
			}
		}
	}

	public boolean isFull() {
		return (this.toMail.size() + this.toPickUp.size()) >= capacity;
	}

	public boolean isEmpty() {
		return (this.toMail.size() + this.toPickUp.size()) != 0;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Office) && (this.name.equals(((Office)obj).getName()));
	}

	@Override
 	public String toString() {
 		return this.name;
 	}
}
