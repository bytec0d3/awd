package util;

import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

public class AutonomousUtils {

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue( Map<K, V> map )
    {
        List<Map.Entry<K, V>> list =
                new LinkedList<>( map.entrySet() );
        Collections.sort( list, new Comparator<Map.Entry<K, V>>()
        {
            @Override
            public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
            {
                return ( o1.getValue() ).compareTo( o2.getValue() );
            }
        } );

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list)
        {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
    }

    public static float jaccardIndex(Iterable<?> firstList, Iterable<?> secondList){

        return (float)CollectionUtils.intersection(firstList, secondList).size() / (float)CollectionUtils.union(firstList, secondList).size();
    }
}
