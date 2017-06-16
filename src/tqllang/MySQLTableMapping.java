package tqllang;

/**
 * Yas
 */

public class MySQLTableMapping
{
    public static String getMySQLNameFor(String collectionName)
    {
        return getMySQLNameForType(CollectionTypeMapping.getTypeOf(collectionName));
    }

    public static String getMySQLNameForType(CollectionType type)
    {
        // TODO:
        switch(type)
        {
            case virtualSensorType:
                return "VirtualSensorType";
            case virtualSensor:
                return "VirtualSensor";
            case user:
                return "Users";
            case sensorType:
                return "SensorType";
            case group:
                return "Groups";
            case infra:
                return "Infrastructure";
            case infraType:
                return "InfrastructureType";
            case location:
                return "Location";
            case observation:
                return "Observation";
            case observationType:
                return "ObservationType";
            case platform:
                return "Platform";
            case platformType:
                return "PlatformType";
            case region:
                return "Region";
            case semanticObservation:
                return "SemanticObservation";
            case semanticObsType:
                return "SemanticObservationType";
            case sensor:
                return "Sensor";

            default:
                return "";
        }
    }
}
