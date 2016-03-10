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
			if (d.getInitDay() + initOffice.getTransitTime() + 2 >= day) {
				Office destOffice = d.getDestOffice();
				Logging.transitArrived(LogType.MASTER, d);
				deliverablesInTransit.remove(idx);
				//put the deliverable into this office
				destOffice.receiveFromNetwork(d);
			}
		}
	}

	public void populateOffices(Set<Office> offices) {
		for (Office o : offices) {
			officeMap.put(o.getName(), o);
		}
	}

	public boolean isNetworkEmpty() {
		return deliverablesInTransit.size() == 0;
	}
}
