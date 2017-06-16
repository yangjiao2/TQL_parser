package tqllang;

/**
 * Yas
 */

public class CollectionTypeMapping
{
    public static String getNameOf(CollectionType type)
    {
        switch (type)
        {
            case group:
                return "Group";
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
            case sensorType:
                return "SensorType";
            case user:
                return "User";
            case virtualSensor:
                return "VirtualSensor";
            case virtualSensorType:
                return "VirtualSensorType";
            case sensorVariable:
                return "SensorCollection";
            case observationVariable:
                return "ObservationCollection";

            default:
                return "";
        }
    }

    public static CollectionType getTypeOf(String collectionName)
    {
        switch(collectionName)
        {
            case "Group":
                return CollectionType.group;
            case "Infrastructure":
                return CollectionType.infra;
            case "InfrastructureType":
                return CollectionType.infraType;
            case "Location":
                return CollectionType.location;
            case "Observation":
                return CollectionType.observation;
            case "ObservationType":
                return CollectionType.observationType;
            case "Platform":
                return CollectionType.platform;
            case "PlatformType":
                return CollectionType.platformType;
            case "Region":
                return CollectionType.region;
            case "SemanticObservation":
                return CollectionType.semanticObsType;
            case "Sensor":
                return CollectionType.sensor;
            case "SensorType":
                return CollectionType.sensorType;
            case "User":
                return CollectionType.user;
            case "VirtualSensor":
                return CollectionType.virtualSensor;
            case "VirtualSensorType":
                return CollectionType.virtualSensorType;
            case "SensorCollection":
                return CollectionType.sensorVariable;
            case "ObservationCollection":
                return CollectionType.observationVariable;

            default:
                return CollectionType.noType;
        }
    }

}
