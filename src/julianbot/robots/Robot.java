package julianbot.robots;

import java.util.ArrayList;
import java.util.List;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Transaction;
import julianbot.robotdata.RobotData;
import julianbot.utils.pathfinder.Pathfinder;

import javax.print.attribute.standard.Destination;

public class Robot {
	
	protected RobotController rc;
	protected RobotData data;
	
	protected static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	
	protected int spawnRound;
	protected int turnCount;
	
	public enum Type{
		TRANSACTION_SOS_AT_LOC(-104),
		TRANSACTION_SOUP_AT_LOC(249),
		TRANSACTION_FRIENDLY_HQ_AT_LOC(9),
		TRANSACTION_ENEMY_HQ_AT_LOC(29),
		TRANSACTION_FRIENDLY_REFINERY_AT_LOC(-477),
		TRANSACTION_ENEMY_REFINERY_AT_LOC(-443),
		TRANSACTION_FRIENDLY_DESIGN_SCHOOL_AT_LOC(793), // Not sure if we'd ever need this, but putting it just in case
		TRANSACTION_ENEMY_DESIGN_SCHOOL_AT_LOC(740),
		TRANSACTION_FRIENDLY_FULFILLMENT_CENTER_AT_LOC(117), // Not sure if we'd ever need this, but putting it just in case
		TRANSACTION_ENEMY_FULFILLMENT_CENTER_AT_LOC(177),
		TRANSACTION_ATTACK_AT_LOC(-171),
		TRANSACTION_KILL_ORDER(88),
		TRANSACTION_PAUSE_LANDSCAPER_BUILDING(8482);
		
		private int val;
		
		Type(int val){
			this.val = val;
		}
		
		public int getVal() {
			return val;
		}
		
		public static Type enumOfValue(int value) {
		    for (Type e : values()) {
		        if (e.val==value) {
		            return e;
		        }
		    }
		    return null;
		}
		//Call Type.enumOfValue(plaintxt[1]) to get enum from value
	}
	
	public Robot(RobotController rc) {
		this.rc = rc;
		this.spawnRound = rc.getRoundNum();
	}
	
	public RobotController getRobotController() {
		return rc;
	}

	public void setRobotController(RobotController rc) {
		this.rc = rc;
	}

	public RobotData getRobotData() {
		return data;
	}

	public void setRobotData(RobotData data) {
		this.data = data;
	}
	
	public void run() throws GameActionException {
		sendPendingTransaction();
    	updateTurnCount();
	}

	public void updateTurnCount() {
		 turnCount = rc.getRoundNum() - spawnRound + 1;
	}
	
	//FLOODING
	protected int getFloodingAtRound(double roundNumber) {
		return (int) (Math.pow(Math.E, (0.0028 * roundNumber) - (1.38 * Math.sin(0.00157 * roundNumber - 1.73)) + (1.38 * Math.sin(-1.73))) - 1);
	}
	
	//RECONNAISSANCE
	protected MapLocation getSpawnerLocation() {
		RobotInfo[] robots = rc.senseNearbyRobots(3, rc.getTeam());
		RobotType targetType = getSpawnerTypeFor(rc.getType());
		
		for(RobotInfo robot : robots) {
			if(robot.type == targetType) return robot.getLocation();
		}
		
		return null;
	}
	
	protected RobotType getSpawnerTypeFor(RobotType type) {
		if(type == RobotType.MINER) return RobotType.HQ;
		if(type == RobotType.REFINERY) return RobotType.MINER;
		if(type == RobotType.LANDSCAPER) return RobotType.DESIGN_SCHOOL;
		return RobotType.HQ;
	}
	
	/**
	 * Senses unit type within sensor radius.
	 * @param rc
	 * @param type
	 * @return First unit of given type. Null if not found
	 */
	protected RobotInfo senseUnitType(RobotType type) {
		RobotInfo[] robots = rc.senseNearbyRobots();
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				return robot;
			}
		}
		return null;
	}
	
	/**
	 * Senses number of units matching given criteria within sensor radius
	 * @param rc
	 * @param type
	 * @return Number of units of given type
	 */
	protected int senseNumberOfUnits(RobotType type) {
		int unitCount = 0;
		
		RobotInfo[] robots = rc.senseNearbyRobots();
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				unitCount++;
			}
		}
		
		return unitCount;
	}

	/**
	 * Senses unit type within sensor radius.
	 * @param rc
	 * @param type
	 * @param team
	 * @return First unit of given type and team. Null if not found
	 */
	protected RobotInfo senseUnitType(RobotType type, Team team) {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, team);
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				return robot;
			}
		}
		return null;
	}
	
	protected RobotInfo[] senseAllUnitsOfType(RobotType type, Team team) {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, team);
		List<RobotInfo> matchingRobots = new ArrayList<>();
		
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				matchingRobots.add(robot);
			}
		}
		
		RobotInfo[] matchingRobotsArray = new RobotInfo[matchingRobots.size()];
		for(int i = 0; i < matchingRobots.size(); i++) {
			matchingRobotsArray[i] = matchingRobots.get(i);
		}
		
		return matchingRobotsArray;
	}
	
	/**
	 * Senses number of units matching given criteria within sensor radius
	 * @param rc
	 * @param type
	 * @param team
	 * @return Number of units of given type and team
	 */
	protected int senseNumberOfUnits(RobotType type, Team team) {
		int unitCount = 0;
		
		RobotInfo[] robots = rc.senseNearbyRobots(-1, team);
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				unitCount++;
			}
		}
		
		return unitCount;
	}

	/**
	 * Senses unit type within inputted radius
	 * @param rc
	 * @param type
	 * @param team
	 * @param radiusSquared
	 * @return First unit of given type and team. Null if not found.
	 */
	protected RobotInfo senseUnitType(RobotType type, Team team, int radiusSquared) {
		RobotInfo[] robots = rc.senseNearbyRobots(radiusSquared, team);
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				return robot;
			}
		}
		return null;
	}
	
	/**
	 * Senses number of units matching given criteria within inputted radius
	 * @param rc
	 * @param type
	 * @param team
	 * @param radiusSquared
	 * @return Number of units of given type and team
	 */
	protected int senseNumberOfUnits(RobotType type, Team team, int radiusSquared) {
		int unitCount = 0;
		
		RobotInfo[] robots = rc.senseNearbyRobots(radiusSquared, team);
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				unitCount++;
			}
		}
		
		return unitCount;
	}
	
	//TURN MANAGEMENT
	protected void waitUntilReady() {
		while(!rc.isReady()) {
			Clock.yield();
		}
	}
	
	//MOVEMENT
	protected boolean move(Direction dir) throws GameActionException {
		stopFollowingPath();
		MapLocation targetLocation = rc.getLocation().add(dir);
		
		waitUntilReady();
		
		if(rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(targetLocation)) {
			data.setPreviousLocation(rc.getLocation());
			rc.move(dir);
			return true;
		}
		
		return false;
	}
	
	protected boolean moveOnPath(Direction dir) throws GameActionException {
		MapLocation targetLocation = rc.getLocation().add(dir);
		
		waitUntilReady();
		
		if(rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(targetLocation)) {
			data.setPreviousLocation(rc.getLocation());
			rc.move(dir);
			return true;
		}
		
		return false;
	}
	
	protected boolean moveAnywhere() throws GameActionException {
		stopFollowingPath();
		
		Direction dir = directions[(int)(Math.random() * directions.length)];
		int rotateLimit = 8;
		
		waitUntilReady();
		
		while (!move(dir) && rotateLimit > 0) {
			dir.rotateRight();
			rotateLimit--;
		}
		
		if(rotateLimit > 0) data.setPreviousLocation(rc.getLocation().add(dir.opposite()));
		return rotateLimit > 0;
	}

	/**
	 * Moves in same direction as before, otherwise moves in random direction
	 * @throws GameActionException
	 */
	public void continueSearch() throws GameActionException {
		//The move function is deliberately unused here.
		waitUntilReady();

		if(rc.canMove(data.getSearchDirection()) && !rc.senseFlooding(rc.getLocation().add(data.getSearchDirection()))) {
			rc.move(data.getSearchDirection());
			return;
		}

		data.setSearchDirection(directions[(int) (Math.random() * directions.length)]);
	}

	/**
	 * Moves in search direction, returns false if unable to.
	 * @return
	 * @throws GameActionException
	 */
	public boolean continueSearchNonRandom() throws GameActionException {
		//The move function is deliberately unused here.
		waitUntilReady();

		if(rc.canMove(data.getSearchDirection()) && !rc.senseFlooding(rc.getLocation().add(data.getSearchDirection()))) {
			rc.move(data.getSearchDirection());
			return true;
		}
		return false;
	}

	//TRANSACTIONS
	protected boolean sendTransaction(int soupBid, Type type, MapLocation loc) throws GameActionException {		
		int transactionTag = (int) (Math.random()*500); //This use of parentheses will prevent truncation of the random number.
		int[] message = new int[]{transactionTag, type.getVal()+transactionTag, loc.x+transactionTag, loc.y+transactionTag, rc.getRoundNum()+transactionTag, 0, 0};
		int odd = 0;
		for (int i : message) {
			if (i%2 == 1)
				odd++;
		}
		message[6] = odd;
		
		if(rc.canSubmitTransaction(message, soupBid)) {
			rc.submitTransaction(message, soupBid);
			return true;
		} else {
			data.setPendingTransaction(type, loc, soupBid);
		}
		
		return false;
	}

	/**
	 * Decodes transaction. Returns empty array if message is from enemy team.
	 * @param transaction
	 * @return
	 * @throws GameActionException
	 */
	protected int[] decodeTransaction(Transaction transaction) throws GameActionException {		
		int[] message = transaction.getMessage();
		int transactionTag = message[0];
		int[] plaintxt = new int[7];
		
		int odd = 0;
		for (int i = 0; i<message.length-1; i++) {
			if (message[i]%2 == 1)
				odd++;
		}
		
		if (odd!=message[6]) return new int[] {0}; //empty means message not from own team
		for (int i = 0; i<message.length-1; i++) {
			plaintxt[i] = message[i] - transactionTag;
		}
		if (Type.enumOfValue(plaintxt[1]) == null) return new int[] {0}; //Checks if matches one of categories.
		return plaintxt;
	}

	/**
	 * Returns the corresponding Transaction type of locating inputted RobotType
	 * @param rc
	 * @param unitType
	 * @param unitTeam
	 * @return
	 */
	protected static Type getLocationType(RobotController rc, RobotType unitType, Team unitTeam) {
		if (unitTeam == rc.getTeam()) {
			switch (unitType) {
				case HQ:
					return Type.TRANSACTION_FRIENDLY_HQ_AT_LOC;
				case REFINERY:
					return Type.TRANSACTION_FRIENDLY_REFINERY_AT_LOC;
				case DESIGN_SCHOOL:
					return Type.TRANSACTION_FRIENDLY_DESIGN_SCHOOL_AT_LOC;
				case FULFILLMENT_CENTER:
					return Type.TRANSACTION_FRIENDLY_FULFILLMENT_CENTER_AT_LOC;
				default:
					return null;
			}
		} else {
			switch (unitType) {
				case HQ:
					return Type.TRANSACTION_ENEMY_HQ_AT_LOC;
				case REFINERY:
					return Type.TRANSACTION_ENEMY_REFINERY_AT_LOC;
				case DESIGN_SCHOOL:
					return Type.TRANSACTION_ENEMY_DESIGN_SCHOOL_AT_LOC;
				case FULFILLMENT_CENTER:
					return Type.TRANSACTION_ENEMY_FULFILLMENT_CENTER_AT_LOC;
				default:
					return null;
			}
		}
	}
	
	public void sendPendingTransaction() throws GameActionException {
		if(!data.hasPendingTransaction()) return;
		
		if(sendTransaction(data.getPendingTransactionSoupBid(), data.getPendingTransactionType(), data.getPendingTransactionLocation())) {
			System.out.println("Submitted pending transaction!");
			data.clearPendingTransaction();
		}
	}
	
	//PATHFINDING
	protected boolean routeTo(MapLocation destination) throws GameActionException {
		rc.setIndicatorLine(rc.getLocation(), destination, 0, 0, 255);
		//If we're already pathfinding, continue on.
		if(data.hasPath()) {
			boolean pathfindingSuccessful = pathfind(destination);
			if(!pathfindingSuccessful) data.setPath(null);
			return pathfindingSuccessful;
		}

		//Otherwise, simply try to move directly towards the destination.
		MapLocation rcLocation = rc.getLocation();
		/*
		Direction initialDirection = rc.getLocation().directionTo(destination);
		if(move(initialDirection)) return true;
		
		//If this isn't possible, try to move around whatever is blocking us.
		//Directions closer to the destination will be favored.
		Direction initialDirectionLeft = initialDirection.rotateLeft();
		Direction initialDirectionRight = initialDirection.rotateRight();
		
		Direction[] nextDirections = new Direction[4];
		
		if(rcLocation.add(initialDirectionLeft).distanceSquaredTo(destination) < rcLocation.add(initialDirectionRight).distanceSquaredTo(destination)) {
			nextDirections[0] = initialDirectionLeft;
			nextDirections[1] = initialDirectionLeft.rotateLeft();
			nextDirections[2] = initialDirectionRight;
			nextDirections[3] = initialDirectionRight.rotateRight();
		} else {
			nextDirections[0] = initialDirectionRight;
			nextDirections[1] = initialDirectionRight.rotateRight();
			nextDirections[2] = initialDirectionLeft;
			nextDirections[3] = initialDirectionLeft.rotateLeft();
		}
		
		for(Direction direction : nextDirections) {
			if(rcLocation.add(direction).equals(data.getPreviousLocation())) continue;
			if(move(direction)) return true;
		} */
		
		//TODO: MAXIMO'S DOMAIN DO NOT TOUCH (FUTURE SITE OF BUG NAV)
		if (bugNav(destination)) {return true;}

		
		//If all of these measures have failed, we'll need to use pathfinding to get around.
		//However, just in case, we will allow for the previous location to be used next turn.
		data.setPreviousLocation(rcLocation);
		return pathfind(destination);
	}

	/**
	 * Tries to move closer to destination. If it can't follows wall until it can move closer.
	 * @param destination
	 * @return
	 */
	public boolean bugNav(MapLocation destination) throws GameActionException {
		if (data.getClosestDist() == -1) {
			System.out.println("Initializing closestDist");
			data.setClosestDist(rc.getLocation().distanceSquaredTo(destination));
		}

		Direction dirToDest = rc.getLocation().directionTo(destination);
		rc.setIndicatorDot(rc.getLocation().add(dirToDest), 255, 182, 193); // Pink dot
		if (rc.getLocation().add(dirToDest).distanceSquaredTo(destination) < data.getClosestDist()) {
			// If the next move toward the destination is closer than the closest its been
			if (move(dirToDest)) {
				//If you can move in that direction
				data.setClosestDist(rc.getLocation().distanceSquaredTo(destination));
				System.out.println("Moved to new closest location. Dist: " + data.getClosestDist());

			} else if (rc.getLocation().add(dirToDest).equals(destination)) {
				//TODO Check if building is in the way
				// Prevents case where robot attempts to move onto occupied space which is its destination
				System.out.println("Adjacent to destination");
				data.setClosestDist(-1); // Should stop nav. But honestly probably doesn't
				return true;

			} else {
				followWall();
			}
		} else {
			followWall();
		}

		if (rc.getLocation().equals(destination)) {
			// After robot moves, checks if it is now at its destination
			System.out.println("Reached destination");
			data.setClosestDist(-1);
		}
		return true;
	}

	/**
	 * Attempts to move in same direction as last turn, otherwise rotates right
	 * @throws GameActionException
	 */
	public void followWall() throws GameActionException {

		System.out.println("Can't move in closer direction. Resorting to wall hugging.");
		// Follows wall on left side
		while (!continueSearchNonRandom()) {
			data.setSearchDirection(data.getSearchDirection().rotateRight());
		}
		data.setSearchDirection(data.getSearchDirection().rotateLeft().rotateLeft());
		rc.setIndicatorLine(rc.getLocation().subtract(data.getSearchDirection()), rc.getLocation(), 102, 255, 255); //Teal line
	}

	/**
	 *
	 * @param destination
	 * @param rc
	 * @param data
	 * @return
	 * @throws GameActionException
	 */
	protected boolean pathfind(MapLocation destination) throws GameActionException {
		if(!data.hasPath() && destination != null) {
			calculatePathTo(destination);
			return data.hasPath();
		}
		
		return proceedAlongPath();
	}
	
	protected void calculatePathTo(MapLocation destination) throws GameActionException {
		data.setMapGraph(Pathfinder.buildMapGraph(rc, rc.getCurrentSensorRadiusSquared()));
		data.setPath(Pathfinder.getRoute(rc.getLocation(), destination, data.getMapGraph()));
		data.setPathProgression(0);
	}
	
	protected boolean proceedAlongPath() throws GameActionException {
		if(data.hasPath() && moveOnPath(data.getNextPathDirection())) {
			data.incrementPathProgression();
			
			if(data.pathCompleted()) {
				stopFollowingPath();
			}
			
			return true;
		}
		
		stopFollowingPath();
		return false;
	}
	
	protected void stopFollowingPath() {
		data.setPath(null);
		data.setPathProgression(0);
	}

	static Direction randomDirection() {
		return Direction.allDirections()[(int) (Math.random() * Direction.allDirections().length)];
	}

	/**
	 * Returns closest location of array to target location
	 * @param rc
	 * @param locs
	 * @param targetLoc
	 * @return
	 */
	protected MapLocation locateClosestLocation(ArrayList<MapLocation> locs, MapLocation targetLoc) {
		MapLocation closestLoc = null;
		int closestDistance = Integer.MAX_VALUE;
		for (MapLocation loc : locs) {
			int dist = loc.distanceSquaredTo(targetLoc);
			if (dist < closestDistance) {
				closestLoc = loc;
				closestDistance = dist;
			}
		}
		return closestLoc;
	}

}
