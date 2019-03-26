package bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
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
    
    public MangoBot(UnitTypeTable utt) 
    {
        super(new AStarPathFinding());
        this.utt = utt;
        worker = utt.getUnitType("Worker");
        base = utt.getUnitType("Base");
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
        
        for (Unit unit : pgs.getUnits()) 
        {
            // TODO: issue commands to units
        	if (unit.getPlayer() == player) 
        	{
            	//Train worker from player's base
            	if (unit.getType() == base)
            	{        		
            		train(unit, worker);
            	}
            	
            	if (unit.getType() == worker) 
            	{
            		workerBehaviour(unit, getPlayer, pgs);
            	}        		
        	}	  	
        }
        
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
            
            if (closestResource != null && closestBase != null) 
            {
            	harvest(worker, closestResource, closestBase);
            }
        }    	
    
    
    @Override
    public List<ParameterSpecification> getParameters() 
    {
        return new ArrayList<>();
    }
    
}
