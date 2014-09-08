package ucar.nc2.ft.point;

import com.google.common.base.Preconditions;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NoFactoryFoundException;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.units.DateUnit;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author cwardgar
 * @since 2014/08/21
 */
// This class ought to be a PointFeatureCollection, by extending PointCollectionImpl.
// However, we do not have the timeUnit and altUnits that the constructor requires. Does it really need
// that info? Can't it calculate it from one of its features? That interface may need to be re-thought.
public class SortingStationPointFeatureCache {
    public static final Comparator<StationPointFeature> stationNameComparator = new Comparator<StationPointFeature>() {
        @Override
        public int compare(StationPointFeature pointFeat1, StationPointFeature pointFeat2) {
            return pointFeat1.getStation().getName().compareTo(pointFeat2.getStation().getName());
        }
    };


    private final Comparator<StationPointFeature> comp;
    private final SortedMap<StationPointFeature, List<StationPointFeature>> inMemCache;

    private volatile StationFeatureCopyFactory stationFeatCopyFactory;

    public SortingStationPointFeatureCache() {
        this(stationNameComparator);
    }

    // We're going to init stationFeatCopyFactory using the first feat that's add()ed.
    public SortingStationPointFeatureCache(Comparator<StationPointFeature> comp) {
        this.comp = Preconditions.checkNotNull(comp, "comp == null");
        this.inMemCache = new TreeMap<>(comp);
        // stationFeatCopyFactory remains null.
    }

    public SortingStationPointFeatureCache(
            Comparator<StationPointFeature> comp, StationPointFeature proto, DateUnit dateUnit) throws IOException {
        this.comp = Preconditions.checkNotNull(comp, "comp == null");
        this.inMemCache = new TreeMap<>(comp);

        if (proto != null && dateUnit != null) {
            this.stationFeatCopyFactory = new StationFeatureCopyFactory(proto, dateUnit);
        }
    }

    public void add(StationPointFeature feat) throws IOException {
        Preconditions.checkNotNull(feat, "feat == null");
        StationPointFeature featCopy = getStationFeatureCopyFactory(feat).deepCopy(feat);

        List<StationPointFeature> bucket = inMemCache.get(featCopy);
        if (bucket == null) {
            bucket = new LinkedList<>();
            inMemCache.put(featCopy, bucket);
        }

        bucket.add(featCopy);
    }

    public void addAll(File datasetFile) throws NoFactoryFoundException, IOException {
        try (FeatureDatasetPoint fdPoint = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                FeatureType.STATION, datasetFile.getAbsolutePath(), null)) {
            addAll(fdPoint);
        }
    }

    // fdPoint remains open.
    public void addAll(FeatureDatasetPoint fdPoint) throws IOException {
        PointFeatureIterator pointFeatIter = new PointDatasetIterator(fdPoint);
        try {
            while (pointFeatIter.hasNext()) {
                StationPointFeature pointFeat = (StationPointFeature) pointFeatIter.next();
                add(pointFeat);
            }
        } finally {
            pointFeatIter.finish();
        }
    }

    // Double-check idiom for lazy initialization of instance fields. See Effective Java 2nd Ed, p. 283.
    private StationFeatureCopyFactory getStationFeatureCopyFactory(StationPointFeature proto) throws IOException {
        if (stationFeatCopyFactory == null) {
            synchronized (this) {
                if (stationFeatCopyFactory == null) {
                    stationFeatCopyFactory = createStationFeatureCopyFactory(proto);
                }
            }
        }

        assert stationFeatCopyFactory != null : "We screwed this up.";
        return stationFeatCopyFactory;
    }

    private StationFeatureCopyFactory createStationFeatureCopyFactory(StationPointFeature proto) throws IOException {
        if (!(proto instanceof PointFeatureImpl)) {
            // LOOK: As of 2014/08/19, all StationPointFeature implementations in NetCDF-Java extend PointFeatureImpl,
            // so this won't be common. Still, we should consider a better solution, such as adding getTimeUnit() to
            // PointFeature.
            throw new IllegalArgumentException("feature is not an instance of PointFeatureImpl, " +
                    "so we cannot gather DateUnit information from it. It is a " + proto.getClass().getName());
        }

        DateUnit timeUnit = ((PointFeatureImpl) proto).getTimeUnit();
        return new StationFeatureCopyFactory(proto, timeUnit);
    }

    // TODO: Once this method is called, prohibit any further additions to cache.
    public PointFeatureIterator getPointFeatureIterator() throws IOException {
        return new PointIteratorAdapter(new Iter());
    }

    private class Iter implements Iterator<StationPointFeature> {
        private final Iterator<List<StationPointFeature>> bucketsIter;
        private Iterator<StationPointFeature> featsIter;

        public Iter() {
            this.bucketsIter = inMemCache.values().iterator();
        }

        @Override
        public boolean hasNext() {  // Method is idempotent.
            while (featsIter == null || !featsIter.hasNext()) {
                if (!bucketsIter.hasNext()) {
                    return false;
                } else {
                    featsIter = bucketsIter.next().iterator();
                }
            }

            assert featsIter != null && featsIter.hasNext();
            return true;
        }

        @Override
        public StationPointFeature next() {
            if (!hasNext()) {  // Don't rely on user to call this.
                throw new NoSuchElementException("There are no more elements.");
            } else {
                return featsIter.next();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Operation not supported by this iterator.");
        }
    }
}
