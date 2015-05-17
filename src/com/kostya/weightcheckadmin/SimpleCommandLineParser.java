package com.kostya.weightcheckadmin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A simple class that provides utilities to ease command line parsing.
 */
public class SimpleCommandLineParser {

    private final Map<String, String> argMap;

    public SimpleCommandLineParser(String[] arg, String predict) {
        argMap = new HashMap<String, String>();
        for (String anArg : arg) {
            String[] str = anArg.split(predict, 2);
            if (str.length > 1)
                argMap.put(str[0], str[1]);
        }
    }

    /**
     * Returns the value of the first key found in the map.
     */
    public String getValue(String... keys) {
        for (String key : keys) {
            if (argMap.get(key) != null) {
                return argMap.get(key);
            }
        }
        return null;
    }

    /**
     * Returns true if any of the given keys are present in the map.
     */
    /*public boolean containsKey(String ... keys) {
        Set<String> keySet = argMap.keySet();
        for (String key : keySet) {
            for (String key1 : keys) {
                if (key.equals(key1)) {
                    return true;
                }
            }
        }
        return false;
    }*/
    public Iterator<String> getKeyIterator() {
        Set<String> keySet = argMap.keySet();
        if (!keySet.isEmpty())
            return keySet.iterator();
        return null;
    }

    /*public int getSize(){
        return argMap.size();
    }*/
}
