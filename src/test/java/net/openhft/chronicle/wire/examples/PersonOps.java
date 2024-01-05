package net.openhft.chronicle.wire.examples;

/**
 * This interface defines operations related to a person.
 */
public interface PersonOps {

   /**
    * Add a new person.
    *
    * @param p The person to be added.
    */
   void addPerson(Person p);
}
