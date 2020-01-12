package julianbot.commands;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import julianbot.robotdata.MinerData;

public class MinerCommands {
	
	static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	
	public static void discernRole(RobotController rc, MinerData data) throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam());
		for(RobotInfo robot : robots) {
			if(robot.type == RobotType.LANDSCAPER) {
				data.setCurrentRole(MinerData.ROLE_DEFENSE_BUILDER);
				return;
			}
		}
	}
	
	public static boolean locateNearbyDesignSchool(RobotController rc) throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam());
		for(RobotInfo robot : robots) {
			if(robot.type == RobotType.DESIGN_SCHOOL) return true;
		}
		
		return false;
	}
	
	public static boolean attemptDesignSchoolConstruction(RobotController rc) throws GameActionException {
		for(Direction buildDirection : directions) {
			if(rc.canBuildRobot(RobotType.DESIGN_SCHOOL, buildDirection)) {
				rc.buildRobot(RobotType.DESIGN_SCHOOL, buildDirection);
				return true;
			}
		}
		
		System.out.println("Failed to build design school...");
		
		return false;
	}

	//TODO Should be unnecessary once communication is fully running. Should remove if running into bytecode limit
	public static Direction getAdjacentRefineryDirection(RobotController rc) throws GameActionException {
		RobotInfo refinery = GeneralCommands.senseUnitType(rc, RobotType.REFINERY, rc.getTeam(), 3);
		RobotInfo hq = GeneralCommands.senseUnitType(rc, RobotType.HQ, rc.getTeam(), 3);

		if (refinery != null) {
			return rc.getLocation().directionTo(refinery.getLocation());
		} else if (hq != null) {
			return rc.getLocation().directionTo(hq.getLocation());
		} else {
			return Direction.CENTER;
		}
	}

	//TODO Should be unnecessary once communication is fully running. Should remove if running into bytecode limit
	public static Direction getAnyRefineryDirection(RobotController rc) throws GameActionException {
		RobotInfo refinery = GeneralCommands.senseUnitType(rc, RobotType.REFINERY, rc.getTeam());
		RobotInfo hq = GeneralCommands.senseUnitType(rc, RobotType.HQ, rc.getTeam());

		if (refinery != null) {
			return rc.getLocation().directionTo(refinery.getLocation());
		} else if (hq != null) {
			return rc.getLocation().directionTo(hq.getLocation());
		}
		else {
			return Direction.CENTER;
		}
	}
	
	public static boolean attemptRefineryConstruction(RobotController rc) throws GameActionException {
		for(Direction buildDirection : directions) {
			if(rc.canBuildRobot(RobotType.REFINERY, buildDirection)) {
				rc.buildRobot(RobotType.REFINERY, buildDirection);
				return true;
			}
		}
		
		System.out.println("Failed to build refinery...");
		
		return false;
	}
	
	public static void depositRawSoup(RobotController rc, Direction dir) throws GameActionException {
		if(rc.canDepositSoup(dir)) rc.depositSoup(dir, rc.getSoupCarrying());
	}

	/**
	 * Finds location with the most soup within 1 tile radius
	 * @param rc
	 * @return
	 * @throws GameActionException
	 */
	public static Direction getAdjacentSoupDirection(RobotController rc) throws GameActionException {
		Direction mostSoupDirection = Direction.CENTER;
		int mostSoupLocated = 0;
		
		for(Direction searchDirection : directions) {
			if (rc.canSenseLocation(rc.adjacentLocation(searchDirection).add(searchDirection))) {
				int foundSoup = rc.senseSoup(rc.adjacentLocation(searchDirection));
				mostSoupDirection = foundSoup > mostSoupLocated ? searchDirection : mostSoupDirection;
			}
		}
		
		return mostSoupDirection;
	}

	/**
	 * Finds location with the most soup within a 2 tile radius
	 * @param rc
	 * @return
	 * @throws GameActionException
	 */
	public static Direction getDistantSoupDirection(RobotController rc) throws GameActionException {
		Direction mostSoupDirection = Direction.CENTER;
		int mostSoupLocated = 0;
		
		for(Direction searchDirection : directions) {
			if (rc.canSenseLocation(rc.adjacentLocation(searchDirection).add(searchDirection))) {
				int foundSoup = rc.senseSoup(rc.adjacentLocation(searchDirection).add(searchDirection));
				mostSoupDirection = foundSoup > mostSoupLocated ? searchDirection : mostSoupDirection;
			}
		}
		
		return mostSoupDirection;
	}

	/**
	 * Returns location of soup within two radius of robot. If not found, will return null.
	 * @param rc
	 * @return
	 * @throws GameActionException
	 */
	public static MapLocation getSoupLocation(RobotController rc) throws GameActionException{
		Direction soupDir = MinerCommands.getAdjacentSoupDirection(rc);
		MapLocation soupLoc = rc.adjacentLocation(soupDir);
		if (soupDir == Direction.CENTER) {
			soupDir = MinerCommands.getDistantSoupDirection(rc);
			if (soupDir != Direction.CENTER) {
				//Now checks non-adjacent tiles
				soupLoc = rc.adjacentLocation(soupDir).add(soupDir);
			}
		}
		return (rc.senseSoup(soupLoc) > 0) ? soupLoc : null;
	}

	/**
	 * Mines soup if able
	 * @param rc Robot Controller
	 * @param dir Direction
	 * @throws GameActionException
	 */
	public static boolean mineRawSoup(RobotController rc, Direction dir) throws GameActionException {
		if(rc.isReady() && rc.canMineSoup(dir) && rc.getSoupCarrying() != RobotType.MINER.soupLimit) {
			rc.mineSoup(dir);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Moves in same direction as before, otherwise moves in random direction
	 * @param rc
	 * @param data
	 * @throws GameActionException
	 */
	public static void continueSearch(RobotController rc, MinerData data) throws GameActionException {
		//The move function is deliberately unused here.
		if(!rc.isReady()) return;
		if(rc.canMove(data.getSearchDirection()) && !rc.senseFlooding(rc.getLocation().add(data.getSearchDirection()))) {
			rc.move(data.getSearchDirection());
			return;
		}
		data.setSearchDirection(directions[(int) (Math.random() * directions.length)]);


	}
	
	public static boolean routeToFulfillmentCenterSite(RobotController rc, MinerData minerData) throws GameActionException {
		if(!minerData.hasPath()) {
			return GeneralCommands.pathfind(minerData.getSpawnerLocation().add(rc.getLocation().directionTo(minerData.getSpawnerLocation())), rc, minerData);
    	}
    	
		return GeneralCommands.pathfind(null, rc, minerData);
	}
	
	public static boolean buildDefenseFulfillmentCenter(RobotController rc, MinerData minerData) throws GameActionException {
		MapLocation hqLocation = minerData.getSpawnerLocation().add(Direction.EAST);
		Direction buildDirection = rc.getLocation().directionTo(hqLocation);
		if(rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, buildDirection)) {
			rc.buildRobot(RobotType.FULFILLMENT_CENTER, buildDirection);
			return true;
		}
		
		System.out.println("Failed to build fulfillment center.");
		
		return false;
	}
	
}
