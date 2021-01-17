/* Copyright (C) 2020 Christoph Theis */
package de.webgen.generator;

public class Fixtures {
    static String[] players = {
        "{plID: 1, plNr: 1001, plFirstName: Aaa, plLastName: AAA, psSex: 1, naName: ABC, naRegion: RAA}",
        "{plID: 2, plNr: 1002, plFirstName: Bbb, plLastName: BBB, psSex: 1, naName: BCD, naRegion: RBB}",
        "{plID: 2, plNr: 1003, plFirstName: Ccc, plLastName: CCC, psSex: 1, naName: BCD, naRegion: RCC}",
        "{plID: 2, plNr: 1004, plFirstName: Ddd, plLastName: DDD, psSex: 1, naName: CDE, naRegion: RDD}"
    };
    
    static String[] competitions = {
        "{cpID: 1, cpName: TEST, cpDesc: \"Men's Singles\", cpCategory: Men, cpType: 1}"
    };
    
    static String[] groups = {
        "{cpID: 1, grID: 2, grName: KO, grDesc: \"Knock Out\", grModus: 2, grSize: 4}",
        "{cpID: 1, grID: 4, grName: PKO, grDesc: \"Progressive Knock Out\", grModus: 2, grSize: 4}"
    };
    
    static String[] singles = {
        // KO 4 players
        "{grID: 2, mtID: 1, mtNr: 1, mtMS: 0, mtRound: 1, mtMatch: 1, mtDateTime: \"2020-12-02T09:00\", mtTable: 1, plAplID: 1, plXplID: 2, mtResA: 0, mtResX: 0}",
        "{grID: 2, mtID: 2, mtNr: 2, mtMS: 0, mtRound: 1, mtMatch: 2, mtDateTime: \"2020-12-02T10:00\", mtTable: 1, plAplID: 3, plXplID: 4, mtResA: 0, mtResX: 0}",
        "{grID: 2, mtID: 3, mtNr: 3, mtMS: 0, mtRound: 2, mtMatch: 1, mtDateTime: \"2020-12-02T11:00\", mtTable: 1, plAplID: 0, plXplID: 0, mtResA: 0, mtResX: 0}",
        // PLO 4 players
        "{grID: 4, mtID: 4, mtNr: 1, mtMS: 0, mtRound: 1, mtMatch: 1, mtDateTime: \"2020-12-04T09:00\", mtTable: 1, plAplID: 1, plXplID: 2, mtResA: 0, mtResX: 0}",
        "{grID: 4, mtID: 5, mtNr: 2, mtMS: 0, mtRound: 1, mtMatch: 2, mtDateTime: \"2020-12-04T10:00\", mtTable: 1, plAplID: 3, plXplID: 4, mtResA: 0, mtResX: 0}",
        "{grID: 4, mtID: 6, mtNr: 3, mtMS: 0, mtRound: 2, mtMatch: 1, mtDateTime: \"2020-12-04T11:00\", mtTable: 1, plAplID: 0, plXplID: 0, mtResA: 0, mtResX: 0}",
        "{grID: 4, mtID: 7, mtNr: 4, mtMS: 0, mtRound: 2, mtMatch: 2, mtDateTime: \"2020-12-04T11:00\", mtTable: 1, plAplID: 0, plXplID: 0, mtResA: 0, mtResX: 0}",
    };
}
