package net.openhft.chronicle.wire;

public interface Sequence {

    /**
     * gets the sequence for a writePosition
     * <p>
     * This method will only return a valid sequence number of the write position if the write position is the
     * last write position in the queue. YOU CAN NOT USE THIS METHOD TO LOOK UP RANDOM SEQUENCES FOR ANY WRITE POSITION.
     * Long.MIN_VALUE will be return if a sequence number can not be found
     *
     * @param writePosition the last write position, expected to be the end of queue
     * @return Long.MIN_VALUE if the sequence for this write position can not be found
     */
    long sequence(long writePosition);

    /**
     * sets the sequence number for a writePosition
     *
     * @param sequence the sequence number
     * @param position the write position
     */
    void sequence(long sequence, long position);
}
