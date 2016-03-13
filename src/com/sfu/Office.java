package com.sfu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sfu.Logging.LogType;

public class Office {
	private boolean is_destroyed = false;
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

	public int itemsInTotal(){
		return toPickUp.size()+toMail.size();
	}
	public void destroy(){
		this.is_destroyed = true;
	}
	
	public boolean is_destroy(){
		return this.is_destroyed;
	}
	
	public void rebuild(){
		this.is_destroyed = false;
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

	public boolean pickUp(String recipient, int day) {
		boolean goodday = false;
		if(!is_destroyed){
			int size = toPickUp.size();
			for (int idx = size-1 ; idx >= 0 ; idx--) {
				Deliverable d = toPickUp.get(idx);
				if (recipient.equals(d.getRecipient())) {
					Deliverable item = toPickUp.remove(idx);
					goodday = true;
					Logging.itemComplete(LogType.OFFICE, d, day);
					if(item instanceof Letter){
						Letter letter = (Letter) item;
						if(wanted.contains(letter.getReturnRecipient())){
							this.destroy();
							Logging.officeDestroy(LogType.MASTER, letter.getDestOffice().getName());
							Logging.officeDestroy(LogType.OFFICE, letter.getDestOffice().getName());
						}
					}
				}
			}
		}
		return goodday;
	}

	public boolean isFull() {
		return (this.toMail.size() + this.toPickUp.size()) >= capacity;
	}

	public boolean isEmpty() {
		return (this.toMail.size() + this.toPickUp.size()) == 0;
	}
	
	public void drop(int day){
		boolean no_more_drop = false;
		while(!no_more_drop){
			if(toPickUp.size()==0)
				no_more_drop = true;
			for(int index = 0;index<toPickUp.size();index++){
				if(day-toPickUp.get(index).getInitDay()>14){
					if(toPickUp.get(index) instanceof Letter){
						Letter letter = (Letter)toPickUp.get(index);
						if(!letter.getReturnRecipient().equals("NONE")){
							Letter new_return_letter = new Letter();
							new_return_letter.setReturnRecipient("NONE");
							new_return_letter.setDestOffice(letter.getIniatingOffice());
							new_return_letter.setIniatingOffice(this);
							new_return_letter.setInitDay(day);
							new_return_letter.setIntendedDest(letter.getIniatingOffice().getName());
							new_return_letter.setRecipient(letter.getReturnRecipient());
							this.toMail.add(new_return_letter);
							Logging.newDeliverable(LogType.OFFICE, new_return_letter);
							Logging.deliverableAccepted(LogType.OFFICE, new_return_letter);
							toPickUp.remove(index);
							break;
						}
					}
					Logging.deliverableDestroyed(LogType.OFFICE, toPickUp.get(index));
					Logging.deliverableDestroyed(LogType.MASTER, toPickUp.get(index));
					toPickUp.remove(index);
					break;
				}
				if(index == toPickUp.size()-1)
					no_more_drop = true;
			}
		}
	}

	public void dropDestroyAll(){
		for(int index=0;index<toPickUp.size();index++){
			Deliverable d = toPickUp.get(index);
			Logging.deliverableDestroyed(LogType.OFFICE, d);
			Logging.deliverableDestroyed(LogType.MASTER, d);
		}
		toPickUp.removeAll(toPickUp);
		this.destroy();
		Logging.officeDestroy(LogType.MASTER, this.name);
		Logging.officeDestroy(LogType.OFFICE, this.name);
	}
	
	public void delay(String name,int delay){
		boolean no_more_items = false;
		int index = 0;
		while(!no_more_items){
			if(toPickUp.size()==0 || index>= toPickUp.size()-1)
				no_more_items = true;
			for(index = 0;index<toPickUp.size();index++){
				if(toPickUp.get(index).getRecipient().equals(name)){
					Deliverable d = toPickUp.remove(index);
					d.setDelay(delay);
					this.network.put(d);
					break;
				}
			}
		}
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
