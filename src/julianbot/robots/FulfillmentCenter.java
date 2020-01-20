package julianbot.robots;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import julianbot.robotdata.FulfillmentCenterData;

public class FulfillmentCenter extends Robot {

	private FulfillmentCenterData fulfillmentCenterData;
	
	public FulfillmentCenter(RobotController rc) {
		super(rc);
		this.data = new FulfillmentCenterData(rc, getSpawnerLocation());
		this.fulfillmentCenterData = (FulfillmentCenterData) this.data;
	}

	@Override
	public void run() throws GameActionException {
		super.run();
		
		if(turnCount == 1) {
			learnHQLocation();
			determineEdgeState();
		}
		
		if(turnCount < GameConstants.INITIAL_COOLDOWN_TURNS) {
    		for(int i = (rc.getRoundNum() > 100) ? rc.getRoundNum() - 100 : 1; i < rc.getRoundNum(); i++)
    		readTransaction(rc.getBlock(i));
    	}

    	readTransaction(rc.getBlock(rc.getRoundNum() - 1));
		
		if(!fulfillmentCenterData.isStableSoupIncomeConfirmed()) confirmStableSoupIncome();
    	if(oughtBuildDrone()) tryBuild(RobotType.DELIVERY_DRONE);
	}
	
	private void learnHQLocation() throws GameActionException {
		for(Transaction transaction : rc.getBlock(1)) {
			int[] message = decodeTransaction(transaction);
			if(message.length > 1 && message[1] == Type.TRANSACTION_FRIENDLY_HQ_AT_LOC.getVal()) {
				fulfillmentCenterData.setHqLocation(new MapLocation(message[2], message[3]));
				return;
			}
		}		
	}

	private void determineEdgeState() {
		MapLocation hqLocation = fulfillmentCenterData.getHqLocation();
		
		boolean leftEdge = hqLocation.x <= 0;
		boolean rightEdge = hqLocation.x >= rc.getMapWidth() - 1;
		boolean topEdge = hqLocation.y >= rc.getMapHeight() - 1;
		boolean bottomEdge = hqLocation.y <= 0;
		fulfillmentCenterData.setBaseOnEdge(leftEdge || rightEdge || topEdge || bottomEdge);
		
		if(leftEdge) {
			//The HQ is next to the western wall.
			if(bottomEdge) fulfillmentCenterData.setWallOffsetBounds(0, 2, 0, 3);
			else if(topEdge) fulfillmentCenterData.setWallOffsetBounds(0, 2, -3, 0);
			else fulfillmentCenterData.setWallOffsetBounds(0, 2, -1, 3);
		} else if(rightEdge) {
			//The HQ is next to the eastern wall.
			if(bottomEdge) fulfillmentCenterData.setWallOffsetBounds(-2, 0, 0, 3);
			else if(topEdge) fulfillmentCenterData.setWallOffsetBounds(-2, 0, -3, 0);
			else fulfillmentCenterData.setWallOffsetBounds(-2, 0, -3, 1);
		} else if(topEdge) {
			//The HQ is next to the northern wall, but not cornered.
			fulfillmentCenterData.setWallOffsetBounds(-1, 3, 0, -2);
		} else if(bottomEdge) {
			//The HQ is next to the southern wall, but not cornered.
			fulfillmentCenterData.setWallOffsetBounds(-3, 1, 0, 2);
		} else {
			fulfillmentCenterData.setWallOffsetBounds(-2, 2, -2, 2);
		}
	}
	
	private boolean oughtBuildDrone() {
		if(fulfillmentCenterData.isStableSoupIncomeConfirmed()) {
			MapLocation hqLocation = fulfillmentCenterData.getHqLocation();
			
			RobotInfo[] landscapers = senseAllUnitsOfType(RobotType.LANDSCAPER, rc.getTeam());
			int xMin = fulfillmentCenterData.getWallOffsetXMin();
			int xMax = fulfillmentCenterData.getWallOffsetYMax();
			int yMin = fulfillmentCenterData.getWallOffsetYMin();
			int yMax = fulfillmentCenterData.getWallOffsetYMax();
			
			for(RobotInfo landscaper : landscapers) {
				MapLocation location = landscaper.getLocation();
				int dx = location.x - hqLocation.x;
				int dy = location.y - hqLocation.y;
				
				boolean xInWall = xMin < dx && dx < xMax;
				boolean yInWall = yMin < dy && dy < yMax;
				
				//A landscaper inside the wall is an attack landscaper. We ought to build a drone to pair with it.
				if(xInWall && yInWall) return true;
			}
			
			//Otherwise, we can still produce scouting drones if we need to.
			return (!fulfillmentCenterData.isEnemyHqLocated()) ? rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + 1 : false;
		} else {
			//If a stable soup income is not confirmed, give the miners time to build a refinery before allocating soup to drones.
			return rc.getTeamSoup() >= RobotType.VAPORATOR.cost + 5;
		}
	}
	
	/**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    private boolean tryBuild(RobotType type) throws GameActionException {
    	Direction buildDirection = fulfillmentCenterData.getBuildDirection();
    	
    	waitUntilReady();
        if (rc.isReady() && rc.canBuildRobot(type, buildDirection)) {
            rc.buildRobot(type, buildDirection);
            if(type == RobotType.DELIVERY_DRONE) fulfillmentCenterData.incrementDronesBuilt();
            return true;
        } 
        
        return false;
    }
    
    private void confirmStableSoupIncome() throws GameActionException {
    	if(senseUnitType(RobotType.VAPORATOR, rc.getTeam()) != null) {
    		fulfillmentCenterData.setStableSoupIncomeConfirmed(true);
    	}
    	
    	/*
    	for(int i = fulfillmentCenterData.getTransactionRound(); i < rc.getRoundNum(); i++) {
    		for(Transaction transaction : rc.getBlock(i)) {
    			int[] message = decodeTransaction(transaction);
    			if(message.length >= 4) {
    				if(message[1] == Robot.Type.TRANSACTION_FRIENDLY_REFINERY_AT_LOC.getVal()) {
    					fulfillmentCenterData.setStableSoupIncomeConfirmed(true);
    					System.out.println("Stable soup income confirmed!");
    					return;
    				}
    			}
    		}
    		
    		if(Clock.getBytecodesLeft() <= 200) {
    			fulfillmentCenterData.setTransactionRound(i);
    			break;
    		}
    	}
    	*/
    }
    
    private void readTransaction(Transaction[] block) throws GameActionException {

		for (Transaction message : block) {
			int[] decodedMessage = decodeTransaction(message);
			if (decodedMessage.length == GameConstants.NUMBER_OF_TRANSACTIONS_PER_BLOCK) {
				Robot.Type category = Robot.Type.enumOfValue(decodedMessage[1]);

				//System.out.println("Category of message: " + category);
				switch(category) {
					case TRANSACTION_ENEMY_HQ_AT_LOC:
						fulfillmentCenterData.setEnemyHqLocated(true);
						break;
					default:
						break;
				}
			}

		}
	}
	
}
