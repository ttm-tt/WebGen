/* Copyright (C) 2020 Christoph Theis */
package de.webgen.generator;

public class Fixtures {
    static String[] players = {
        "{plID: 1, plNr: 1001, plFirstName: Aaa, plLastName: AAA, psSex: 1, naName: ABC, naRegion: RAB}",
        "{plID: 2, plNr: 1002, plFirstName: Bbb, plLastName: BBB, psSex: 1, naName: BCD, naRegion: RBC}"
    };
    
    static String[] competitions = {
        "{cpID: 1, cpName: MS, cpDesc: \"Men's Singles\", cpCategory: Men, cpType: 1}"
    };
    
    static String[] groups = {
        "{cpID: 1, grID: 1, grName: CH, grDesc: \"Main Draw\", grModus: 2, grSize: 4}"
    };
    
    static String[] singles = {
        // KO 4 players
        "{grID: 1, mtID: 1, mtNr: 1, mtMS: 0, mtRound: 1, mtMatch: 1, mtDateTime: \"2020-12-01T09:00\", mtTable: 1, plAplID: 1, plXplID: 0, mtResA: 0, mtResX: 0}",
        "{grID: 1, mtID: 2, mtNr: 2, mtMS: 0, mtRound: 1, mtMatch: 2, mtDateTime: \"2020-12-01T10:00\", mtTable: 1, plAplID: 2, plXplID: 0, mtResA: 0, mtResX: 0}",
        "{grID: 1, mtID: 3, mtNr: 3, mtMS: 0, mtRound: 2, mtMatch: 1, mtDateTime: \"2020-12-01T11:00\", mtTable: 1, plAplID: 0, plXplID: 0, mtResA: 0, mtResX: 0}",
    };
}
