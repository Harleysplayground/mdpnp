package org.mdpnp.devices;

import java.util.Date;

public interface DeviceClock {

    /**
     * @return a reading representing the current instant as defined by the clock, not null
     */
    Reading instant();

    /**
     * A point on the time-line as perceived by the instance of the clock that is returning this reading.
     *
     * In reality, just like different calendars have different conventions of now to name a particular year –
     * Gregorian vs Julian vs etc calendars, a moment in time can be described by the system clock and by the
     * device clock and the two are not guaranteed to be the same.  But unlike the dates that can be converted
     * from one calendar to another, the clocks are not guaranteed to be in sync. Therefore we would
     * want to carry both timestamps around.
     *
     * @see CombinedReading
     */
    public interface Reading {
        long getTime();
        boolean hasDeviceTime();
        long getDeviceTime();
        Reading refineResolutionForFrequency(int frequency, int size);
    };


    public static class WallClock implements DeviceClock {

        @Override
        public Reading instant() {
            return new ReadingImpl(getTimeInMillis());
        }

        protected long getTimeInMillis()
        {
            return System.currentTimeMillis();
        }

    }

    public static class Metronome extends WallClock {

        final long updatePeriod;

        public Metronome(long p) {
            updatePeriod = p;
        }

        @Override
        protected long getTimeInMillis() {
            long now = System.currentTimeMillis();
            now = now - now % updatePeriod;
            return now;
        }
    }

    public static class ReadingImpl implements DeviceClock.Reading {
        private final long ms;

        public ReadingImpl(long time) {
            ms = time;
        }

        @Override
        public String toString() {
            return (new Date(ms)).toString();
        }

        @Override
        public long getTime() {
            return ms;
        }

        @Override
        public long getDeviceTime() {
            return ms;
        }

        @Override
        public boolean hasDeviceTime() {
            return true;
        }

        @Override
        public Reading refineResolutionForFrequency(int frequency, int size) {
            return this;
        }
    }

    public static class CombinedReading implements DeviceClock.Reading {

        final Reading ref;
        final Reading dev;

        public CombinedReading(Reading ref, Reading dev) {
            this.ref = ref;
            this.dev = dev;
        }

        @Override
        public String toString() {
            return ref.toString() + " " + dev.toString();
        }

        @Override
        public long getTime() {
            return ref.getTime();
        }

        @Override
        public boolean hasDeviceTime() {
            return dev.hasDeviceTime();
        }

        @Override
        public long getDeviceTime() {
            return dev.getTime();
        }

        @Override
        public Reading refineResolutionForFrequency(int frequency, int size) {
            ref.refineResolutionForFrequency(frequency, size);
            return this;
        }
    }
}
