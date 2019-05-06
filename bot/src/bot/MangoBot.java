package bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Attack;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import rts.*;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class MangoBot extends AbstractionLayerAI 
{    
    private UnitTypeTable utt;
    private UnitType worker;
    private UnitType base;
    private UnitType barracks;
    
    private int enemyRaxCount;
    private int workersNear;
    
    //Number of tiles in which an enemy is considered nearby in relation to player's base and therefore a threat to the workers.
    //Actual distance is this radius * 2 to account for both axes.
    private int enemyCheckRadius = 5;
    
    private Unit myBase;
    
    
    /*--------------------
     * Unit lists 
     *------------------- */
    
    //All the player's workers.
    private List<Unit> myWorkers = new ArrayList<Unit>();
    
    //Player's workers that are harvesting.
    private List<Unit> myHarvestingWorkers = new ArrayList<Unit>();
    
    
    //All enemy units in the game.
    private List<Unit> allEnemyUnits = new ArrayList<Unit>();
    
    //All the enemy workers.
    private List<Unit> enemyWorkers = new ArrayList<Unit>();
    
    //All the enemy bases.
    private List<Unit> enemyBases = new ArrayList<Unit>();
    
    
    //Enemy units near base
    private List<Unit> nearbyEnemyUnits = new ArrayList<Unit>();
    
    //Enemy workers near base
    private List<Unit> nearbyEnemyWorkers = new ArrayList<Unit>();
    
    //Player workers who must always work.    
    private List<Unit> forcedHarvestingWorkers = new ArrayList<Unit>();
    
    
    //Single nearest enemy worker
    private Unit nearestEnemyWorker;
    
    //Current target when workers are attacking nearby enemy workers.
    int attackedWorkerIndex = 0;
    
    //Number of workers that must always be harvesting.
    int minimumHarvesterCount = 1;
    
    //Maximum number of workers to train.
    int maxWorkerTrain = 50;
    
    
    //Setup unit types
    public MangoBot(UnitTypeTable utt) 
    {
        super(new AStarPathFinding());
        this.utt = utt;
        
        worker = utt.getUnitType("Worker");
        base = utt.getUnitType("Base");
        barracks = utt.getUnitType("Barracks");
        
        enemyRaxCount = 0;
        
        enemyCheckRadius = enemyCheckRadius * 2;
        System.out.println("Enemy check radius: " + Integer.toString(enemyCheckRadius));
    }
    

    @Override
    public void reset() 
    {
    }

    
    @Override
    public AI clone() 
    {
        return new MangoBot(utt);
    }
   
    
    //Old version for posterity.
    /*@Override
    public PlayerAction getAction(int player, GameState gs) 
    {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player getPlayer = gs.getPlayer(player);
        
        //Reset rax count.
        enemyRaxCount = 0;
        workersNear = 0;
        
        System.out.print("Get Action called:");
        myWorkers.clear();
        enemyWorkers.clear();
        nearbyEnemyWorkers.clear();
        
        // TODO: issue commands to units
        for (Unit unit : pgs.getUnits()) 
        {
        	
        	//Get player's units
        	if (unit.getPlayer() == player) 
        	{
        		if (unit.getType() == base) 
        		{
        			myBase = unit;
        		}
        		
            	//Find workers
            	if (unit.getType() == worker) 
            	{
            		//Add worker
            		myWorkers.add(unit);
            	}  
        	}
        	
        	//Check how many barracks the enemy has built.
        	if ((unit.getType() == barracks) && (isEnemy(unit.getPlayer(), player))) 
        	{
        		enemyRaxCount++;
        	}
        	
        	//Catalogue enemy workers
        	if ((unit.getType() == worker) && (isEnemy(unit.getPlayer(), player))) 
        	{
        		//Get all enemy workers
        		enemyWorkers.add(unit);
        		
        		//Check for nearby enemy workers
        		if ((unit.getX() - myBase.getX() < 5) || (unit.getY() - myBase.getY() < 5)) 
        		{
        			nearbyEnemyWorkers.add(unit);
        			nearestEnemyWorker = unit; //Test with just the nearest worker at a time
        		}
        		
        		//Remove any workers who are no longer nearby from the nearby workers list
        		else 
        		{
        			for (int i = 0; i < nearbyEnemyWorkers.size(); i++) 
        			{
        				if (nearbyEnemyWorkers.get(i) == unit) 
        				{
        					nearbyEnemyWorkers.remove(i);
        				}
        			}      			
        		}
        	}
        	

        	//Player actions
        	if (unit.getPlayer() == player) 
        	{
            	//Find workers
            	if (unit.getType() == worker) 
            	{
            		//workerBehaviour(unit, getPlayer, pgs);
            	}  
        		
        		if (enemyRaxCount > 0) 
        		{
        			//Do macro/defense stuff
        		}
        		
        		else 
        		{
        			//Prepare for worker rush
        			
                	//Train worker from player's base
                	if (unit.getType() == base)
                	{        		
                		train(unit, worker);
                	}
        		}
        		
            	     		
        	}	  	
        }
        
        
         //After all units evaluated this tick:
         
       
        
        //Number of targets
        int numNearbyEnemyWorkers = nearbyEnemyWorkers.size();
        
        //Workers that will always continue harvesting
        int forcedHarvestingWorkers = 0;
        
        //Each worker, if not attacking, attacks an enemy worker that is nearby.
		for (Unit worker : myWorkers)
		{			
			AbstractAction currentWorkerAction = getAbstractAction(worker);
			
			//If harvesting
			if ((currentWorkerAction instanceof Harvest == true) && (forcedHarvestingWorkers < workersAlwaysHarvesting)) 
			{
				forcedHarvestingWorkers++;
			}
			
			
			//This worker's current action
			//AbstractAction currentWorkerAction = getAbstractAction(worker);
			
			//If a worker is not attacking and there are available targets, assign it to attack the target
			else if ((currentWorkerAction instanceof Attack == false) && (attackedWorkerIndex < numNearbyEnemyWorkers)) 
			{
				//attack(worker, nearbyEnemyWorkers.get(attackedWorkerIndex));
				attack(worker, nearestEnemyWorker);
				
				//Ensure we do not go outside index range.
				if (attackedWorkerIndex + 1 < numNearbyEnemyWorkers) 
				{
					//Move on to next target
					attackedWorkerIndex++;
				}
				
				else 
				{
					attackedWorkerIndex = 0;
				}
			}	

			else 
			{
				workerBehaviour(worker, getPlayer, pgs);				
			}
			
		}
        
		System.out.print(numNearbyEnemyWorkers + "\n");
        
        return translateActions(player, gs);
    }
    */
    
    
    //Main game loop. 
    //Called every game tick.
    public PlayerAction getAction(int player, GameState gs) 
    {
    	//Get the player and game state.
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player getPlayer = gs.getPlayer(player);
     
        //Track all units.
        TrackUnits(player, pgs);
        
        
        // TODO: issue commands to units.
        for (Unit unit : pgs.getUnits()) 
        {                       	

        	//Player actions
        	if (unit.getPlayer() == player) 
        	{        		
        		
        		if (enemyRaxCount > 0) 
        		{
        			//Do macro/defense stuff
        		}
        		
        		else 
        		{
        			//Prepare for worker rush
        			
                	//Actions from Base.
                	if (unit.getType() == base)
                	{   
                		//Training up to 5 workers. This number should be a variable!
                		if (myWorkers.size() < maxWorkerTrain) 
                		{
                    		train(unit, worker);
                		}
                	}
        		}        		
            	        		
        	}
        	
        }
        
        //Worker actions.
		for (Unit worker : myWorkers)
		{	
			workerBehaviour(worker, getPlayer, pgs);
		}
       
		
		//System.out.print(numNearbyEnemyWorkers + "\n");
        
		//Game logic. Must be kept for game to work.
        return translateActions(player, gs);
    }
    
    
    //Check if the unit is an enemy of the inputed player. Quite possibly redundant and not worth using!
    public boolean isEnemy(int unitOwner, int player) 
    {
    	//If unitOwner is not the current player or Gaia
    	if ((unitOwner != player) && (unitOwner != -1)) 
    	{
    		return true;    		
    	}
    	
    	else 
    	{
    		return false;
    	}
    }
    
    
    //Should be replaced!
    public void workerBehaviour(Unit worker, Player player, PhysicalGameState pgs) 
    {
		if (forcedHarvestingWorkers.size() < minimumHarvesterCount) 
		{
			HarvestResource(worker, player, pgs);
			forcedHarvestingWorkers.add(worker);
		}
		
		//Use workers to defend if enemies are nearby.
		if (nearbyEnemyUnits.size() > 0) 
		{
			WorkerDefense(worker);
			//System.out.print("Workers defending!" + "\n");
		}
		
		//Send workers to harvest.
		else if (getAbstractAction(worker) instanceof Harvest != true) 
		{
			HarvestResource(worker, player, pgs);
		}
        }    	
    
    
    /*------------------
     * Battle Functions
     * ----------------*/
    
    //Behaviour pattern where workers engage nearby enemies and then return to mining.
    //Currently broken as the one worker that should remain harvesting attacks as it often sees workers on their way to harvest!!!!
    public void WorkerDefense(Unit worker)
    {
    	
    	//System.out.println("Attacking enemy unit at: " + Integer.toString(nearbyEnemyUnits.get(0).getX()) + ", " + Integer.toString(nearbyEnemyUnits.get(0).getY()));
		System.out.println("Number of harvesting workers: " + Integer.toString(myHarvestingWorkers.size()));
    	
    	Unit targetEnemy = null;
    	
    	for (int i = 0; i < nearbyEnemyUnits.size(); i++) 
    	{
    		if (nearbyEnemyUnits.get(i) != null) 
    		{
    			targetEnemy = nearbyEnemyUnits.get(i);
    			break;
    		}
    	}
    	
		//Send non-harvesting workers to attack first nearby enemy
		if (getAbstractAction(worker) instanceof Harvest != true) 
		{
			//attack(worker, targetEnemy);
			BasicOffense(worker);
		} 				
    	
		//Send harvesting workers to help if there is at least one worker still harvesting
		else if ((myHarvestingWorkers.size() > 1) && (forcedHarvestingWorkers.contains(worker) == false)) 
    	{
    		//attack(worker, targetEnemy);  
			BasicOffense(worker);
    	}
    }    
    
    
    public void BasicOffense(Unit unit) 
    {
    	//Find the closest enemy to this unit and attack it.
    	Unit target = getClosestEnemy(unit);
    	
    	if (target != null) 
    	{
        	attack(unit, target);
    	}

    }
    
    public Unit getClosestEnemy(Unit unit) 
    {
    	int closestEnemyDistance = 100000;
    	Unit closestEnemy = null;
    	
    	//Check through enemy list
    	for (Unit enemyUnit : allEnemyUnits) 
    	{
    		int enemyDistance = Math.abs(enemyUnit.getX() - unit.getX()) + Math.abs(enemyUnit.getY() - unit.getY());	
    		
    		if (enemyDistance < closestEnemyDistance)
			{
    			closestEnemyDistance = enemyDistance;
    			closestEnemy = enemyUnit;    			
			}
    	}    	
    	
    	return closestEnemy;
    }
    
    /*------------------
     * Build Functions
     * ----------------*/
    
    
    
    /*------------------
     * Utility Functions
     * ----------------*/
        
    //Check for enemies that are nearby.
    //Search within the checkRadius.
    public void CheckNearbyEnemies(PhysicalGameState pgs, int checkRadius) 
    {    	
    	//Check through enemy list
    	for (Unit enemyUnit : allEnemyUnits) 
    	{
    		int enemyDistance = Math.abs(enemyUnit.getX() - myBase.getX()) + Math.abs(enemyUnit.getY() - myBase.getY());
    		
    		//Check for nearby enemy units
    		if (enemyDistance < checkRadius) 
    		{
    			//System.out.println("Nearby enemy added");
        		//System.out.println("Enemy unit location: " + Integer.toString(enemyUnit.getX()) + ", " + Integer.toString(enemyUnit.getY()));
        		//System.out.println("Enemy Unit Distance: " + Integer.toString(enemyDistance));
    			
    			nearbyEnemyUnits.add(enemyUnit);
    		}
    	}    	
    	    	
    }
    
    //Tracks units in the game adding them to the unit lists. 
    //Done from the perspective of the selected player.
    public void TrackUnits(int player, PhysicalGameState pgs) 
    {
    	//Clear lists for new game tick.
        myWorkers = new ArrayList<Unit>();
        this.myHarvestingWorkers = new ArrayList<Unit>();
		//System.out.println("Number of harvesting workers in TrackUnits start: " + Integer.toString(myHarvestingWorkers.size()));
        
        
        allEnemyUnits = new ArrayList<Unit>();
        enemyWorkers = new ArrayList<Unit>();
        enemyBases = new ArrayList<Unit>();
        nearbyEnemyWorkers = new ArrayList<Unit>();
        nearbyEnemyUnits = new ArrayList<Unit>();
        forcedHarvestingWorkers = new ArrayList<Unit>();
        
        enemyRaxCount = 0;
        workersNear = 0;
        
    	
    	//Check all units in the game.
        for (Unit unit : pgs.getUnits()) 
        {
        	
        	/*-------------------
        	 * Check Enemy Units
        	 *-------------------*/        	
        	
        	//Catalogue all enemy units that are not dead. Unsure if the last check is necessary.
        	if ((unit.getPlayer() != player) && (unit.getPlayer() != -1) && (unit.getHitPoints() > 0)) 
        	{
        		allEnemyUnits.add(unit);
        		
        		//Check workers
        		if (unit.getType() == worker) 
        		{
            		enemyWorkers.add(unit);        			
        		}
        		
        		//Check barracks
        		else if (unit.getType() == barracks) 
        		{
            		enemyRaxCount++;
        		}
        		
        		//Check bases
        		else if (unit.getType() == base) 
        		{
        			enemyBases.add(unit);
        		}
        		
        	}
        	
        	
        	/*-------------------
        	 * Check Player's Units
        	 *-------------------*/        	
        	
        	//Get player's units.
        	else if (unit.getPlayer() == player) 
        	{
        		//Find player's base. This will need to be updated to handle multiple bases!
        		if (unit.getType() == base) 
        		{
        			myBase = unit;
        		}
        		
            	//Catalogue player's workers.
            	if (unit.getType() == worker) 
            	{
            		myWorkers.add(unit);
            		
            		//Check harvesting workers.
            		if (getAbstractAction(unit) instanceof Harvest == true) 
            		{
            			myHarvestingWorkers.add(unit);
            		}
            	}  
        	}     
        	
        }
        
    	CheckNearbyEnemies(pgs, enemyCheckRadius);    
        
    }
       
    //Finds the closest resource patch.
    public Unit GetClosestResource(Unit worker, PhysicalGameState pgs) 
    {
        Unit closestResource = null;
        int closestDistance = 0;
        
        //Get closest resources
        for (Unit u2 : pgs.getUnits()) 
        {
            if (u2.getType().isResource) 
            {
                int d = Math.abs(u2.getX() - worker.getX()) + Math.abs(u2.getY() - worker.getY());
                if (closestResource == null || d < closestDistance) 
                {
                    closestResource = u2;
                    closestDistance = d;
                }
            }
        }
        
        return closestResource;
    }
    
    //Finds the closest base where resources can be deposited.
    public Unit GetClosestStockpile(Unit worker, Player player, PhysicalGameState pgs) 
    {
        Unit closestBase = null;
        int closestDistance = 0;
    	
        //Get closest stockpile (player's base)
        for (Unit u2 : pgs.getUnits()) 
        {
            if (u2.getType().isStockpile && u2.getPlayer() == player.getID()) 
            {
                int d = Math.abs(u2.getX() - worker.getX()) + Math.abs(u2.getY() - worker.getY());
                if (closestBase == null || d < closestDistance) 
                {
                    closestBase = u2;
                    closestDistance = d;
                }
            }
        }
        
        return closestBase;
        
    }
    
    //Target worker harvests from nearest resource patch and deposits at the nearest base.
    public void HarvestResource(Unit worker, Player player, PhysicalGameState pgs) 
    {
    	Unit closestResource = GetClosestResource(worker, pgs);
    	Unit closestStockpile = GetClosestStockpile(worker, player, pgs);
    	
    	if (closestResource != null && closestStockpile != null) 
    	{
        	harvest(worker, closestResource, closestStockpile);	
    	}    	

    }
    
    
    
    
    @Override
    public List<ParameterSpecification> getParameters() 
    {
        return new ArrayList<>();
    }
    
}
