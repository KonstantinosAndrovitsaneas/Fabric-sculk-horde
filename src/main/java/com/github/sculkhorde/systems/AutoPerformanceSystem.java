package com.github.sculkhorde.systems;

import com.github.sculkhorde.core.ModConfig;
import com.github.sculkhorde.core.SculkHorde;

import java.util.concurrent.TimeUnit;

public class AutoPerformanceSystem {

    private static long lastTimeOfTPSCheck = System.currentTimeMillis();
    private static int TPSTickCount = 0;
    private static int TPS = 0;

    /*
    High performance is >= 15
    Medium Performance >= 10
    Low Performance >= 5
    Potato Performance < 5
     */
    public enum PerformanceMode
    {
        High,
        Medium,
        Low,
        Potato
    }
    protected final int HIGH_PERFORMANCE_TPS_MINIMUM = 15;
    protected final int MEDIUM_PERFORMANCE_TPS_MINIMUM = 10;
    protected final int LOW_PERFORMANCE_TPS_MINIMUM = 5;
    protected long timeStampAboveTPSMaximum = 0;
    protected long timeStampBelowTPSMinimum = 0;

    protected PerformanceMode performanceMode = PerformanceMode.Medium;

    // Controllable Variables
    protected int maxSculkUnitPopulation = 0;
    protected int maxInfectorCursorPopulation = 0;
    protected int maxNodesActive = 0;

    // Variables for Cursor Performance System
    protected int infectorCursorPopulationThreshold = 0;
    protected int cursorsToTickPerTick = 0;
    protected int delayBetweenCursorTicks = 0;
    protected boolean thanosSnapCursors = false;

    public AutoPerformanceSystem()
    {
        setPerformanceMode(PerformanceMode.Medium);
    }

    //#### Accessors ####

    public static double getTPS() {
        return TPS;
    }
    public PerformanceMode getPerformanceMode() {
        return performanceMode;
    }

    public boolean isPerformanceModeEnabled()
    {
        return !isHighPerformanceMode();
    }

    public boolean isHighPerformanceMode()
    {
        return performanceMode == PerformanceMode.High;
    }

    public boolean isMediumPerformanceMode()
    {
        return performanceMode == PerformanceMode.Medium;
    }

    public boolean isLowPerformanceMode()
    {
        return performanceMode == PerformanceMode.Low;
    }

    public boolean isPotatoPerformanceMode()
    {
        return performanceMode == PerformanceMode.Potato;
    }

    public int getMaxSculkUnitPopulation()
    {
        if(isPerformanceModeEnabled())
        {
            return maxSculkUnitPopulation;
        }

        return ModConfig.SERVER.max_unit_population.get();
    }

    public int getInfectorCursorPopulationThreshold()
    {
        return infectorCursorPopulationThreshold;
    }

    public int getCursorsToTickPerTick()
    {
        return cursorsToTickPerTick;
    }

    public int getDelayBetweenCursorTicks()
    {
        return delayBetweenCursorTicks;
    }

    public boolean isThanosSnappingCursors()
    {
        return thanosSnapCursors;
    }

    public int getMaxNodesActive()
    {
        return maxNodesActive;
    }

    public int getMaxInfectorCursorPopulation()
    {
        return maxInfectorCursorPopulation;
    }


    //#### Mutators ####

    protected void setInfectorCursorPopulationThreshold(int value)
    {
        SculkHorde.LOGGER.info("AutoPerformanceSystem | Cursor population threshold to activate cursor performance mode = " + value);
        infectorCursorPopulationThreshold = value;
    }

    protected void setCursorsToTickPerTick(int value)
    {
        SculkHorde.LOGGER.info("AutoPerformanceSystem | Cursors to tick per tick when cursor performance mode is active = " + value);
        cursorsToTickPerTick = value;
    }

    protected void setDelayBetweenCursorTicks(int value)
    {
        SculkHorde.LOGGER.info("AutoPerformanceSystem | Delay between cursor ticks when cursor performance mode is active = " + value);
        delayBetweenCursorTicks = value;
    }

    protected void setMaxSculkUnitPopulation(int value)
    {
        SculkHorde.LOGGER.info("AutoPerformanceSystem | Max Sculk Unit Population = " + value);
        maxSculkUnitPopulation = value;
    }

    protected void setMaxInfectorCursorPopulation(int value)
    {
        SculkHorde.LOGGER.info("AutoPerformanceSystem | Max Infector Cursor Population = " + value);
        maxInfectorCursorPopulation = value;
    }

    protected void setMaxNodesActive(int value)
    {
        SculkHorde.LOGGER.info("AutoPerformanceSystem | Max Nodes Active = " + value);
        maxNodesActive = value;
    }

    protected void setThanosSnapCursors(boolean value)
    {
        SculkHorde.LOGGER.info("AutoPerformanceSystem | Thanos Snap Cursors = " + value);
        thanosSnapCursors = value;
    }

    protected void setPerformanceMode(PerformanceMode mode)
    {
        timeStampAboveTPSMaximum = 0;
        timeStampBelowTPSMinimum = 0;

        if(mode == PerformanceMode.High)
        {
            performanceMode = PerformanceMode.High;
            SculkHorde.LOGGER.info("AutoPerformanceSystem | New Performance Mode = High");
            setMaxSculkUnitPopulation(ModConfig.SERVER.max_unit_population.get());
            setMaxNodesActive(ModConfig.SERVER.max_nodes_active.get());
            setMaxInfectorCursorPopulation(ModConfig.SERVER.max_infestation_cursor_population.get());
            setInfectorCursorPopulationThreshold(ModConfig.SERVER.max_infestation_cursor_population.get() - 25);
            setCursorsToTickPerTick(50);
            setDelayBetweenCursorTicks(1);
            setThanosSnapCursors(false);
        }
        if(mode == PerformanceMode.Medium)
        {
            performanceMode = PerformanceMode.Medium;
            SculkHorde.LOGGER.info("AutoPerformanceSystem | New Performance Mode = Medium");
            setMaxSculkUnitPopulation(Math.max(ModConfig.SERVER.max_unit_population.get() - 50, 25));
            setMaxNodesActive(Math.max(ModConfig.SERVER.max_nodes_active.get() - 1, 1));
            setMaxInfectorCursorPopulation(150);
            setInfectorCursorPopulationThreshold(125);
            setCursorsToTickPerTick(25);
            setDelayBetweenCursorTicks(1);
            setThanosSnapCursors(false);
        }
        if(mode == PerformanceMode.Low)
        {
            performanceMode = PerformanceMode.Low;
            SculkHorde.LOGGER.info("AutoPerformanceSystem | New Performance Mode = Low");
            setMaxSculkUnitPopulation(Math.max(ModConfig.SERVER.max_unit_population.get() - 100, 25));
            setMaxNodesActive(Math.max(ModConfig.SERVER.max_nodes_active.get() - 2, 1));
            setMaxInfectorCursorPopulation(100);
            setInfectorCursorPopulationThreshold(75);
            setCursorsToTickPerTick(10);
            setDelayBetweenCursorTicks(2);
            setThanosSnapCursors(true);

        }
        if(mode == PerformanceMode.Potato)
        {
            performanceMode = PerformanceMode.Potato;
            SculkHorde.LOGGER.info("AutoPerformanceSystem | New Performance Mode = Potato");
            setMaxSculkUnitPopulation(Math.max(ModConfig.SERVER.max_unit_population.get() - 150, 25));
            setMaxNodesActive(Math.max(ModConfig.SERVER.max_nodes_active.get() - 3, 1));
            setMaxInfectorCursorPopulation(50);
            setInfectorCursorPopulationThreshold(25);
            setCursorsToTickPerTick(1);
            setDelayBetweenCursorTicks(3);
            setThanosSnapCursors(true);
        }
    }

    public void increasePerformanceMode()
    {
        if(isMediumPerformanceMode())
        {
            setPerformanceMode(PerformanceMode.High);
        }
        else if(isLowPerformanceMode())
        {
            setPerformanceMode(PerformanceMode.Medium);
        }
        else if(isPotatoPerformanceMode())
        {
            setPerformanceMode(PerformanceMode.Low);
        }
    }

    public void decreasePerformanceMode()
    {
        if(isHighPerformanceMode())
        {
            setPerformanceMode(PerformanceMode.Medium);
        }
        else if(isMediumPerformanceMode())
        {
            setPerformanceMode(PerformanceMode.Low);
        }
        else if(isLowPerformanceMode())
        {
            setPerformanceMode(PerformanceMode.Potato);
        }
    }

    public boolean isTPSBelowMinimum()
    {
        if(isHighPerformanceMode() && getTPS() < HIGH_PERFORMANCE_TPS_MINIMUM)
        {
            return true;
        }
        else if(isMediumPerformanceMode() && getTPS() < MEDIUM_PERFORMANCE_TPS_MINIMUM)
        {
            return true;
        }
        else if(isLowPerformanceMode() && getTPS() < LOW_PERFORMANCE_TPS_MINIMUM)
        {
            return true;
        }

        return false;
    }

    public boolean isTPSAboveMaximum()
    {
        if(isMediumPerformanceMode() && getTPS() >= HIGH_PERFORMANCE_TPS_MINIMUM)
        {
            return true;
        }
        else if(isLowPerformanceMode() && getTPS() >= MEDIUM_PERFORMANCE_TPS_MINIMUM)
        {
            return true;
        }
        else if(isPotatoPerformanceMode() && getTPS() >= LOW_PERFORMANCE_TPS_MINIMUM)
        {
            return true;
        }

        return false;
    }

    public void onServerTick()
    {
        TPSTickCount++;
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastTimeOfTPSCheck;

        if (timeDiff >= 1000) {
            TPS = (int) (TPSTickCount / (timeDiff / 1000.0));
            TPSTickCount = 0;
            lastTimeOfTPSCheck = currentTime;
        }

        if(isTPSBelowMinimum() && timeStampBelowTPSMinimum == 0)
        {
            timeStampBelowTPSMinimum = System.currentTimeMillis();
        }
        else if(!isTPSBelowMinimum() && timeStampBelowTPSMinimum != 0)
        {
            timeStampBelowTPSMinimum = 0;
        }

        if(isTPSAboveMaximum() && timeStampAboveTPSMaximum == 0)
        {
            timeStampAboveTPSMaximum = System.currentTimeMillis();
        }
        else if(!isTPSAboveMaximum() && timeStampAboveTPSMaximum != 0)
        {
            timeStampAboveTPSMaximum = 0;
        }


        long TIME_THRESHOLD_BELOW_MINIMUM_TPS = TimeUnit.SECONDS.toMillis(ModConfig.SERVER.seconds_required_for_performance_decrease.get());
        long TIME_THRESHOLD_ABOVE_MAXIMUM_TPS = TimeUnit.MINUTES.toMillis(ModConfig.SERVER.minutes_required_for_performance_increase.get());

        if(System.currentTimeMillis() - timeStampBelowTPSMinimum >= TIME_THRESHOLD_BELOW_MINIMUM_TPS && timeStampBelowTPSMinimum != 0)
        {
            decreasePerformanceMode();
        }
        else if((System.currentTimeMillis() - timeStampAboveTPSMaximum >= TIME_THRESHOLD_ABOVE_MAXIMUM_TPS  && timeStampAboveTPSMaximum != 0) || (ModConfig.SERVER.disable_auto_performance_system.get() && !isHighPerformanceMode()))
        {
            increasePerformanceMode();
        }
    }
}
