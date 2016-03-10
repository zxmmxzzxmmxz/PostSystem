package com.sfu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Logging {
	public static enum LogType {
		MASTER, FRONT, OFFICE
	};
	static PrintWriter masterWriter;
	static PrintWriter frontWriter;
	static Map<String, PrintWriter> officeWriterMap = new HashMap<>();
	public static void initialize (Set<Office> offices) throws FileNotFoundException, UnsupportedEncodingException {
		String baseDir = System.getProperty("user.dir");
		new File("./output").mkdir();
		masterWriter = new PrintWriter(baseDir + "\\Output\\log_master.txt", "UTF-8");
		frontWriter = new PrintWriter(baseDir + "\\Output\\log_front.txt", "UTF-8");

		for (Office o : offices) {
			PrintWriter writer = new PrintWriter(baseDir +"\\Output\\log_" + o.getName() + ".txt", "UTF-8");
			officeWriterMap.put(o.getName(), writer);
		}
	}

	static private PrintWriter getWriter (LogType type, String officeName) {
		if (type == LogType.MASTER) {
			return masterWriter;
		}
		if (type == LogType.FRONT) {
			return frontWriter;
		}
		if (type == LogType.OFFICE && officeName != null) {
			return officeWriterMap.get(officeName);
		}
		return null;
	}

	static public void endOfDay(LogType type, int day, String officeName) {
		PrintWriter w = getWriter(type, officeName);
		if (w != null) {
			w.println("- - DAY " + day + " OVER - -");
		}
	}

	static public void newDeliverable(LogType type, Deliverable d) {
		String src = d.getIniatingOffice().getName();
		String dest = d.getDestOffice() != null ? d.getDestOffice().getName() : d.getIntendedDest();
		PrintWriter w = getWriter(type, src);
		if (w != null) {
			if (d instanceof Letter) {
				w.println("- New letter -");
			} else {
				w.println("- New package -");
			}
			w.println("Source: " + src);
			w.println("Destination: " + dest);
		}
	}

	static public void rejectDeliverable(LogType type, Deliverable d) {
		String src = d.getIniatingOffice().getName();
		String dest = d.getDestOffice() != null ? d.getIntendedDest() : d.getDestOffice().getName();
		  
		PrintWriter w = getWriter(type, src);
		if (w != null) {
			if (d instanceof Letter) {
				w.println("- Rejected letter -");
			} else {
				w.println("- Rejected package -");
			}
			w.println("Source: " + dest);
		}
	}

	static public void deliverableAccepted(LogType type, Deliverable d) {
		String src = d.getIniatingOffice().getName();
		String dest = d.getDestOffice().getName();
		PrintWriter w = getWriter(type, src);
		if (w != null) {
			if (d instanceof Letter) {
				w.println("- Accepted letter -");
			} else {
				w.println("- Accepted package -");
			}
			w.println("Destination: " + dest);
		}
	}

	static public void deliverableDestroyed(LogType type, Deliverable d) {
		String dest = d.getDestOffice().getName();
		PrintWriter w = getWriter(type, dest);
		if (w != null) {
			if (d instanceof Letter) {
				w.println("- Incinerated letter -");
			} else {
				w.println("- Incinerated package -");
			}
			w.println("Destroyed at: " + dest);
		}
	}

	static public void briberyDetected(LogType type, Deliverable d) {
		String src = d.getDestOffice().getName();
		PrintWriter w = getWriter(type, src);
		if (w != null) {
            w.println("- Something funny going on... -");
			w.println("Where did that extra money at " + src + " come from?");
		}
	}

	static public void itemComplete(LogType type, Deliverable d, int day) {
		String src = d.getDestOffice().getName();
		PrintWriter w = getWriter(type, src);
		if (w != null) {
            w.println("- Delivery process complete -");
			w.println("Delivery took " + (day - d.getInitDay()) + " days");
		}
	}

	static public void transitSent (LogType type,  Deliverable d) {
		PrintWriter w = getWriter(type, d.getIniatingOffice().getName());
		if (w != null) {
            w.println("- Standard transit departure -");
		}
	}

	static public void transitArrived (LogType type, Deliverable d) {
		PrintWriter w = getWriter(type, d.getDestOffice().getName());
		if (w != null) {
            w.println("- Standard transit arrival -");
		}
	}

	static public void criminalAppended (LogType type, String criminalName, String office) {
		PrintWriter w = getWriter(type, null);
		if (w != null) {
            w.println("- Criminal appended -");
            w.println("Criminal name: " + criminalName + " at office: " + office);
		}
	}

	static public void cleanUp() {
		masterWriter.close();
		frontWriter.close();
		for(Entry<String, PrintWriter> entry : officeWriterMap.entrySet()) {
			entry.getValue().close();
		}
	}
}
