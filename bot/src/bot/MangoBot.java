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
    
    private Unit myBase;
    
    //All the player's workers
    private List<Unit> myWorkers = new ArrayList<Unit>();
    
    //All the enemy workers
    private List<Unit> enemyWorkers = new ArrayList<Unit>();
    
    //Enemy workers near base
    private List<Unit> nearbyEnemyWorkers = new ArrayList<Unit>();
    
    //Single nearest enemy worker
    private Unit nearestEnemyWorker;
    
    //Current target when workers are attacking nearby enemy workers.
    int attackedWorkerIndex = 0;
    
    //Number of workers that must alwyas be harvesting.
    int workersAlwaysHarvesting = 1;
    
    
    public MangoBot(UnitTypeTable utt) 
    {
        super(new AStarPathFinding());
        this.utt = utt;
        worker = utt.getUnitType("Worker");
        base = utt.getUnitType("Base");
        barracks = utt.getUnitType("Barracks");
        
        enemyRaxCount = 0;
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
   
    
    @Override
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
        
        /*
         * After all units evaluated this tick:
         * */
       
        
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
    
    
    public void workerBehaviour(Unit worker, Player player, PhysicalGameState pgs) 
    {
            Unit closestBase = null;
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
            
            closestDistance = 0;
            
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
            
            //Harvest
            if (closestResource != null && closestBase != null) 
            {
    			//This worker's current action
    			AbstractAction currentWorkerAction = getAbstractAction(worker);
    			
    			//If not already attacking, harvest
    			if (currentWorkerAction instanceof Attack == false) 
    			{
                	harvest(worker, closestResource, closestBase);
    			}   			

            }
        }    	
    
    
    @Override
    public List<ParameterSpecification> getParameters() 
    {
        return new ArrayList<>();
    }
    
}
