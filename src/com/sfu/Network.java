package com.sfu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sfu.Logging.LogType;

public class Network {
	private List<Deliverable> deliverablesInTransit = new ArrayList<>();
	private Map<String, Office> officeMap = new HashMap<>();
	public void put(Deliverable d) {
		deliverablesInTransit.add(d);
	}

	public void checkAndDeliver(int day) {
		for (int idx = deliverablesInTransit.size() -1 ; idx >= 0 ; idx--) {
			Deliverable d = deliverablesInTransit.get(idx);
			Office initOffice = d.getIniatingOffice();
			if (d.getInitDay() + initOffice.getTransitTime() + 1 +d.getDelay() <= day) {
				Office destOffice = d.getDestOffice();
				deliverablesInTransit.remove(idx);
				//put the deliverable into this office
				if(destOffice.is_destroy()){
					if(d instanceof Letter && !((Letter) d).getReturnRecipient().equals("NONE")){
						Letter new_return_letter = new Letter();
						new_return_letter.setReturnRecipient("NONE");
						new_return_letter.setDestOffice(d.getIniatingOffice());
						new_return_letter.setIniatingOffice(d.getDestOffice());
						new_return_letter.setInitDay(day);
						new_return_letter.setIntendedDest(d.getIniatingOffice().getName());
						new_return_letter.setRecipient(((Letter) d).getReturnRecipient());
						this.put(new_return_letter);
					}
				}
				else{
					Logging.transitArrived(LogType.OFFICE, d);
					destOffice.receiveFromNetwork(d);
				}
			}
		}
	}

	public void delay(String name,int delay){
		for(int index = 0;index<deliverablesInTransit.size();index++){
			if(deliverablesInTransit.get(index).getRecipient().equals(name)){
				deliverablesInTransit.get(index).setDelay(delay);
			}
		}
	}

	public void populateOffices(Set<Office> offices) {
		for (Office o : offices) {
			officeMap.put(o.getName(), o);
		}
	}
	public void populateOffices(Office office) {
		officeMap.put(office.getName(), office);
	}

	public boolean isNetworkEmpty() {
		return deliverablesInTransit.size() == 0;
	}
}
