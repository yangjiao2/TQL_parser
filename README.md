## Parser for Tippers (Testbed for IoT-based Privacy Preserving PERvasive SSpaces)

Translate TQL into MySQL syntax

### Main Challenges:

1. Handling the “.” notation. 

2. Resolving joins.

3. Creating one final query

### Architecture
![Architecture](https://github.com/yangjiao2/TQL_parser/blob/master/architecture.png)

TQL query: 

> DEFINE SensorCollection sensor_collection1;
>
> DEFINE ObservationCollection observation_collection1;
>
> sensor_collection1 = SELECT sen.* FROM Sensor sen
> WHERE sen.coverageRooms.region.floor = 3;
>
> observation_collection1 = SENSORS_TO_OBSERVATIONS(sensor_collection1);
>
> SELECT obs.* FROM observation_collection1 obs;

MySQL Query (translated): 

> SELECT obs.*
> FROM (
> SELECT observation_collection1.*  FROM Observation AS observation_collection1
> INNER JOIN ( 
>
>	SELECT sen.* FROM Sensor AS sen
>	INNER JOIN 
>   	Sen_Infr AS sen_Sen_Infr ON sen.id = sen_Sen_Infr.sen_id
> 
> 	INNER JOIN 
>		Infrastructure AS sen_Sen_Infr_Infrastructure 
>		ON sen_Sen_Infr.infr_id = sen_Sen_Infr_Infrastructure.id
>
>	INNER JOIN 
>			Region AS sen_Sen_Infr_Infrastructure_Region 
>			ON 
>  sen_Sen_Infr_Infrastructure.reg_id = sen_Sen_Infr_Infrastructure_Region.id
>
> WHERE sen_Sen_Infr_Infrastructure_Region.floors = 3 ) 
>	AS sensor_collection1 
>
>	ON (observation_collection1.sen_id = sensor_collection1.id)
>	) AS obs;



