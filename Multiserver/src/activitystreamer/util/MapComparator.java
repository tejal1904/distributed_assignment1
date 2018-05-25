package activitystreamer.util;

import org.json.simple.JSONObject;

import java.util.Comparator;
import java.util.Map;

public class MapComparator implements Comparator<String> {

    Map<String, JSONObject> base;

    public MapComparator(Map<String, JSONObject> base){
        this.base = base;
    }
    @Override
    public int compare(String o1, String o2) {
        int level1 = ((Long) base.get(o1).get("level")).intValue();
        int level2 = ((Long) base.get(o2).get("level")).intValue();
        int rank1 = ((Long) base.get(o1).get("rank")).intValue();
        int rank2 = ((Long) base.get(o2).get("rank")).intValue();
        if(level1 > level2){
            return 1;
        }else if(level1 < level2){
            return -1;
        }else if(level1 == level2){
            if(rank1 > rank2){
                return 1;
            }else if(rank1 < rank2){
                return -1;
            }
        }
        return 0;
    }
}
