/* 
 * enviroCar 2014
 * Copyright (C) 2014 enviroCar contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.application;

import org.envirocar.app.R;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.NumberWithUOM;
import org.envirocar.app.storage.Track;

import android.content.Context;
import android.content.res.Resources;
import android.preference.PreferenceManager;

/**
 * This class returns different locale dependent values, e.g. currency, speed, distance.
 * 
 * @author bpross-52n
 *
 */
public class L10NManager {

	private static Logger logger = Logger.getLogger(L10NManager.class);
	private Context context;
	
	public static final double KM_TO_MILE_FACTOR = 1.609344;
	
	public static final double LITER_TO_USGALLON_FACTOR = 3.785411784;
	public static final double LITER_TO_IMPERIALGALLON_FACTOR = 4.54609;
	
	private double lPer100kmToMPGUSFactor = KM_TO_MILE_FACTOR / LITER_TO_USGALLON_FACTOR;
	private double lPer100kmToMPGImperialFactor = KM_TO_MILE_FACTOR / LITER_TO_IMPERIALGALLON_FACTOR;
	
	private final String perHour = "/h";
	
	private Resources resources;
	
	public L10NManager(Context ctx){
		this.context = ctx;
		resources = context.getResources();
	}
	
	/**
	 * This method returns a {@link NumberWithOUM} that contains the speed value and unit. If the user has not selected a preferred unit,
	 * the default unit for the current locale is returned.
	 * As the speed in km/h already is calculated in the {@link Track}s, this is used as base for the conversion
	 * in other speed units.
	 * 
	 * @param speedInKMperHour the speed value in km/h
	 * @return NumberWithUOM containing the speed value and unit
	 */
	public NumberWithUOM getSpeed(int speedInKMperHour){
		
		Number speed = 0.0;
		String unit = resources.getString(R.string.not_applicable);
	
		String unitDescription = getSpeedUnitDescription();
		
		if(unitDescription.equals(resources.getString(R.string.description_kilometers_per_hour))){
			speed = speedInKMperHour;
			unit = resources.getString(R.string.unit_kilometers_per_hour);
		}else if(unitDescription.equals(resources.getString(R.string.description_miles_per_hour))){
			speed = speedInKMperHour/KM_TO_MILE_FACTOR;		
			unit = resources.getString(R.string.unit_miles_per_hour);	
		}else{
			logger.debug("No speed unit found.");
		}
		
		NumberWithUOM result = new NumberWithUOM(speed, unit);
		
		return result;		
		
	}
	
	/**
	 * This method returns a {@link NumberWithOUM} that contains the consumption value and unit. If the user has not selected a preferred unit,
	 * the default unit for the current locale is returned.
	 * As the consumption in l/h already is calculated in the {@link Track}s, this is used as base for the conversion
	 * in other consumption units.
	 * 
	 * @param consumptionPerHour the consumption value in l/h
	 * @return NumberWithUOM containing the consumption value and unit
	 */
	public NumberWithUOM getConsumptionValuePerHour(double consumptionPerHour){	
		
		Number fuelConsumption = 0.0;
		String unit = context.getResources().getString(R.string.not_applicable);
	
		String unitDescription = getFuelVolumeUnitDescription();
		
		if(unitDescription.equals(resources.getString(R.string.description_liter))){
			fuelConsumption = consumptionPerHour;
			unit = context.getResources().getString(R.string.unit_liter) + perHour;
		}else if(unitDescription.equals(resources.getString(R.string.description_imperial_gallon))){
			fuelConsumption = consumptionPerHour / LITER_TO_IMPERIALGALLON_FACTOR;
			unit = context.getResources().getString(R.string.unit_imperial_gallon) + perHour;
		}else if(unitDescription.equals(resources.getString(R.string.description_us_liquid_gallon))){
			fuelConsumption = consumptionPerHour / LITER_TO_USGALLON_FACTOR;
			unit = context.getResources().getString(R.string.unit_us_liquid_gallon) + perHour;
		}else{
			logger.debug("No unit for consumption per hour found.");
		}
		
		NumberWithUOM result = new NumberWithUOM(fuelConsumption, unit);
		
		return result;
		
	}
	
	/**
	 * This method returns a {@link NumberWithOUM} that contains the consumption value and unit in a more common representation than l/h. If the user has not selected a preferred unit,
	 * the default unit for the current locale is returned.
	 * As the consumption in l/100km already is calculated in the {@link Track}s, this is used as base for the conversion
	 * in other consumption units.
	 * 
	 * @param literOn100km the consumption value in l/100km
	 * @return NumberWithUOM containing the consumption value and unit
	 */
	public NumberWithUOM getCommonConsumptionValue(double literOn100km){		
		
		Number fuelConsumption = 0.0;
		String unit = context.getResources().getString(R.string.not_applicable);
	
		String unitDescription = getConsumptionUnitDescription();
		
		if(unitDescription.equals(resources.getString(R.string.description_miles_per_us_gallon))){
			fuelConsumption = (100/lPer100kmToMPGUSFactor)/literOn100km;
			unit = context.getResources().getString(R.string.unit_miles_per_us_gallon);
		}else if(unitDescription.equals(resources.getString(R.string.description_miles_per_imperial_gallon))){
			fuelConsumption = (100/lPer100kmToMPGImperialFactor)/literOn100km;
			unit = context.getResources().getString(R.string.unit_miles_per_imperial_gallon);
		}else if(unitDescription.equals(resources.getString(R.string.description_liters_per_100_km))){
			fuelConsumption = literOn100km;		
			unit = context.getResources().getString(R.string.unit_liters_per_100_km);
		}else if(unitDescription.equals(resources.getString(R.string.description_kilometers_per_liter))){
			fuelConsumption = (1/literOn100km)*100;
			unit = context.getResources().getString(R.string.unit_kilometer_per_liter);
		}else if(unitDescription.equals(resources.getString(R.string.description_us_gallons_per_100_miles))){
			fuelConsumption = lPer100kmToMPGUSFactor*literOn100km;
			unit = context.getResources().getString(R.string.unit_miles_per_us_gallon);
		}else if(unitDescription.equals(resources.getString(R.string.description_imperial_gallons_per_100_miles))){
			fuelConsumption = lPer100kmToMPGImperialFactor*literOn100km;	
			unit = context.getResources().getString(R.string.unit_imperial_gallons_per_100_miles);
		}else{
			logger.debug("No common consumption unit found.");
		}

		NumberWithUOM result = new NumberWithUOM(fuelConsumption, unit);
		
		return result;
		
	}
	
	/**
	 * This method returns a {@link NumberWithOUM} that contains the distance value and unit. If the user has not selected a preferred unit,
	 * the default unit for the current locale is returned.
	 * As the distance in km already is calculated in the {@link Track}s, this is used as base for the conversion
	 * in other distance units.
	 * 
	 * @param distanceInKM the distance value in km
	 * @return NumberWithUOM containing the distance value and unit
	 */
	public NumberWithUOM getDistance(double distanceInKM){
		
		Number distance = 0.0;
		String unit = context.getResources().getString(R.string.not_applicable);
	
		String unitDescription = getDistanceUnitDescription();
		
		if(unitDescription.equals(resources.getString(R.string.description_kilometers))){
			distance = distanceInKM;
			unit = context.getResources().getString(R.string.unit_kilometer);
		}else if(unitDescription.equals(resources.getString(R.string.description_miles))){
			distance = distanceInKM/KM_TO_MILE_FACTOR;
			unit = context.getResources().getString(R.string.unit_mile);
		}else{
			logger.debug("No distance unit found.");
		}
		
		NumberWithUOM result = new NumberWithUOM(distance, unit);
		
		return result;	
		
	}
	
	public void getCurrency(){
		
	}
	
	public void getTime(){
		
	}
	
	/**
	 * This method returns the textual description of the distance unit. If the user has not selected a preferred unit,
	 * the default unit for the current locale is returned.
	 * 
	 * @return String representing the textual description of the distance unit
	 */
	public String getDistanceUnitDescription(){
		return getUnitDescription(SettingsActivity.DISTANCE_UNITS_LIST_KEY, R.string.local_distance_unit_description);
	}
	
	/**
	 * This method returns the textual description of the speed unit. If the user has not selected a preferred unit,
	 * the default unit for the current locale is returned.
	 * 
	 * @return String representing the textual description of the speed unit
	 */
	public String getSpeedUnitDescription(){
		return getUnitDescription(SettingsActivity.SPEED_UNITS_LIST_KEY, R.string.local_speed_unit_description);
	}
	
	/**
	 * This method returns the textual description of the fuel volume unit. If the user has not selected a preferred unit,
	 * the default unit for the current locale is returned.
	 * 
	 * @return String representing the textual description of the fuel volume unit
	 */
	public String getFuelVolumeUnitDescription(){
		return getUnitDescription(SettingsActivity.FUEL_VOLUME_UNITS_LIST_KEY, R.string.local_fuel_volume_unit_description);
	}
	
	/**
	 * This method returns the textual description of the consumption unit. If the user has not selected a preferred unit,
	 * the default unit for the current locale is returned.
	 * 
	 * @return String representing the textual description of the consumption unit
	 */
	public String getConsumptionUnitDescription(){
		return getUnitDescription(SettingsActivity.CONSUMPTION_UNITS_LIST_KEY, R.string.local_consumption_unit_description);
	}
	
	private String getUnitDescription(String key, int fallBackLocalUnitDescriptionID){
		
		String locale = context.getResources().getConfiguration().locale.getDisplayName();
		
		String unitDescription = PreferenceManager.getDefaultSharedPreferences(context).getString(key, null);
		
		if (unitDescription == null) {
			String fallBackLocalUnitDescription = resources.getString(fallBackLocalUnitDescriptionID);
			logger.debug("A preferred unit description was not set, using " + fallBackLocalUnitDescription + " for locale: " + locale);			
			unitDescription = fallBackLocalUnitDescription;	
		}
		
		return unitDescription;
	}
	
}
