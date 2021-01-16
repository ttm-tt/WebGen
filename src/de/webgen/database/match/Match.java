/* Copyright (C) 2020 Christoph Theis */

package de.webgen.database.match;

import de.webgen.database.Group;
import de.webgen.database.Player;
import de.webgen.database.Xx;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;


public abstract class Match {

    public Group gr;
    
    public int grID;
    public int mtID;
    public int mtNr;
    public int mtMS;
    public int mtRound;
    public int mtMatch;
    public boolean mtReverse;

    public Timestamp mtDateTime;
    public int mtTable;

    public int mtMatches;
    public int mtBestOf;

    public int mtResA;
    public int mtResX;

    public int stA;
    public int stX;
    
    public int tmAtmID;
    public int tmXtmID;
    
    public  Xx      xxA;
    public  Xx      xxX;

    public boolean mtWalkOverA;
    public boolean mtWalkOverX;
    public boolean mtInjuredA;
    public boolean mtInjuredX;
    public boolean mtDisqualifiedA;
    public boolean mtDisqualifiedX;
    
    public boolean mtPrinted;
    public boolean mtChecked;
    
    public int result[][];

    public Timestamp mtTimestamp;
    
    public enum Side {
        None,
        A,
        X
    };

    abstract public void read(ResultSet rs) throws SQLException;
    
    abstract public boolean isFinished();
    abstract public Side  getWinnerAX();
    /**
     *
     * @param forWinner
     * @param locale
     * @return
     */
    abstract public String getResultShortForm(boolean forWinner);

    abstract public String aToString(boolean withAssoc, boolean withNr, boolean withFlag);
    abstract public String xToString(boolean withAssoc, boolean withNr, boolean withFlag);
    abstract public String aNrToString();
    abstract public String xNrToString();
    abstract public String aAssocToString();
    abstract public String xAssocToString();
    
    abstract public Player getPlA();
    abstract public Player getPlB();
    abstract public Player getPlX();
    abstract public Player getPlY();

    public String winnerToString(boolean withAssoc) {
        switch (getWinnerAX()) {
            case None :
                return "<br>";
                
            case A :
                return aToString(withAssoc, true, false);
                
            case X :
                return xToString(withAssoc, true, false);
        }
        
        // To make a simple compiler happy
        return "<br>";
    }
    
    
    public String winnerAssocToString() {
        if (gr == null)
            return "<br>";
        else switch (getWinnerAX()) {
            case None :
                return "<br>";
                
            case A :
                return aAssocToString();
                
            case X :
                return xAssocToString();
        }
        
        // Not reached
        return "<br>";        
    }
    
    
    public String roundToString() {
        if (gr == null)
            return "";
        
        switch (gr.grModus) {
            case 1: // RR
                return "data-i18n=\"rr.format.short.round\" data-i18n-options=\'{\"round\" : \"" + mtRound + "\"}\'";
                
            case 2: // SKO
            {     
                int nof = gr.grSize >> mtRound;
                
                if (gr.grNofMatches > 0 || gr.grNofRounds > 0)
                    return "data-i18n=\"ko.format.short.round\" data-i18n-options=\'{\"round\" : \"" + mtRound + "\"}\'";
                else if (mtRound <= gr.grQualRounds)
                    return "data-i18n=\"ko.format.short.qualification\"";
                else if (nof == 1)
                    return "data-i18n=\"ko.format.short.final\"";
                else if (nof == 2)
                    return "data-i18n=\"ko.format.short.semifinal\"";
                else
                    return "data-i18n=\"ko.format.short.roundof\" data-i18n-options=\'{\"matches\" : \"" + nof + "\", \"entries\": \"" + (2 * nof) + "\"}\'";
            }
            
            case 3: // DKO
                return "data-i18n=\"ko.format.short.round\" data-i18n-options=\'{\"round\" : \"" + mtRound + "\"}\'";
                
            case 4: // PLO
            {     
                int nof = (gr.grNofRounds == 0 ? gr.grSize >> mtRound : 1 << (gr.grNofRounds - mtRound));
                
                if (gr.grWinner == 1) {
                    if (nof == 1 && mtMatch == 1)
                        return "data-i18n=\"ko.format.short.final\"";
                    else if (nof == 2 && mtMatch <= 2)
                        return "data-i18n=\"ko.format.short.semifinal\"";
                }
                
                int m = mtMatch - 1;
                int from = ((m / nof) * nof);
                int to = (from + nof);
                
                from = gr.grWinner + 2 * from;
                to = gr.grWinner + 2 * to - 1;
                
                // Ein Spiel geht um 2 Plaetze
                return "data-i18n=\"pko.format.short.pos\" data-i18n-options=\'{\"from\" : \"" + from + "\", \"to\": \"" + to + "\"}\'";
            }
            
            default:
                return "data-i18n=\"ko.format.short.round\" data-i18n-options=\'{\"round\" : \"" + mtRound + "\"}\'";
        }
    }
}
