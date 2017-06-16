package tqllang;

// this is for the scanner ---> parser communication
// the scanner tells the parser the type of token it has seen
public enum Token
{
    // keywords
    selectToken,
    fromToken,
    whereToken,
    groupbyToken,
    havingToken,
    asToken,
    numberToken,
    identToken,
    defineToken,
    sensorToken,
    observationToken,
    sensorToObsToken,

    andToken,
    orToken,
    inToken,
    stringToken,
    characterToken,

    // arithmetic operators
    timesToken,
    divToken,
    plusToken,
    minusToken,

    // logical operators
    eqlToken,
    neqToken,
    lssToken,
    geqToken,
    leqToken,
    gtrToken,

    commaToken,
    openparenToken,
    closeparenToken,

    endOfFileToken,
    errorToken,

    periodToken,
    varToken,
    semiToken
}
