package uk.ac.ngs.common;


/**
 * @author dawid
 */
public class Pair< T, U > {

    public static < T, U > Pair< T, U > create( T first, U second ) {
        return new Pair< T, U >( first, second );
    }


    public Pair( T first, U second ) {
        this.first = first;
        this.second = second;
    }


    public T first;
    public U second;

}
