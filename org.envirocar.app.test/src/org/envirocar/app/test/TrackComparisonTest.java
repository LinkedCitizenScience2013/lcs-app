package org.envirocar.app.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;

import android.test.AndroidTestCase;

public class TrackComparisonTest extends AndroidTestCase {
	
	private DbAdapter dbMock = new DbAdapterMockup();

	public void testTracksWithMeasurements() {
		Track t1 = new Track(null, null, dbMock);
		t1.addMeasurement(createMeasurement(0));
		Track t2 = new Track(null, null, dbMock);
		t2.addMeasurement(createMeasurement(1));
		Track t3 = new Track(null, null, dbMock);
		t3.addMeasurement(createMeasurement(2));
		
		List<Track> list = createListAndSort(t1, t2, t3);
		
		Assert.assertTrue("Unexpected position!", list.get(0) == t3);
		Assert.assertTrue("Unexpected position!", list.get(1) == t2);
		Assert.assertTrue("Unexpected position!", list.get(2) == t1);
	}

	public void testTracksWithoutMeasurements() {
		Track t1 = new Track(null, null, dbMock);
		Track t3 = new Track(null, null, dbMock);
		
		List<Track> list = createListAndSort(t1, t3);
		
		Assert.assertTrue("Unexpected position!", list.get(0) == t1);
		Assert.assertTrue("Unexpected position!", list.get(1) == t3);
	}
	
	public void testOneTrackWithNoMeasurements() {
		Track t1 = new Track(null, null, dbMock);
		t1.addMeasurement(createMeasurement(0));
		Track t2 = new Track(null, null, dbMock);
		Track t3 = new Track(null, null, dbMock);
		
		List<Track> list = createListAndSort(t1, t2, t3);
		
		Assert.assertTrue("Unexpected position!", list.get(2) == t1);
	}
	
	private List<Track> createListAndSort(Track... t1) {
		List<Track> list = new ArrayList<Track>();
		
		for (Track track : t1) {
			list.add(track);
		}
		
		Collections.sort(list);
		return list;
	}
	
	private Measurement createMeasurement(int deltaMillis) {
		Measurement result = new Measurement(0.0, 0.0);
		result.setTime(System.currentTimeMillis()+deltaMillis);
		return result;
	}

}
