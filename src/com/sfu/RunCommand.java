package com.sfu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sfu.Logging.LogType;

public class RunCommand {
	private static Set<Office> offices = new HashSet<>();
	private static Set<String> wanted;
	private static Network network;

	private static String baseDir;
	private static String commandsFilePath, officesFilePath, wantedFilePath;

	private static Office getOffice(String officeName) {
		for (Office o : offices) {
		    if (o.getName().equals(officeName)) {
				return o;
			}
		}
		return null;
	}

	private static List<String> readFileIntoLine(String path) throws Exception {
		int count = 0;
		List<String> lines = new ArrayList<>();
		File file = new File(path);
		FileReader fileReader = new FileReader(file);

		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line;
		int index = 0;
		while ((line = bufferedReader.readLine()) != null) {
			if (index == 0) {
				count = Integer.parseInt(line);
			} else if (line != null && line.length() != 0) {
				lines.add(line);
			}
			index ++;
		}
		fileReader.close();

		if (lines.size() != count) {
			throw new Exception ("Record number does not match: " + path);
		}
		return lines;
	}

	private static Set<Office> initOffices(String path) throws Exception {
		Set<Office> offices = new HashSet<>();
		List<String> lines = readFileIntoLine(path);
		for (String line : lines) {
			String[] parts = line.split(" ");
			if (parts.length == 6) {
				Office o = new Office(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
						Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]));
				offices.add(o);
			}
		}
		return offices;
	}

	private static Set<String> initWanted(String path) throws Exception {
		Set<String> wanted = new HashSet<>();
		List<String> lines = readFileIntoLine(path);
		for (String line : lines) {
			wanted.add(line.trim());
		}
		return wanted;
	}

	private static void prepareFiles() {
		baseDir = System.getProperty("user.dir");
		commandsFilePath = baseDir + "\\commands.txt";
		officesFilePath = baseDir + "\\offices.txt";
		wantedFilePath = baseDir + "\\wanted.txt";
	}

	public static void main(String[] args) throws Exception {
		prepareFiles();
		List<String> commands;
		network = new Network();
		try {
			wanted = initWanted(wantedFilePath);
			offices = initOffices(officesFilePath);
			for (Office o : offices) {
				o.setWanted(wanted);
				o.setNetwork(network);
			}
			network.populateOffices(offices);
			commands = readFileIntoLine(commandsFilePath);
		} catch (Exception e) {
			//File reading problem, exit the program
			throw new Exception("Problem happened", e);
		}

		//Initialize Logging
		Logging.initialize(offices);

		int idx = 0;
		int day = 1;
		boolean goodday = false;
		ArrayList<Integer> indexForDays = new ArrayList<>();
		indexForDays.add(0);
		boolean hasPendingDeliverables = hasPendingDeliverables();
		boolean sneak=false;

		while (idx < commands.size() || hasPendingDeliverables) {
			if(idx == commands.size())
				break;
			//Start of the day, check if any in transit items have arrived
			network.checkAndDeliver(day);

			while(idx< commands.size()) {
				String cmd = commands.get(idx);
				idx ++;
				if (isDayCommand(cmd)) {
					indexForDays.add(idx);
					break;
				}
				
				if(isGoodCommand(cmd) && goodday){
					indexForDays.add(idx);
					while(!isDayCommand(commands.get(idx))){
						idx++;
					}
					idx++;
					Logging.goodDay();
					break;
				}
				
				String[] parts = cmd.split(" ");
				if (isPickupCommand(cmd)) {
					String dest = parts[1];
					String recipient = parts[2].trim();
					if (wanted.contains(recipient)) {
						Logging.criminalAppended(LogType.FRONT, recipient, dest);
					} else {
						Office office = getOffice(dest);
						goodday = office.pickUp(recipient, day);
					}
				} else if (isLetterCommand(cmd)) {
					String src = parts[1];
					String recipient = parts[2];
					String dest = parts[3];
					String returnRecipient = parts[4];
					Office srcOffice = getOffice(src);
					Office destOffice = getOffice(dest);

					Letter letter = new Letter();
					letter.setIniatingOffice(srcOffice);
					letter.setDestOffice(destOffice);
					letter.setInitDay(day);
					letter.setRecipient(recipient);
					letter.setReturnRecipient(returnRecipient);
					letter.setIntendedDest(dest);

					Logging.newDeliverable(LogType.OFFICE, letter);

					boolean hasCriminalRecipient = wanted.contains(letter.getRecipient());
					boolean officeFull = srcOffice.isFull();
					if (destOffice != null && !hasCriminalRecipient && !officeFull && !destOffice.is_destroy()) {
						srcOffice.accept(letter);
					} else {
						Logging.rejectDeliverable(LogType.MASTER, letter);
						Logging.rejectDeliverable(LogType.OFFICE, letter);
					}
				} else if (isPackageCommand(cmd)) {
					String src = parts[1];
					String recipient = parts[2];
					String dest = parts[3];
					int money = Integer.parseInt(parts[4]);
					int length = Integer.parseInt(parts[5]);

					Office srcOffice = getOffice(src);
					Office destOffice = getOffice(dest);

					Package pkg = new Package();
					pkg.setIniatingOffice(srcOffice);
					pkg.setDestOffice(destOffice);
					pkg.setInitDay(day);
					pkg.setRecipient(recipient);
					pkg.setLength(length);
					pkg.setMoney(money);
					pkg.setIntendedDest(dest);

					Logging.newDeliverable(LogType.OFFICE, pkg);

					boolean hasCriminalRecipient = wanted.contains(pkg.getRecipient());
					boolean officeFull = srcOffice.isFull();
					boolean lengthFitSrc = (length < srcOffice.getMaxPackageLength());
					if (sneak || !hasCriminalRecipient && !officeFull
							&& lengthFitSrc && destOffice != null && (length <= destOffice.getMaxPackageLength()) && pkg.getMoney() >= srcOffice.getRequiredPostage()) {
						srcOffice.accept(pkg);
						if(sneak)
							sneak=false;
					} else if (pkg.getMoney() >= (srcOffice.getRequiredPostage() + srcOffice.getPersuasionAmount())) {
						Logging.briberyDetected(LogType.MASTER, pkg);
						srcOffice.accept(pkg);
					} else {
						Logging.rejectDeliverable(LogType.MASTER, pkg);
						Logging.rejectDeliverable(LogType.OFFICE, pkg);
					}
				}
				else if(isBuildCommand(cmd)){
					String[] things = cmd.split(" ");
					if (things.length == 7) {
						Office existingOffice = null;
						for(Office o : offices){
							if(o.getName().equals(things[1]))
								existingOffice = o;
						}
						if(existingOffice==null){
							Office o = new Office(things[1], Integer.parseInt(things[2]), Integer.parseInt(things[3]),Integer.parseInt(things[4]), Integer.parseInt(things[5]), Integer.parseInt(things[6]));
							offices.add(o);
							network.populateOffices(o);
							Logging.addOffice(o);
						}
						else{
							if(!existingOffice.is_destroy()){
								//destroy logging
								Logging.officeDestroy(LogType.MASTER, things[1]);
								Logging.officeDestroy(LogType.OFFICE, things[1]);
							}
						}
						//build loggging
						Logging.buildOffice(LogType.MASTER, things[1]);
						Logging.buildOffice(LogType.OFFICE, things[1]);
					}
				}
				else if(isScienceCommand(cmd)){
					int interval = Integer.parseInt(parts[1]);
					if(interval<0){
						if(day+interval<0){
							for(Office o:offices){
								o.dropDestroyAll();
							}
							Logging.cleanUp();
							return;
						}
						commands.remove(idx-1);
						idx = indexForDays.get(day+interval);							
						break;
					}
					else if(interval>0){
						int day_wanted=idx;
						while(interval>0){
							while(!isDayCommand(commands.get(idx))){
								idx++;
								if(idx>=commands.size()){
									for(Office o:offices){
										o.dropDestroyAll();
									}
									Logging.cleanUp();
									return;
								}
							}
							day_wanted = idx+1;
							idx++;
							interval--;
						}
						idx=day_wanted;
						continue;
					}
				}
				else if(isDelayCommand(cmd)){
					//Delay
					String receipt = parts[1];
					int delay = Integer.parseInt(parts[2]);
					for(Office o:offices){
						o.delay(receipt, delay);
					}
					network.delay(receipt, delay);
				}
				else if(cmd.startsWith("SNEAK")){
					sneak = true;
					continue;
				}
				else if(cmd.startsWith("INFLATION")){
					for(Office o:offices){
						o.setRequiredPostage(o.itemsInTotal()+o.getRequiredPostage());
						o.setPersuasionAmount(o.itemsInTotal()+o.getPersuasionAmount());
					}
				}
				else if(cmd.startsWith("DEFLATION")){
					for(Office o:offices){
						int amount = o.itemsInTotal();
						int postage = o.getRequiredPostage();
						int persuasion = o.getPersuasionAmount();
						o.setRequiredPostage( (postage-amount>=0)? postage-amount : 0);
						o.setPersuasionAmount( (persuasion-amount>=0)? persuasion-amount : 0);
					}
				}
			}
			//End of the day.
			for (Office o : offices) {
				// Remove deliverables longer than 14 days
				o.drop(day);
				// Send accepted deliverables
				o.sendToNetwork();
			}
			//End of the day. Log end of day.
			Logging.endOfDay(LogType.MASTER, day, null);
			for (Office o : offices) {
				Logging.endOfDay(LogType.OFFICE, day, o.getName());
			}

			hasPendingDeliverables = hasPendingDeliverables();
			//Ready for next day
			day++;
		}

		Logging.cleanUp();

	}

	static boolean hasPendingDeliverables() {
		//Checks if in network, there are any deliverables.
		//Checks if in offices, if there are any deliverables.
		boolean hasPendingDeliverables = false;
		if (!network.isNetworkEmpty()) {
			hasPendingDeliverables = true;
		}
		if (!hasPendingDeliverables) {
			for (Office o : offices) {
				if (!o.isEmpty()) {
					hasPendingDeliverables = true;
				}
			}
		}
		return hasPendingDeliverables;
	}
  
	static boolean isDayCommand(String command) {
		return command.startsWith("DAY");
	}

	static boolean isPickupCommand(String command) {
		return command.startsWith("PICKUP");
	}

	static boolean isLetterCommand(String command) {
		return command.startsWith("LETTER");
	}

	static boolean isPackageCommand(String command) {
		return command.startsWith("PACKAGE");
	}
	
	static boolean isBuildCommand(String command){
		return command.startsWith("BUILD");
	}
	
	static boolean isScienceCommand(String command){
		return command.startsWith("SCIENCE");
	}
	
	static boolean isGoodCommand(String command){
		return command.startsWith("GOOD");
	}
	
	static boolean isDelayCommand(String command){
		return command.startsWith("NSADELAY");
	}
}
